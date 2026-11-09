"""Live market-data tools backed by Yahoo Finance public endpoints.

These replace the hard-coded fake quote/news tools when AGENT_LIVE_MARKET_DATA=true.
They only return data fetched from the provider; nothing is invented.
"""
from __future__ import annotations

from datetime import datetime, timezone

import httpx

from app.models.recent_news import NewsArticle, RecentNewsInput, RecentNewsResult
from app.models.stock_quote import StockQuoteInput, StockQuoteResult
from app.tools.tool_errors import ToolError

_HEADERS = {"User-Agent": "Mozilla/5.0 (EasyMoney agent-service)"}
_TIMEOUT = httpx.Timeout(10.0)


class LiveStockQuoteTool:
    name = "get_stock_quote"
    input_model = StockQuoteInput
    requires_approval = False

    description = (
        "Returns the latest available price and daily movement for one validated stock symbol. "
        "This tool does not return news, earnings, predictions, or investment recommendations."
    )

    def __init__(self, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self._transport = transport
        self.calls: list[StockQuoteInput] = []

    async def execute(self, tool_input: StockQuoteInput) -> StockQuoteResult:
        self.calls.append(tool_input)
        url = f"https://query1.finance.yahoo.com/v8/finance/chart/{tool_input.symbol}"
        try:
            async with httpx.AsyncClient(
                timeout=_TIMEOUT, headers=_HEADERS, transport=self._transport
            ) as client:
                response = await client.get(url, params={"interval": "1d", "range": "5d"})
        except httpx.HTTPError as error:
            raise ToolError(
                code="QUOTE_PROVIDER_UNAVAILABLE",
                message="The quote provider could not be reached.",
            ) from error

        if response.status_code == 404:
            raise ToolError(code="QUOTE_NOT_FOUND", message="No quote is available for the requested symbol.")
        if response.status_code != 200:
            raise ToolError(
                code="QUOTE_PROVIDER_UNAVAILABLE",
                message="The quote provider returned an error.",
            )

        try:
            meta = response.json()["chart"]["result"][0]["meta"]
            price = float(meta["regularMarketPrice"])
            previous = meta.get("chartPreviousClose") or meta.get("previousClose")
            currency = str(meta.get("currency") or "USD").upper()[:3]
            timestamp = meta.get("regularMarketTime")
        except (KeyError, IndexError, TypeError, ValueError) as error:
            raise ToolError(
                code="QUOTE_NOT_FOUND",
                message="No quote is available for the requested symbol.",
            ) from error

        change_percent = 0.0
        if previous:
            change_percent = round((price - float(previous)) / float(previous) * 100, 2)

        data_timestamp = (
            datetime.fromtimestamp(timestamp, tz=timezone.utc)
            if isinstance(timestamp, (int, float))
            else datetime.now(timezone.utc)
        )

        return StockQuoteResult(
            symbol=tool_input.symbol,
            current_price=price,
            price_change_percent=change_percent,
            currency=currency if len(currency) == 3 else "USD",
            data_timestamp=data_timestamp,
        )


class LiveRecentNewsTool:
    name = "get_recent_news"
    input_model = RecentNewsInput
    requires_approval = False

    description = (
        "Returns a limited list of recent news articles for one validated stock symbol. "
        "This tool does not return stock prices, earnings, predictions, or investment recommendations."
    )

    def __init__(self, transport: httpx.AsyncBaseTransport | None = None) -> None:
        self._transport = transport
        self.calls: list[RecentNewsInput] = []

    async def execute(self, tool_input: RecentNewsInput) -> RecentNewsResult:
        self.calls.append(tool_input)
        try:
            async with httpx.AsyncClient(
                timeout=_TIMEOUT, headers=_HEADERS, transport=self._transport
            ) as client:
                response = await client.get(
                    "https://query1.finance.yahoo.com/v1/finance/search",
                    params={
                        "q": tool_input.symbol,
                        "quotesCount": 0,
                        "newsCount": tool_input.maximum_items,
                    },
                )
        except httpx.HTTPError as error:
            raise ToolError(
                code="NEWS_PROVIDER_UNAVAILABLE",
                message="The news provider could not be reached.",
            ) from error

        if response.status_code != 200:
            raise ToolError(
                code="NEWS_PROVIDER_UNAVAILABLE",
                message="The news provider returned an error.",
            )

        articles: list[NewsArticle] = []
        for item in (response.json().get("news") or [])[: tool_input.maximum_items]:
            title = (item.get("title") or "").strip()
            publisher = (item.get("publisher") or "").strip()
            published = item.get("providerPublishTime")
            if not title or not publisher or not isinstance(published, (int, float)):
                continue
            articles.append(
                NewsArticle(
                    headline=title[:500],
                    source=publisher[:200],
                    published_at=datetime.fromtimestamp(published, tz=timezone.utc),
                )
            )

        return RecentNewsResult(symbol=tool_input.symbol, articles=articles)

