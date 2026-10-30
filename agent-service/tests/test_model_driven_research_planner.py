import pytest

from app.models.planner_decision import PlannerDecision
from app.models.tool_definition import ToolDefinition
from app.services.fake_decision_model import FakeDecisionModel
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.tools.default_registry import create_default_registry
from app.tools.fake_recent_news_tool import FakeRecentNewsTool
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.tool_registry import ToolRegistry


def quote_decision(symbol: str = "AAPL") -> PlannerDecision:
    return PlannerDecision(
        action="call_tool",
        tool_name="get_stock_quote",
        arguments={"symbol": symbol},
        reason="The current quote is required.",
    )


def news_decision(symbol: str = "AAPL", maximum_items: int | None = 2) -> PlannerDecision:
    arguments: dict = {"symbol": symbol}
    if maximum_items is not None:
        arguments["maximum_items"] = maximum_items
    return PlannerDecision(
        action="call_tool",
        tool_name="get_recent_news",
        arguments=arguments,
        reason="Recent news may provide context.",
    )


def finish_decision() -> PlannerDecision:
    return PlannerDecision(action="finish", reason="Enough information collected.")


class EndlessDecisionModel:
    """A broken or malicious model that never finishes."""

    def __init__(self, decision: PlannerDecision) -> None:
        self._decision = decision
        self.call_count = 0

    async def decide(self, state, available_tools=None) -> PlannerDecision:
        self.call_count += 1
        return self._decision.model_copy(deep=True)


def make_planner(decision_model, registry: ToolRegistry | None = None):
    return ModelDrivenResearchPlanner(
        registry=registry or create_default_registry(),
        decision_model=decision_model,
    )


@pytest.mark.asyncio
async def test_model_driven_planner_gets_quote_then_finishes() -> None:
    decision_model = FakeDecisionModel(decisions=[quote_decision(), finish_decision()])

    state = await make_planner(decision_model).research_stock("AAPL")

    assert state.symbol == "AAPL"
    assert state.quote is not None
    assert state.news is None
    assert state.error is None
    assert state.agent_used is True
    assert state.tool_call_count == 1

    assert len(state.steps) == 1
    assert state.steps[0].tool_name == "get_stock_quote"
    assert state.steps[0].status == "completed"

    assert len(decision_model.calls) == 2
    assert decision_model.calls[0].quote is None
    assert decision_model.calls[1].quote is not None
    assert decision_model.calls[1].tool_call_count == 1


@pytest.mark.asyncio
async def test_model_driven_planner_gets_quote_and_news_then_finishes() -> None:
    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), news_decision(), finish_decision()]
    )

    state = await make_planner(decision_model).research_stock("AAPL")

    assert state.quote is not None
    assert state.news is not None
    assert state.error is None
    assert state.agent_used is True
    assert state.tool_call_count == 2

    assert [(s.tool_name, s.status) for s in state.steps] == [
        ("get_stock_quote", "completed"),
        ("get_recent_news", "completed"),
    ]

    articles = state.news["articles"]
    assert len(articles) == 2
    assert articles[0]["headline"] == "Quarterly results were published."


@pytest.mark.asyncio
async def test_model_sees_updated_state_snapshots() -> None:
    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), news_decision(), finish_decision()]
    )

    await make_planner(decision_model).research_stock("AAPL")

    first, second, third = decision_model.calls

    assert first.quote is None and first.news is None
    assert first.tool_call_count == 0 and first.decision_count == 0

    assert second.quote is not None and second.news is None
    assert second.tool_call_count == 1 and second.decision_count == 1

    assert third.quote is not None and third.news is not None
    assert third.tool_call_count == 2 and third.decision_count == 2


@pytest.mark.asyncio
async def test_model_receives_registered_tool_definitions() -> None:
    decision_model = FakeDecisionModel(decisions=[finish_decision()])

    await make_planner(decision_model).research_stock("AAPL")

    tools = decision_model.available_tools_seen[0]
    assert all(isinstance(tool, ToolDefinition) for tool in tools)
    assert [tool.name for tool in tools] == ["get_recent_news", "get_stock_quote"]


