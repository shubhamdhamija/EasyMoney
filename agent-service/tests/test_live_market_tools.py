import asyncio

import httpx
import pytest

from app.models.recent_news import RecentNewsInput
from app.models.stock_quote import StockQuoteInput
from app.tools.live_market_tools import LiveRecentNewsTool, LiveStockQuoteTool
from app.tools.tool_errors import ToolError


def _transport(handler):
    return httpx.MockTransport(handler)


def test_live_quote_maps_provider_payload() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path.endswith("/ORCL")
        return httpx.Response(
            200,
            json={
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "regularMarketPrice": 126.19,
                                "chartPreviousClose": 130.5,
                                "currency": "USD",
                                "regularMarketTime": 1762203864,
                            }
                        }
                    ]
                }
            },
        )

    tool = LiveStockQuoteTool(transport=_transport(handler))
    result = asyncio.run(tool.execute(StockQuoteInput(symbol="orcl")))

    assert result.symbol == "ORCL"
    assert result.current_price == 126.19
    assert result.price_change_percent == pytest.approx(-3.30, abs=0.01)
    assert result.currency == "USD"


def test_live_quote_unknown_symbol_raises_quote_not_found() -> None:
    tool = LiveStockQuoteTool(transport=_transport(lambda r: httpx.Response(404, json={})))

    with pytest.raises(ToolError) as error:
        asyncio.run(tool.execute(StockQuoteInput(symbol="ZZZZ")))

    assert error.value.code == "QUOTE_NOT_FOUND"


def test_live_quote_provider_failure_is_a_safe_tool_error() -> None:
    tool = LiveStockQuoteTool(transport=_transport(lambda r: httpx.Response(500)))

    with pytest.raises(ToolError) as error:
        asyncio.run(tool.execute(StockQuoteInput(symbol="AAPL")))

    assert error.value.code == "QUOTE_PROVIDER_UNAVAILABLE"


def test_live_news_maps_and_limits_articles() -> None:
    payload = {
        "news": [
            {"title": f"Headline {i}", "publisher": "Wire", "providerPublishTime": 1762200000 + i}
            for i in range(5)
        ]
        + [{"title": "", "publisher": "Wire", "providerPublishTime": 1}]
    }
    tool = LiveRecentNewsTool(transport=_transport(lambda r: httpx.Response(200, json=payload)))

    result = asyncio.run(tool.execute(RecentNewsInput(symbol="AAPL", maximum_items=3)))

    assert [a.headline for a in result.articles] == ["Headline 0", "Headline 1", "Headline 2"]

