import pytest

from app.models.planner_decision import PlannerDecision
from app.models.research_state import ResearchState
from app.services.fake_decision_model import FakeDecisionModel


@pytest.mark.asyncio
async def test_returns_configured_decision() -> None:
    expected_decision = PlannerDecision(
        action="call_tool",
        tool_name="get_stock_quote",
        arguments={"symbol": "AAPL"},
        reason="The current quote is required.",
    )

    model = FakeDecisionModel(decisions=[expected_decision])
    state = ResearchState(symbol="AAPL")

    result = await model.decide(state)

    assert result == expected_decision


@pytest.mark.asyncio
async def test_returns_decisions_in_order() -> None:
    quote_decision = PlannerDecision(
        action="call_tool",
        tool_name="get_stock_quote",
        arguments={"symbol": "AAPL"},
        reason="Get the quote.",
    )
    finish_decision = PlannerDecision(
        action="finish",
        reason="Research is complete.",
    )

    model = FakeDecisionModel(decisions=[quote_decision, finish_decision])
    state = ResearchState(symbol="AAPL")

    first = await model.decide(state)
    second = await model.decide(state)

    assert first.action == "call_tool"
    assert first.tool_name == "get_stock_quote"
    assert second.action == "finish"
    assert second.tool_name is None


@pytest.mark.asyncio
async def test_finishes_when_decisions_are_empty() -> None:
    model = FakeDecisionModel(decisions=[])
    state = ResearchState(symbol="AAPL")

    result = await model.decide(state)

    assert result.action == "finish"
    assert result.tool_name is None
    assert result.arguments == {}


@pytest.mark.asyncio
async def test_records_decision_calls() -> None:
    model = FakeDecisionModel(
        decisions=[PlannerDecision(action="finish", reason="Finished.")]
    )
    state = ResearchState(symbol="AAPL")

    await model.decide(state)

    assert len(model.calls) == 1
    assert model.calls[0].symbol == "AAPL"
    assert model.calls[0].tool_call_count == 0


@pytest.mark.asyncio
async def test_records_state_snapshot() -> None:
    model = FakeDecisionModel(
        decisions=[PlannerDecision(action="finish", reason="Finished.")]
    )
    state = ResearchState(symbol="AAPL")

    await model.decide(state)
    state.tool_call_count = 3

    assert model.calls[0].tool_call_count == 0

