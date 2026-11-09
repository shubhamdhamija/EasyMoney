"""Application-owned rules for deciding whether a proposed tool call may run.

Both the custom planner and the LangGraph workflow call these public functions,
so the two orchestrators cannot drift apart.
"""

from dataclasses import dataclass
from typing import Any, Literal

from app.models.research_state import ResearchError, ResearchState

PolicyOutcome = Literal["execute", "skip_duplicate", "limit_reached", "invalid_arguments"]


@dataclass(frozen=True)
class ToolRequestPolicyResult:
    outcome: PolicyOutcome
    arguments: dict[str, Any]

    @property
    def should_execute(self) -> bool:
        return self.outcome == "execute"


def is_duplicate_request(state: ResearchState, tool_name: str) -> bool:
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
    if isinstance(symbol, str) and symbol.strip().upper() != state.symbol:
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
    """Check order: duplicate (free) -> tool budget -> argument constraints."""
    if is_duplicate_request(state, tool_name):
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


def apply_tool_result(
    *,
    state: ResearchState,
    tool_name: str,
    result: dict[str, Any],
) -> None:
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
