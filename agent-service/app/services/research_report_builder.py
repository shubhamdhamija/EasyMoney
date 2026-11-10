import re

from app.models.financial_document import FinancialCitation
from app.models.portfolio_agent import PORTFOLIO_REPORT_STATEMENT
from app.models.research_report import ResearchReport
from app.models.research_state import ResearchState


def _to_evidence_summary(document_results: dict[str, object] | None) -> tuple[list[str], list[FinancialCitation]]:
    if not document_results:
        return [], []
    results = document_results.get("results")
    if not isinstance(results, list):
        return [], []

    evidence: list[str] = []
    citations: list[FinancialCitation] = []
    seen: set[str] = set()
    for item in results:
        if not isinstance(item, dict):
            continue
        chunk = item.get("chunk")
        if not isinstance(chunk, dict):
            continue
        citation = item.get("citation")
        if not isinstance(citation, dict):
            continue
        citation_id = citation.get("citation_id")
        if not isinstance(citation_id, str) or not citation_id:
            continue
        if citation_id in seen:
            continue
        seen.add(citation_id)
        text = str(chunk.get("text", "")).strip()
        if not text:
            continue
        sentence = text[:500]
        evidence.append(sentence)
        citations.append(FinancialCitation.model_validate(citation))
        if len(evidence) >= 5:
            break
    return evidence, citations


_INSTRUCTION_PATTERNS = (
    re.compile(r"\b(ignore|disregard|forget)\b.{0,30}\b(previous|prior|above|all)\b", re.IGNORECASE),
    re.compile(r"\b(system prompt|developer message|instructions?)\b", re.IGNORECASE),
    re.compile(r"\b(call|run|execute|invoke)\b.{0,20}\b\w+_\w+\b", re.IGNORECASE),
    re.compile(r"\b\w+_\w+\(", re.IGNORECASE),
)


def _looks_like_instruction(text: str) -> bool:
    """Defence in depth: third-party headlines that read like commands to an AI are not echoed."""
    return any(pattern.search(text) for pattern in _INSTRUCTION_PATTERNS)


def _to_headlines(articles: list[object]) -> list[dict[str, object]]:
    headlines: list[dict[str, object]] = []
    for article in articles:
        if hasattr(article, "model_dump"):
            article = article.model_dump(mode="json")
        if not isinstance(article, dict):
            continue
        headline = str(article.get("headline", "")).strip()
        source = str(article.get("source", "")).strip()
        if not headline or _looks_like_instruction(headline) or _looks_like_instruction(source):
            continue
        headlines.append(
            {
                "headline": headline,
                "source": source,
                "published_at": article.get("published_at"),
            }
        )
        if len(headlines) >= 5:
            break
    return headlines


_MAX_REPORT_POSITIONS = 20
_MAX_REPORT_WARNINGS = 12


def _portfolio_positions(state: ResearchState) -> list[dict]:
    """Distinct positions from tool results. Only safe display fields (no position IDs)."""
    seen: set[str] = set()
    rows: list[dict] = []

    def add(position: dict) -> None:
        key = str(position.get("position_id"))
        if key in seen or len(rows) >= _MAX_REPORT_POSITIONS:
            return
        seen.add(key)
        rows.append({k: v for k, v in position.items() if k != "position_id"})

    for result in state.portfolio_position_results:
        for position in result.get("positions") or []:
            add(position)
        for security in result.get("securities") or []:
            for position in security.get("positions") or []:
                add(position)
    return rows


def _portfolio_warnings(state: ResearchState) -> list[dict]:
    seen: set[tuple] = set()
    warnings: list[dict] = []
    results = [state.portfolio_summary, *state.portfolio_position_results, *state.portfolio_concentration_results]
    for result in results:
        for warning in (result or {}).get("warnings") or []:
            key = (warning.get("code"), warning.get("currency"))
            if key not in seen and len(warnings) < _MAX_REPORT_WARNINGS:
                seen.add(key)
                warnings.append(warning)
    return warnings


