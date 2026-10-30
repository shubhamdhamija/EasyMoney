import pytest

from app.services.default_research_service import create_default_research_service


@pytest.mark.asyncio
async def test_returns_quote_only_report() -> None:
    service = create_default_research_service()

    report = await service.research_stock(
        "AAPL",
        include_news=False,
    )

    assert report.symbol == "AAPL"
    assert report.status == "completed"
    assert report.quote is not None
    assert report.news_count == 0
    assert report.error_code is None
    assert report.agent_used is False
    assert report.tools_used == ["get_stock_quote"]


@pytest.mark.asyncio
async def test_returns_quote_and_news_report() -> None:
    service = create_default_research_service()

    report = await service.research_stock(
        "AAPL",
        include_news=True,
        maximum_news_items=2,
    )

    assert report.symbol == "AAPL"
    assert report.status == "completed"
    assert report.quote is not None
    assert report.news_count == 2
    assert report.error_code is None
    assert report.tools_used == [
        "get_stock_quote",
        "get_recent_news",
    ]


@pytest.mark.asyncio
async def test_automatic_plan_fetches_news() -> None:
    service = create_default_research_service()

    report = await service.research_stock(
        "AAPL",
        include_news=None,
        maximum_news_items=2,
    )

    assert report.status == "completed"
    assert report.news_count == 2
    assert "get_recent_news" in report.tools_used


@pytest.mark.asyncio
async def test_returns_failed_report_for_unknown_symbol() -> None:
    service = create_default_research_service()

    report = await service.research_stock("UNKNOWN")

    assert report.symbol == "UNKNOWN"
    assert report.status == "failed"
    assert report.quote is None
    assert report.news_count == 0
    assert report.error_code == "QUOTE_NOT_FOUND"
    assert report.tools_used == []


@pytest.mark.asyncio
async def test_returns_partial_report_when_news_fails() -> None:
    service = create_default_research_service()

    report = await service.research_stock(
        "SHOP.TO",
        include_news=True,
    )

    assert report.symbol == "SHOP.TO"
    assert report.status == "partial"
    assert report.quote is not None
    assert report.news_count == 0
    assert report.error_code == "NEWS_NOT_FOUND"
    assert report.tools_used == ["get_stock_quote"]

