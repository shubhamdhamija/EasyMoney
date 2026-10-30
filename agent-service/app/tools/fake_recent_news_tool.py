from datetime import datetime, timezone

from app.models.recent_news import NewsArticle, RecentNewsInput, RecentNewsResult
from app.tools.tool_errors import ToolError


class FakeRecentNewsTool:
    name = "get_recent_news"
    input_model = RecentNewsInput

    description = (
        "Returns a limited list of recent news articles for one validated stock symbol. "
        "This tool does not return stock prices, earnings, predictions, or investment recommendations."
    )

    def __init__(self) -> None:
        self._articles = {
            "AAPL": [
                {
                    "headline": "Quarterly results were published.",
                    "source": "Controlled News",
                    "published_at": datetime(2026, 10, 27, 14, 0, tzinfo=timezone.utc),
                },
                {
                    "headline": "The company announced a software update.",
                    "source": "Controlled Technology News",
                    "published_at": datetime(2026, 10, 26, 18, 30, tzinfo=timezone.utc),
                },
                {
                    "headline": "The company published an operational update.",
                    "source": "Controlled Business News",
                    "published_at": datetime(2026, 10, 25, 16, 0, tzinfo=timezone.utc),
                },
            ],
            "MSFT": [
                {
                    "headline": "The company published its quarterly results.",
                    "source": "Controlled News",
                    "published_at": datetime(2026, 10, 27, 13, 0, tzinfo=timezone.utc),
                }
            ],
        }

        self.calls: list[RecentNewsInput] = []

    async def execute(self, tool_input: RecentNewsInput) -> RecentNewsResult:
        self.calls.append(tool_input)

        articles = self._articles.get(tool_input.symbol)

        if articles is None:
            raise ToolError(
                code="NEWS_NOT_FOUND",
                message="No recent news is available for the requested symbol.",
            )

        selected_articles = articles[: tool_input.maximum_items]

        return RecentNewsResult(
            symbol=tool_input.symbol,
            articles=[
                NewsArticle(
                    headline=article["headline"],
                    source=article["source"],
                    published_at=article["published_at"],
                )
                for article in selected_articles
            ],
        )

