import pytest

from app.langgraph_agent import build_research_graph, run_research_graph
from app.models.planner_decision import PlannerDecision
from app.services.fake_decision_model import FakeDecisionModel
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.services.research_report_builder import build_research_report
from app.tools.default_registry import create_default_registry


def quote_decision(symbol: str = "AAPL") -> PlannerDecision:
    return PlannerDecision(
        action="call_tool",
        tool_name="get_stock_quote",
        arguments={"symbol": symbol},
        reason="The current quote is required.",
    )


def news_decision(symbol: str = "AAPL") -> PlannerDecision:
    return PlannerDecision(
        action="call_tool",
        tool_name="get_recent_news",
        arguments={"symbol": symbol, "maximum_items": 10},
        reason="News adds context.",
    )


def finish_decision() -> PlannerDecision:
    return PlannerDecision(action="finish", reason="Enough information collected.")


@pytest.mark.asyncio
async def test_graph_runs_quote_only_workflow() -> None:
    model = FakeDecisionModel([quote_decision(), finish_decision()])

    state, report = await run_research_graph(
        symbol=" aapl ",
        registry=create_default_registry(),
        decision_model=model,
    )

    assert state.symbol == "AAPL"
    assert state.error is None
    assert state.quote is not None
    assert state.news is None
    assert state.decision_count == 2
    assert state.tool_call_count == 1
    assert [(s.tool_name, s.status) for s in state.steps] == [
        ("get_stock_quote", "completed")
    ]
    assert report.status == "completed"
    assert report.tools_used == ["get_stock_quote"]
    assert report.agent_used is True
    assert len(model.calls) == 2


@pytest.mark.asyncio
async def test_graph_exposes_expected_nodes() -> None:
    compiled = build_research_graph(
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([]),
    )

    nodes = set(compiled.get_graph().nodes)

    assert {"decide", "execute_tool", "finalize_report"} <= nodes


@pytest.mark.asyncio
async def test_graph_finish_without_quote_fails_safely() -> None:
    state, report = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([finish_decision()]),
    )

    assert state.error is not None
    assert state.error.code == "QUOTE_NOT_COLLECTED"
    assert report.status == "failed"


@pytest.mark.asyncio
async def test_graph_rejects_unknown_tool() -> None:
    bad = PlannerDecision(
        action="call_tool",
        tool_name="place_order",
        arguments={"symbol": "AAPL"},
        reason="Try to trade.",
    )

    state, report = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([bad]),
    )

    assert state.error is not None
    assert state.error.code == "UNKNOWN_TOOL"
    assert report.status == "failed"


@pytest.mark.asyncio
async def test_graph_blocks_cross_symbol_arguments() -> None:
    state, _ = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([quote_decision("MSFT")]),
    )

    assert state.error is not None
    assert state.error.code == "INVALID_TOOL_ARGUMENTS"
    assert state.quote is None
    assert state.tool_call_count == 0


@pytest.mark.asyncio
async def test_graph_enforces_decision_limit_without_recursion_error() -> None:
    class EndlessModel:
        async def decide(self, state, available_tools=None):
            return news_decision()

    state, report = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=EndlessModel(),
        maximum_decisions=3,
    )

    assert state.error is not None
    assert state.error.code == "DECISION_LIMIT_REACHED"
    assert state.decision_count == 3
    assert report.status == "failed"


@pytest.mark.asyncio
async def test_graph_enforces_tool_limit() -> None:
    state, _ = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([quote_decision(), news_decision()]),
        maximum_tool_calls=1,
    )

    assert state.error is not None
    assert state.error.code == "TOOL_CALL_LIMIT_REACHED"
    assert state.tool_call_count == 1


@pytest.mark.asyncio
async def test_graph_skips_duplicate_calls_without_spending_budget() -> None:
    state, report = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel(
            [quote_decision(), quote_decision(), finish_decision()]
        ),
    )

    assert state.tool_call_count == 1
    assert [s.status for s in state.steps] == ["completed", "skipped"]
    assert report.status == "completed"


@pytest.mark.asyncio
async def test_graph_matches_custom_planner_for_quote_and_news() -> None:
    script = [quote_decision(), news_decision(), finish_decision()]

    custom_state = await ModelDrivenResearchPlanner(
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([d.model_copy(deep=True) for d in script]),
    ).research_stock("AAPL")
    graph_state, graph_report = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([d.model_copy(deep=True) for d in script]),
    )

    # Fake tools stamp the current time, so compare everything except timestamps.
    assert graph_state.steps == custom_state.steps
    assert graph_state.error == custom_state.error
    assert graph_state.decision_count == custom_state.decision_count
    assert graph_state.tool_call_count == custom_state.tool_call_count
    assert graph_state.quote["current_price"] == custom_state.quote["current_price"]
    assert len(graph_state.news["articles"]) == len(custom_state.news["articles"]) == 3
    custom_report = build_research_report(custom_state)
    assert graph_report.status == custom_report.status
    assert graph_report.summary == custom_report.summary
    assert graph_report.tools_used == custom_report.tools_used


