import pytest

from app.models.recent_news import RecentNewsInput
from app.tools.fake_recent_news_tool import FakeRecentNewsTool
from app.tools.tool_errors import ToolError


@pytest.mark.asyncio
async def test_returns_aapl_news() -> None:
    tool = FakeRecentNewsTool()

    result = await tool.execute(RecentNewsInput(symbol="AAPL", maximum_items=3))

    assert result.symbol == "AAPL"
    assert len(result.articles) == 3
    assert result.articles[0].headline == "Quarterly results were published."
    assert result.articles[0].source == "Controlled News"


@pytest.mark.asyncio
async def test_respects_maximum_items() -> None:
    tool = FakeRecentNewsTool()

    result = await tool.execute(RecentNewsInput(symbol="AAPL", maximum_items=2))

    assert len(result.articles) == 2


@pytest.mark.asyncio
async def test_unknown_symbol_returns_structured_error() -> None:
    tool = FakeRecentNewsTool()

    with pytest.raises(ToolError) as error_info:
        await tool.execute(RecentNewsInput(symbol="UNKNOWN"))

    assert error_info.value.code == "NEWS_NOT_FOUND"
    assert error_info.value.message == "No recent news is available for the requested symbol."


@pytest.mark.asyncio
async def test_news_tool_records_calls() -> None:
    tool = FakeRecentNewsTool()

    await tool.execute(RecentNewsInput(symbol="AAPL", maximum_items=2))

    assert len(tool.calls) == 1
    assert tool.calls[0].symbol == "AAPL"
    assert tool.calls[0].maximum_items == 2


def test_news_tool_has_name_and_input_model() -> None:
    tool = FakeRecentNewsTool()

    assert tool.name == "get_recent_news"
    assert tool.input_model is RecentNewsInput
    assert "news" in tool.description.lower()
    assert "investment recommendations" in tool.description.lower()

