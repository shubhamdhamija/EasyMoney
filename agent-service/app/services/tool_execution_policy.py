"""Application-owned rules for deciding whether a proposed tool call may run.

Both the custom planner and the LangGraph workflow call these public functions,
so the two orchestrators cannot drift apart.

Portfolio tools (read-only) additionally require application-owned portfolio
consent in the research state, the selected portfolio ID is pinned, and exact
duplicate portfolio requests are skipped. Identity (tenant/user) never comes from
model arguments: it lives in the trusted ToolExecutionContext.
"""

import hashlib
import json
from dataclasses import dataclass
from typing import Any, Literal

from pydantic import ValidationError

from app.models.portfolio_agent import (
    MAX_POSITIONS,
    MAX_TOP_POSITIONS,
    AnalyzePortfolioConcentrationInput,
    GetPortfolioPositionInput,
    GetPortfolioPositionsInput,
    GetPortfolioSummaryInput,
)
from app.models.research_state import ResearchError, ResearchState

PolicyOutcome = Literal[
    "execute",
    "skip_duplicate",
    "limit_reached",
    "invalid_arguments",
    "consent_required",
    "portfolio_rejected",
]

PORTFOLIO_TOOLS = frozenset(
    {
        "get_portfolio_summary",
        "get_portfolio_positions",
        "get_portfolio_position",
        "analyze_portfolio_concentration",
    }
)

_PORTFOLIO_INPUT_MODELS = {
    "get_portfolio_summary": GetPortfolioSummaryInput,
    "get_portfolio_positions": GetPortfolioPositionsInput,
    "get_portfolio_position": GetPortfolioPositionInput,
    "analyze_portfolio_concentration": AnalyzePortfolioConcentrationInput,
}

# The model never needs to know the real portfolio ID: it may omit it or use this
# placeholder, and the application pins the selected portfolio.
SELECTED_PORTFOLIO_PLACEHOLDER = "selected"

_REJECTION_ERRORS = {
    "limit_reached": ("TOOL_CALL_LIMIT_REACHED", "The research workflow reached its tool-call limit."),
    "invalid_arguments": ("INVALID_TOOL_ARGUMENTS", "The tool arguments are invalid."),
    "consent_required": ("PORTFOLIO_CONSENT_REQUIRED", "Confirm portfolio research before continuing."),
    "portfolio_rejected": ("PORTFOLIO_ACCESS_DENIED", "This portfolio is no longer available."),
}


@dataclass(frozen=True)
class ToolRequestPolicyResult:
    outcome: PolicyOutcome
    arguments: dict[str, Any]

    @property
    def should_execute(self) -> bool:
        return self.outcome == "execute"


def policy_rejection_error(outcome: str) -> ResearchError | None:
    """The workflow error for a stopping outcome; None for execute / skip_duplicate."""
    entry = _REJECTION_ERRORS.get(outcome)
    return None if entry is None else ResearchError(code=entry[0], message=entry[1])


def has_portfolio_evidence(state: ResearchState) -> bool:
    return bool(
        state.portfolio_summary is not None
        or state.portfolio_position_results
        or state.portfolio_concentration_results
    )


def finish_error(state: ResearchState) -> ResearchError | None:
    """Error to record when the model finishes without the required evidence."""
    if state.error is not None:
        return None
    if state.portfolio_mode:
        if not has_portfolio_evidence(state):
            return ResearchError(
                code="PORTFOLIO_NOT_COLLECTED",
                message="The workflow finished without collecting portfolio data.",
            )
        return None
    if state.quote is None:
        return ResearchError(
            code="QUOTE_NOT_COLLECTED",
            message="The workflow finished without collecting a quote.",
        )
    return None


def _prepare_portfolio_arguments(
    state: ResearchState,
    tool_name: str,
    arguments: dict[str, Any],
) -> tuple[dict[str, Any] | None, PolicyOutcome | None]:
    """Pin the portfolio, cap bounds, normalize. Never mutates the input."""
    prepared = dict(arguments)
    requested_id = prepared.get("portfolio_id")
    if requested_id is None or requested_id == SELECTED_PORTFOLIO_PLACEHOLDER:
        prepared["portfolio_id"] = state.selected_portfolio_id
    elif not isinstance(requested_id, str) or requested_id.strip() != state.selected_portfolio_id:
        return None, "portfolio_rejected"
    else:
        prepared["portfolio_id"] = requested_id.strip()

    for key, cap in (("maximum_positions", MAX_POSITIONS), ("maximum_top_positions", MAX_TOP_POSITIONS)):
        value = prepared.get(key)
        if isinstance(value, int) and not isinstance(value, bool):
            prepared[key] = max(1, min(value, cap))

    symbol = prepared.get("symbol")
    if isinstance(symbol, str):
        prepared["symbol"] = symbol.strip().upper()

    try:
        normalized = _PORTFOLIO_INPUT_MODELS[tool_name].model_validate(prepared)
    except ValidationError:
        return None, "invalid_arguments"
    return normalized.model_dump(mode="json"), None


def portfolio_request_fingerprint(tool_name: str, normalized_arguments: dict[str, Any]) -> str:
    canonical = json.dumps(
        {"tool": tool_name, "arguments": normalized_arguments}, sort_keys=True, separators=(",", ":")
    )
    return hashlib.sha256(canonical.encode()).hexdigest()[:32]


