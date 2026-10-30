from app.models.research_state import ResearchError, ResearchState, ResearchStep
from app.services.research_report_builder import build_research_report


def test_builds_completed_report() -> None:
    state = ResearchState(
        symbol="AAPL",
        quote={
            "symbol": "AAPL",
            "current_price": 228.30,
            "price_change_percent": 2.40,
            "currency": "USD",
            "data_timestamp": "2026-10-27T14:30:00Z",
        },
        news={
            "symbol": "AAPL",
            "articles": [
                {
                    "headline": "Quarterly results were published.",
                    "source": "Controlled News",
                    "published_at": "2026-10-27T14:00:00Z",
                },
                {
                    "headline": "A software update was announced.",
                    "source": "Controlled Technology News",
                    "published_at": "2026-10-26T18:30:00Z",
                },
            ],
        },
        steps=[
            ResearchStep(
                tool_name="get_stock_quote",
                status="completed",
            ),
            ResearchStep(
                tool_name="get_recent_news",
                status="completed",
            ),
        ],
        tool_call_count=2,
    )

    report = build_research_report(state)

    assert report.status == "completed"
    assert report.symbol == "AAPL"
    assert report.news_count == 2
    assert report.tools_used == [
        "get_stock_quote",
        "get_recent_news",
    ]
    assert report.error_code is None
    assert report.agent_used is False
    assert "+2.40%" in report.summary


def test_builds_partial_report_when_news_fails() -> None:
    state = ResearchState(
        symbol="SHOP.TO",
        quote={
            "symbol": "SHOP.TO",
            "current_price": 153.75,
            "price_change_percent": -1.25,
            "currency": "CAD",
            "data_timestamp": "2026-10-27T14:30:00Z",
        },
        error=ResearchError(
            code="NEWS_NOT_FOUND",
            message=(
                "No recent news is available "
                "for the requested symbol."
            ),
        ),
        steps=[
            ResearchStep(
                tool_name="get_stock_quote",
                status="completed",
            ),
            ResearchStep(
                tool_name="get_recent_news",
                status="failed",
            ),
        ],
        tool_call_count=2,
    )

    report = build_research_report(state)

    assert report.status == "partial"
    assert report.quote is not None
    assert report.news_count == 0
    assert report.error_code == "NEWS_NOT_FOUND"
    assert report.tools_used == ["get_stock_quote"]


def test_builds_failed_report_when_quote_fails() -> None:
    state = ResearchState(
        symbol="UNKNOWN",
        error=ResearchError(
            code="QUOTE_NOT_FOUND",
            message=(
                "No quote is available for "
                "the requested symbol."
            ),
        ),
        steps=[
            ResearchStep(
                tool_name="get_stock_quote",
                status="failed",
            )
        ],
        tool_call_count=1,
    )

    report = build_research_report(state)

    assert report.status == "failed"
    assert report.quote is None
    assert report.news_count == 0
    assert report.error_code == "QUOTE_NOT_FOUND"
    assert report.tools_used == []

