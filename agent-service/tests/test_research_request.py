import pytest
from pydantic import ValidationError

from app.models.research_request import ResearchRequest


def test_research_request_normalizes_symbol() -> None:
    request = ResearchRequest(symbol=" aapl ")

    assert request.symbol == "AAPL"


def test_research_request_uses_safe_defaults() -> None:
    request = ResearchRequest(symbol="MSFT")

    assert request.include_news is None
    assert request.maximum_news_items == 3
    assert request.maximum_tool_calls == 5


def test_research_request_accepts_explicit_news_choice() -> None:
    request = ResearchRequest(
        symbol="AAPL",
        include_news=True,
        maximum_news_items=2,
        maximum_tool_calls=3,
    )

    assert request.include_news is True
    assert request.maximum_news_items == 2
    assert request.maximum_tool_calls == 3


def test_research_request_rejects_invalid_symbol() -> None:
    with pytest.raises(ValidationError):
        ResearchRequest(symbol="AAPL; DELETE")


def test_research_request_rejects_excessive_news_limit() -> None:
    with pytest.raises(ValidationError):
        ResearchRequest(symbol="AAPL", maximum_news_items=20)


def test_research_request_rejects_zero_tool_limit() -> None:
    with pytest.raises(ValidationError):
        ResearchRequest(symbol="AAPL", maximum_tool_calls=0)

