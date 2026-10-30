import pytest

from app.models.stock_quote import StockQuoteInput
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.tool_errors import ToolError


@pytest.mark.asyncio
async def test_returns_aapl_quote() -> None:
    tool = FakeStockQuoteTool()

    result = await tool.execute(StockQuoteInput(symbol="AAPL"))

    assert result.symbol == "AAPL"
    assert result.current_price == 228.30
    assert result.price_change_percent == 2.40
    assert result.currency == "USD"
    assert result.data_timestamp is not None


@pytest.mark.asyncio
async def test_returns_canadian_currency() -> None:
    tool = FakeStockQuoteTool()

    result = await tool.execute(StockQuoteInput(symbol="SHOP.TO"))

    assert result.symbol == "SHOP.TO"
    assert result.currency == "CAD"
    assert result.price_change_percent == -1.25


@pytest.mark.asyncio
async def test_unknown_symbol_returns_structured_error() -> None:
    tool = FakeStockQuoteTool()

    with pytest.raises(ToolError) as error_info:
        await tool.execute(StockQuoteInput(symbol="UNKNOWN"))

    assert error_info.value.code == "QUOTE_NOT_FOUND"
    assert error_info.value.message == "No quote is available for the requested symbol."


@pytest.mark.asyncio
async def test_tool_records_calls() -> None:
    tool = FakeStockQuoteTool()

    await tool.execute(StockQuoteInput(symbol="AAPL"))

    assert len(tool.calls) == 1
    assert tool.calls[0].symbol == "AAPL"


def test_tool_has_clear_name_and_description() -> None:
    tool = FakeStockQuoteTool()

    assert tool.name == "get_stock_quote"
    assert "price" in tool.description.lower()
    assert "news" in tool.description.lower()
    assert "investment recommendations" in tool.description.lower()

