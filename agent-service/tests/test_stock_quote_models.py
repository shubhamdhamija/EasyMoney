import pytest
from pydantic import ValidationError

from app.models.stock_quote import StockQuoteInput, StockQuoteResult


def test_symbol_is_trimmed_and_uppercased() -> None:
    tool_input = StockQuoteInput(symbol=" aapl ")
    assert tool_input.symbol == "AAPL"


def test_symbol_with_period_is_allowed() -> None:
    tool_input = StockQuoteInput(symbol="brk.b")
    assert tool_input.symbol == "BRK.B"


def test_blank_symbol_is_rejected() -> None:
    with pytest.raises(ValidationError):
        StockQuoteInput(symbol=" ")


def test_symbol_with_unsupported_characters_is_rejected() -> None:
    with pytest.raises(ValidationError):
        StockQuoteInput(symbol="AAPL; DELETE")


def test_negative_price_is_rejected() -> None:
    with pytest.raises(ValidationError):
        StockQuoteResult(
            symbol="AAPL",
            current_price=-100.0,
            price_change_percent=2.4,
            currency="USD",
            data_timestamp="2026-10-27T14:30:00Z",
        )

