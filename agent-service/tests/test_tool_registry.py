import pytest

from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.tool_registry import ToolRegistry


@pytest.mark.asyncio
async def test_executes_registered_tool() -> None:
    registry = ToolRegistry()
    registry.register(FakeStockQuoteTool())

    response = await registry.execute(
        tool_name="get_stock_quote",
        arguments={"symbol": " aapl "},
    )

    assert response["ok"] is True
    assert response["result"]["symbol"] == "AAPL"
    assert response["result"]["current_price"] == 228.30


@pytest.mark.asyncio
async def test_rejects_unknown_tool() -> None:
    registry = ToolRegistry()

    response = await registry.execute(
        tool_name="delete_database",
        arguments={},
    )

    assert response["ok"] is False
    assert response["error"]["code"] == "UNKNOWN_TOOL"


@pytest.mark.asyncio
async def test_rejects_invalid_arguments() -> None:
    quote_tool = FakeStockQuoteTool()
    registry = ToolRegistry()
    registry.register(quote_tool)

    response = await registry.execute(
        tool_name="get_stock_quote",
        arguments={"symbol": "AAPL; DELETE"},
    )

    assert response["ok"] is False
    assert response["error"]["code"] == "INVALID_TOOL_ARGUMENTS"
    assert len(quote_tool.calls) == 0