def _build_portfolio_report(state: ResearchState, completed_tools: list[str]) -> ResearchReport:
    evidence_summary, citations = _to_evidence_summary(
        state.document_results.model_dump(mode="json") if state.document_results else None
    )
    summary_data = state.portfolio_summary or {}
    currency_summaries = list(summary_data.get("currency_summaries") or [])
    has_evidence = bool(
        state.portfolio_summary is not None
        or state.portfolio_position_results
        or state.portfolio_concentration_results
    )

    if not has_evidence:
        return ResearchReport(
            symbol=state.symbol,
            status="failed",
            summary="Portfolio research could not be completed.",
            tools_used=completed_tools,
            error_code=(state.error.code if state.error else "UNKNOWN_ERROR"),
            agent_used=state.agent_used,
            evidence_summary=evidence_summary,
            citations=citations,
            portfolio_source=state.portfolio_source,
            portfolio_snapshot_at=state.portfolio_snapshot_at,
            portfolio_disclaimer=PORTFOLIO_REPORT_STATEMENT,
        )

    parts = [f"Portfolio research used a read-only {state.portfolio_source or 'portfolio'} snapshot."]
    for item in currency_summaries:
        parts.append(
            f"{item.get('currency')}: investments {item.get('investment_market_value')} "
            f"across {item.get('position_count')} position"
            f"{'' if item.get('position_count') == 1 else 's'}."
        )
    for item in state.portfolio_concentration_results:
        weight = item.get("largest_position_weight_percent")
        largest = item.get("largest_position") or {}
        if weight is not None:
            label = largest.get("symbol") or largest.get("security_name") or "largest position"
            parts.append(f"{item.get('currency')}: {label} is {weight}% of eligible holdings.")
    parts.append(PORTFOLIO_REPORT_STATEMENT)

    articles = state.news.get("articles", []) if state.news else []

    return ResearchReport(
        symbol=state.symbol,
        status="partial" if state.error is not None else "completed",
        summary=" ".join(parts),
        quote=state.quote,
        news_count=len(articles),
        tools_used=completed_tools,
        error_code=(state.error.code if state.error else None),
        agent_used=state.agent_used,
        evidence_summary=evidence_summary,
        citations=citations,
        headlines=_to_headlines(articles),
        portfolio_snapshot_at=state.portfolio_snapshot_at,
        portfolio_source=state.portfolio_source,
        portfolio_currency_summaries=currency_summaries,
        portfolio_positions=_portfolio_positions(state),
        portfolio_concentration=list(state.portfolio_concentration_results),
        portfolio_warnings=_portfolio_warnings(state),
        portfolio_disclaimer=PORTFOLIO_REPORT_STATEMENT,
    )


def build_research_report(state: ResearchState) -> ResearchReport:
    completed_tools = [
        step.tool_name
        for step in state.steps
        if step.status == "completed"
    ]

    if state.portfolio_mode:
        return _build_portfolio_report(state, completed_tools)

    evidence_summary, citations = _to_evidence_summary(state.document_results.model_dump(mode="json") if state.document_results else None)

    if state.quote is None:
        return ResearchReport(
            symbol=state.symbol,
            status="failed",
            summary=(
                f"Research could not be completed for {state.symbol}."
            ),
            quote=None,
            news_count=0,
            tools_used=completed_tools,
            error_code=(state.error.code if state.error else "UNKNOWN_ERROR"),
            agent_used=state.agent_used,
            evidence_summary=evidence_summary,
            citations=citations,
        )

    articles = []

    if state.news is not None:
        articles = state.news.get("articles", [])

    news_count = len(articles)

    if state.error is not None:
        status = "partial"
        summary = (
            f"Quote data was collected for {state.symbol}, but the complete "
            f"research workflow could not finish."
        )
    else:
        status = "completed"

        current_price = state.quote["current_price"]
        currency = state.quote["currency"]
        change_percent = state.quote["price_change_percent"]
        sign = "+" if change_percent > 0 else ""
        summary = (
            f"{state.symbol} is trading at {current_price:.2f} {currency} "
            f"with a daily movement of {sign}{change_percent:.2f}%. "
            f"{news_count} recent article{'s' if news_count != 1 else ''} "
            f"were collected."
        )

    return ResearchReport(
        symbol=state.symbol,
        status=status,
        summary=summary,
        quote=state.quote,
        news_count=news_count,
        tools_used=completed_tools,
        error_code=(state.error.code if state.error else None),
        agent_used=state.agent_used,
        evidence_summary=evidence_summary,
        citations=citations,
        headlines=_to_headlines(articles),
    )
