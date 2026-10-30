from app.models.research_report import ResearchReport
from app.models.research_state import ResearchState


def build_research_report(state: ResearchState) -> ResearchReport:
    completed_tools = [
        step.tool_name
        for step in state.steps
        if step.status == "completed"
    ]

    if state.quote is None:
        return ResearchReport(
            symbol=state.symbol,
            status="failed",
            summary=(
                f"Research could not be completed "
                f"for {state.symbol}."
            ),
            quote=None,
            news_count=0,
            tools_used=completed_tools,
            error_code=(state.error.code if state.error else "UNKNOWN_ERROR"),
            agent_used=state.agent_used,
        )

    articles = []

    if state.news is not None:
        articles = state.news.get("articles", [])

    news_count = len(articles)

    if state.error is not None:
        status = "partial"
        summary = (
            f"Quote data was collected for "
            f"{state.symbol}, but the complete "
            f"research workflow could not finish."
        )
    else:
        status = "completed"

        current_price = state.quote["current_price"]
        currency = state.quote["currency"]
        change_percent = state.quote["price_change_percent"]
        sign = "+" if change_percent > 0 else ""
        summary = (
            f"{state.symbol} is trading at "
            f"{current_price:.2f} {currency} "
            f"with a daily movement of "
            f"{sign}{change_percent:.2f}%. "
            f"{news_count} recent article"
            f"{'s' if news_count != 1 else ''} "
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
    )

