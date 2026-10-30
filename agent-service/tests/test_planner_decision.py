import pytest
from pydantic import ValidationError

from app.models.planner_decision import PlannerDecision


def test_accepts_call_tool_decision() -> None:
    decision = PlannerDecision(
        action="call_tool",
        tool_name="get_stock_quote",
        arguments={"symbol": "AAPL"},
        reason="The current quote is required.",
    )

    assert decision.action == "call_tool"
    assert decision.tool_name == "get_stock_quote"
    assert decision.arguments == {"symbol": "AAPL"}


def test_accepts_finish_decision() -> None:
    decision = PlannerDecision(
        action="finish",
        reason="The required information has been collected.",
    )

    assert decision.action == "finish"
    assert decision.tool_name is None
    assert decision.arguments == {}


def test_call_tool_requires_tool_name() -> None:
    with pytest.raises(ValidationError):
        PlannerDecision(
            action="call_tool",
            tool_name=None,
            arguments={"symbol": "AAPL"},
            reason="Get the quote.",
        )


def test_finish_rejects_tool_name() -> None:
    with pytest.raises(ValidationError):
        PlannerDecision(
            action="finish",
            tool_name="get_stock_quote",
            reason="Finished.",
        )


def test_finish_rejects_arguments() -> None:
    with pytest.raises(ValidationError):
        PlannerDecision(
            action="finish",
            arguments={"symbol": "AAPL"},
            reason="Finished.",
        )


def test_rejects_unknown_action() -> None:
    with pytest.raises(ValidationError):
        PlannerDecision(
            action="delete_everything",
            reason="Invalid action.",
        )