@pytest.mark.asyncio
async def test_finish_with_no_tool_calls() -> None:
    decision_model = FakeDecisionModel(decisions=[finish_decision()])

    state = await make_planner(decision_model).research_stock("AAPL")

    assert state.tool_call_count == 0
    assert state.steps == []
    assert state.quote is None
    assert state.error is not None
    assert state.error.code == "QUOTE_NOT_COLLECTED"
    assert state.decision_count == 1


@pytest.mark.asyncio
async def test_model_driven_planner_rejects_unknown_tool() -> None:
    decision_model = FakeDecisionModel(
        decisions=[
            PlannerDecision(
                action="call_tool",
                tool_name="delete_database",
                arguments={},
                reason="Attempt to call an unapproved tool.",
            )
        ]
    )

    state = await make_planner(decision_model).research_stock("AAPL")

    assert state.quote is None
    assert state.news is None
    assert state.error is not None
    assert state.error.code == "UNKNOWN_TOOL"
    assert state.tool_call_count == 1
    assert [(s.tool_name, s.status) for s in state.steps] == [
        ("delete_database", "failed")
    ]


@pytest.mark.asyncio
async def test_model_cannot_bypass_registry_to_run_unregistered_tool() -> None:
    quote_tool = FakeStockQuoteTool()
    unregistered_news_tool = FakeRecentNewsTool()

    registry = ToolRegistry()
    registry.register(quote_tool)

    decision_model = FakeDecisionModel(decisions=[news_decision(), finish_decision()])

    state = await make_planner(decision_model, registry).research_stock("AAPL")

    assert state.error is not None
    assert state.error.code == "UNKNOWN_TOOL"
    assert unregistered_news_tool.calls == []
    assert quote_tool.calls == []
    assert state.news is None


@pytest.mark.asyncio
async def test_invalid_arguments_are_rejected_before_tool_runs() -> None:
    quote_tool = FakeStockQuoteTool()
    registry = ToolRegistry()
    registry.register(quote_tool)

    decision_model = FakeDecisionModel(
        decisions=[
            PlannerDecision(
                action="call_tool",
                tool_name="get_stock_quote",
                arguments={"ticker": "AAPL"},
                reason="Wrong argument name.",
            )
        ]
    )

    state = await make_planner(decision_model, registry).research_stock("AAPL")

    assert state.error is not None
    assert state.error.code == "INVALID_TOOL_ARGUMENTS"
    assert state.quote is None
    assert quote_tool.calls == []
    assert state.steps[0].status == "failed"


@pytest.mark.asyncio
async def test_symbol_mismatch_is_rejected_without_executing_tool() -> None:
    quote_tool = FakeStockQuoteTool()
    registry = ToolRegistry()
    registry.register(quote_tool)

    decision_model = FakeDecisionModel(decisions=[quote_decision("MSFT")])

    state = await make_planner(decision_model, registry).research_stock("AAPL")

    assert state.error is not None
    assert state.error.code == "INVALID_TOOL_ARGUMENTS"
    assert state.quote is None
    assert state.tool_call_count == 0
    assert quote_tool.calls == []


@pytest.mark.asyncio
async def test_tool_business_error_is_preserved() -> None:
    decision_model = FakeDecisionModel(
        decisions=[quote_decision("UNKNOWN"), finish_decision()]
    )

    state = await make_planner(decision_model).research_stock("UNKNOWN")

    assert state.error is not None
    assert state.error.code == "QUOTE_NOT_FOUND"
    assert state.tool_call_count == 1
    assert state.steps[0].status == "failed"


@pytest.mark.asyncio
async def test_news_business_error_is_preserved_with_quote() -> None:
    decision_model = FakeDecisionModel(
        decisions=[quote_decision("SHOP.TO"), news_decision("SHOP.TO")]
    )

    state = await make_planner(decision_model).research_stock("SHOP.TO")

    assert state.quote is not None
    assert state.news is None
    assert state.error is not None
    assert state.error.code == "NEWS_NOT_FOUND"