def is_duplicate_request(
    state: ResearchState,
    tool_name: str,
    arguments: dict[str, Any] | None = None,
) -> bool:
    if tool_name in PORTFOLIO_TOOLS:
        if arguments is None:
            return False
        prepared, _ = _prepare_portfolio_arguments(state, tool_name, arguments)
        if prepared is None:
            return False
        return portfolio_request_fingerprint(tool_name, prepared) in state.portfolio_tool_requests
    if tool_name == "get_stock_quote":
        return state.quote is not None
    if tool_name == "get_recent_news":
        return state.news is not None
    if tool_name == "search_financial_documents":
        return any(
            query.strip().lower() == candidate.strip().lower()
            for query in state.document_search_queries
            for candidate in [query]
        )
    return False


def prepare_tool_arguments(
    *,
    state: ResearchState,
    tool_name: str,
    arguments: dict[str, Any],
    maximum_news_items: int,
) -> dict[str, Any] | None:
    """Apply application-owned constraints; None means reject outright.

    The input dictionary is never mutated. All other validation is left to the
    registry and the tool input models.
    """
    prepared = dict(arguments)

    symbol = prepared.get("symbol")
    # In portfolio mode the symbol comes from a held security, not one pinned request.
    if (
        not state.portfolio_mode
        and isinstance(symbol, str)
        and symbol.strip().upper() != state.symbol
    ):
        return None

    if tool_name == "get_recent_news":
        requested = prepared.get("maximum_items")
        if requested is None:
            prepared["maximum_items"] = maximum_news_items
        elif isinstance(requested, int) and not isinstance(requested, bool):
            prepared["maximum_items"] = min(requested, maximum_news_items)

    if tool_name == "search_financial_documents":
        query = prepared.get("query")
        if not isinstance(query, str):
            return None
        normalized_query = query.strip()
        if not normalized_query:
            return None
        maximum_results = prepared.get("maximum_results", 5)
        if not isinstance(maximum_results, int) or isinstance(maximum_results, bool):
            return None
        prepared["maximum_results"] = max(1, min(int(maximum_results), 10))
        prepared["query"] = normalized_query
        if any(existing.strip().lower() == normalized_query.lower() for existing in state.document_search_queries):
            return None

    return prepared


def evaluate_tool_request(
    *,
    state: ResearchState,
    tool_name: str,
    arguments: dict[str, Any],
    maximum_news_items: int,
) -> ToolRequestPolicyResult:
    """Portfolio gate -> duplicate (free) -> tool budget -> argument constraints."""
    if tool_name in PORTFOLIO_TOOLS:
        if not state.portfolio_mode or state.portfolio_consent_id is None:
            return ToolRequestPolicyResult("consent_required", dict(arguments))
        prepared_portfolio, rejection = _prepare_portfolio_arguments(state, tool_name, arguments)
        if prepared_portfolio is None:
            return ToolRequestPolicyResult(rejection or "invalid_arguments", dict(arguments))
        if portfolio_request_fingerprint(tool_name, prepared_portfolio) in state.portfolio_tool_requests:
            return ToolRequestPolicyResult("skip_duplicate", prepared_portfolio)
        if state.tool_call_count >= state.maximum_tool_calls:
            return ToolRequestPolicyResult("limit_reached", prepared_portfolio)
        return ToolRequestPolicyResult("execute", prepared_portfolio)

    if is_duplicate_request(state, tool_name, arguments):
        return ToolRequestPolicyResult("skip_duplicate", dict(arguments))

    if state.tool_call_count >= state.maximum_tool_calls:
        return ToolRequestPolicyResult("limit_reached", dict(arguments))

    prepared = prepare_tool_arguments(
        state=state,
        tool_name=tool_name,
        arguments=arguments,
        maximum_news_items=maximum_news_items,
    )
    if prepared is None:
        return ToolRequestPolicyResult("invalid_arguments", dict(arguments))

    return ToolRequestPolicyResult("execute", prepared)


def _record_portfolio_result(
    state: ResearchState,
    tool_name: str,
    result: dict[str, Any],
    arguments: dict[str, Any] | None,
) -> None:
    source = result.get("source")
    snapshot_at = result.get("snapshot_at")
    if isinstance(source, str):
        state.portfolio_source = source
    if snapshot_at is not None:
        state.portfolio_snapshot_at = snapshot_at

    if tool_name == "get_portfolio_summary":
        state.portfolio_summary = result
    elif tool_name == "analyze_portfolio_concentration":
        if len(state.portfolio_concentration_results) < 5:
            state.portfolio_concentration_results.append(result)
    elif len(state.portfolio_position_results) < 10:
        state.portfolio_position_results.append(result)

    if arguments is not None:
        fingerprint = portfolio_request_fingerprint(tool_name, arguments)
        if fingerprint not in state.portfolio_tool_requests and len(state.portfolio_tool_requests) < 20:
            state.portfolio_tool_requests.append(fingerprint)


def apply_tool_result(
    *,
    state: ResearchState,
    tool_name: str,
    result: dict[str, Any],
    arguments: dict[str, Any] | None = None,
) -> None:
    if tool_name in PORTFOLIO_TOOLS:
        _record_portfolio_result(state, tool_name, result, arguments)
        return

    if tool_name == "get_stock_quote":
        state.quote = result
        return

    if tool_name == "get_recent_news":
        state.news = result
        return

    if tool_name == "search_financial_documents":
        state.document_results = result.get("results")
        if isinstance(result, dict):
            state.document_search_queries.append(result.get("query", ""))
        return

    if tool_name == "save_research_report":
        return

    state.error = ResearchError(
        code="UNSUPPORTED_TOOL_RESULT",
        message="The planner could not store the tool result.",
    )
