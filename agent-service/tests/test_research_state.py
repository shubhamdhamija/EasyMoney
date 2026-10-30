import pytest
from pydantic import ValidationError

from app.models.research_state import (
    ResearchError,
    ResearchState,
    ResearchStep,
)


def test_initial_research_state() -> None:
    state = ResearchState(symbol="AAPL")

    assert state.symbol == "AAPL"
    assert state.steps == []
    assert state.quote is None
    assert state.news is None
    assert state.error is None
    assert state.agent_used is False
    assert state.tool_call_count == 0
    assert state.maximum_tool_calls == 5


def test_state_accepts_completed_step() -> None:
    state = ResearchState(
        symbol="AAPL",
        steps=[
            ResearchStep(
                tool_name="get_stock_quote",
                status="completed",
            )
        ],
        tool_call_count=1,
    )

    assert len(state.steps) == 1
    assert state.steps[0].tool_name == "get_stock_quote"
    assert state.steps[0].status == "completed"
    assert state.tool_call_count == 1


def test_state_accepts_structured_error() -> None:
    state = ResearchState(
        symbol="UNKNOWN",
        error=ResearchError(
            code="QUOTE_NOT_FOUND",
            message=(
                "No quote is available for "
                "the requested symbol."
            ),
        ),
    )

    assert state.error is not None
    assert state.error.code == "QUOTE_NOT_FOUND"


def test_invalid_step_status_is_rejected() -> None:
    with pytest.raises(ValidationError):
        ResearchStep(
            tool_name="get_stock_quote",
            status="unknown",
        )


def test_negative_tool_count_is_rejected() -> None:
    with pytest.raises(ValidationError):
        ResearchState(
            symbol="AAPL",
            tool_call_count=-1,
        )


def test_excessive_tool_limit_is_rejected() -> None:
    with pytest.raises(ValidationError):
        ResearchState(
            symbol="AAPL",
            maximum_tool_calls=100,
        )

