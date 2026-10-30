from datetime import datetime, timezone

from app.models.stock_quote import StockQuoteInput, StockQuoteResult
from app.tools.tool_errors import ToolError


class FakeStockQuoteTool:
    name = "get_stock_quote"
    input_model = StockQuoteInput

    description = (
        "Returns the latest available price and daily movement for one validated stock symbol. "
        "This tool does not return news, earnings, predictions, or investment recommendations."
    )

    def __init__(self) -> None:
        self._quotes = {
            "AAPL": {
                "current_price": 228.30,
                "price_change_percent": 2.40,
                "currency": "USD",
            },
            "MSFT": {
                "current_price": 419.20,
                "price_change_percent": 0.80,
                "currency": "USD",
            },
            "SHOP.TO": {
                "current_price": 153.75,
                "price_change_percent": -1.25,
                "currency": "CAD",
            },
        }

        self.calls: list[StockQuoteInput] = []

    async def execute(self, tool_input: StockQuoteInput) -> StockQuoteResult:
        self.calls.append(tool_input)

        quote = self._quotes.get(tool_input.symbol)

        if quote is None:
            raise ToolError(
                code="QUOTE_NOT_FOUND",
                message="No quote is available for the requested symbol.",
            )

        return StockQuoteResult(
            symbol=tool_input.symbol,
            current_price=quote["current_price"],
            price_change_percent=quote["price_change_percent"],
            currency=quote["currency"],
            data_timestamp=datetime.now(timezone.utc),
        )