@pytest.mark.asyncio
async def test_model_driven_planner_stops_at_tool_call_limit() -> None:
    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), news_decision(), finish_decision()]
    )

    state = await make_planner(decision_model).research_stock(
        "AAPL", maximum_tool_calls=1
    )

    assert state.quote is not None
    assert state.news is None
    assert state.error is not None
    assert state.error.code == "TOOL_CALL_LIMIT_REACHED"
    assert state.tool_call_count == 1

    assert [(s.tool_name, s.status) for s in state.steps] == [
        ("get_stock_quote", "completed"),
        ("get_recent_news", "failed"),
    ]
    assert len(decision_model.calls) == 2


@pytest.mark.asyncio
async def test_decision_limit_stops_endless_model() -> None:
    model = EndlessDecisionModel(quote_decision())

    state = await make_planner(model).research_stock("AAPL", maximum_decisions=3)

    assert state.error is not None
    assert state.error.code == "DECISION_LIMIT_REACHED"
    assert model.call_count == 3
    assert state.decision_count == 3
    assert state.tool_call_count == 1


@pytest.mark.asyncio
async def test_decision_limit_and_tool_limit_are_independent() -> None:
    model = EndlessDecisionModel(quote_decision())

    state = await make_planner(model).research_stock(
        "AAPL", maximum_tool_calls=1, maximum_decisions=20
    )

    # Duplicates are skipped without using tool budget, so the decision limit
    # is what finally stops the run.
    assert state.error is not None
    assert state.error.code == "DECISION_LIMIT_REACHED"
    assert model.call_count == 20
    assert state.tool_call_count == 1


@pytest.mark.asyncio
async def test_duplicate_quote_request_is_skipped() -> None:
    quote_tool = FakeStockQuoteTool()
    registry = ToolRegistry()
    registry.register(quote_tool)

    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), quote_decision(), finish_decision()]
    )

    state = await make_planner(decision_model, registry).research_stock("AAPL")

    assert state.error is None
    assert len(quote_tool.calls) == 1
    assert state.tool_call_count == 1
    assert [(s.tool_name, s.status) for s in state.steps] == [
        ("get_stock_quote", "completed"),
        ("get_stock_quote", "skipped"),
    ]
    assert len(decision_model.calls) == 3


@pytest.mark.asyncio
async def test_duplicate_news_request_is_skipped() -> None:
    news_tool = FakeRecentNewsTool()
    registry = ToolRegistry()
    registry.register(FakeStockQuoteTool())
    registry.register(news_tool)

    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), news_decision(), news_decision(), finish_decision()]
    )

    state = await make_planner(decision_model, registry).research_stock("AAPL")

    assert state.error is None
    assert len(news_tool.calls) == 1
    assert state.tool_call_count == 2
    assert state.steps[-1].tool_name == "get_recent_news"
    assert state.steps[-1].status == "skipped"


@pytest.mark.asyncio
async def test_news_item_request_is_capped_by_application_limit() -> None:
    news_tool = FakeRecentNewsTool()
    registry = ToolRegistry()
    registry.register(FakeStockQuoteTool())
    registry.register(news_tool)

    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), news_decision(maximum_items=5), finish_decision()]
    )

    state = await make_planner(decision_model, registry).research_stock(
        "AAPL", maximum_news_items=1
    )

    assert news_tool.calls[0].maximum_items == 1
    assert len(state.news["articles"]) == 1


@pytest.mark.asyncio
async def test_missing_news_item_count_uses_application_limit() -> None:
    news_tool = FakeRecentNewsTool()
    registry = ToolRegistry()
    registry.register(FakeStockQuoteTool())
    registry.register(news_tool)

    decision_model = FakeDecisionModel(
        decisions=[quote_decision(), news_decision(maximum_items=None), finish_decision()]
    )

    await make_planner(decision_model, registry).research_stock(
        "AAPL", maximum_news_items=2
    )

    assert news_tool.calls[0].maximum_items == 2

