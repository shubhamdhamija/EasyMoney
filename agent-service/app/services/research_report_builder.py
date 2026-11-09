import re

from app.models.financial_document import FinancialCitation
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


def build_research_report(state: ResearchState) -> ResearchReport:
    completed_tools = [
        step.tool_name
        for step in state.steps
        if step.status == "completed"
    ]

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
