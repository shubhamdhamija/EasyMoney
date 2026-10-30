import pytest
from pydantic import ValidationError

from app.models.recent_news import NewsArticle, RecentNewsInput, RecentNewsResult


def test_news_symbol_is_normalized() -> None:
    tool_input = RecentNewsInput(symbol=" aapl ", maximum_items=3)
    assert tool_input.symbol == "AAPL"
    assert tool_input.maximum_items == 3


def test_news_limit_defaults_to_three() -> None:
    tool_input = RecentNewsInput(symbol="MSFT")
    assert tool_input.maximum_items == 3


def test_news_limit_above_five_is_rejected() -> None:
    with pytest.raises(ValidationError):
        RecentNewsInput(symbol="AAPL", maximum_items=10)


def test_blank_headline_is_rejected() -> None:
    with pytest.raises(ValidationError):
        NewsArticle(
            headline=" ",
            source="Controlled News",
            published_at="2026-10-27T14:00:00Z",
        )


def test_recent_news_result_accepts_articles() -> None:
    result = RecentNewsResult(
        symbol="AAPL",
        articles=[
            NewsArticle(
                headline="Quarterly results were published.",
                source="Controlled News",
                published_at="2026-10-27T14:00:00Z",
            )
        ],
    )
    assert result.symbol == "AAPL"
    assert len(result.articles) == 1

