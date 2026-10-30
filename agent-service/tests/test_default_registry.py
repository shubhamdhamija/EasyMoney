import pytest

from app.tools.default_registry import create_default_registry


def test_default_registry_contains_approved_tools() -> None:
    registry = create_default_registry()

    assert registry.contains("get_stock_quote")
    assert registry.contains("get_recent_news")


def test_default_registry_lists_approved_tools() -> None:
    registry = create_default_registry()

    assert registry.available_tool_names() == ["get_recent_news", "get_stock_quote"]


@pytest.mark.asyncio
async def test_default_registry_executes_quote_tool() -> None:
    registry = create_default_registry()

    response = await registry.execute(
        tool_name="get_stock_quote",
        arguments={"symbol": " aapl "},
    )

    assert response["ok"] is True
    assert response["result"]["symbol"] == "AAPL"
    assert response["result"]["current_price"] == 228.30


@pytest.mark.asyncio
async def test_default_registry_executes_news_tool() -> None:
    registry = create_default_registry()

    response = await registry.execute(
        tool_name="get_recent_news",
        arguments={"symbol": " aapl ", "maximum_items": 2},
    )

    assert response["ok"] is True
    assert response["result"]["symbol"] == "AAPL"
    assert len(response["result"]["articles"]) == 2

