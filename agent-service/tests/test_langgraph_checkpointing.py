"""Checkpointing, pause and resume for the LangGraph research workflow."""

import json
from typing import Any

import pytest
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.errors import EmptyInputError
from app.langgraph_agent import (
    create_research_graph,
    initial_graph_input,
    run_research_graph,
    thread_config,
)
from app.models.planner_decision import PlannerDecision
from app.services.fake_decision_model import FakeDecisionModel
from app.tools.default_registry import create_default_registry
from app.tools.tool_registry import ToolRegistry


class CountingRegistry(ToolRegistry):
    """Records every real tool execution."""

    def __init__(self) -> None:
        super().__init__()
        self.executions: list[str] = []

    async def execute(self, tool_name: str, arguments: dict[str, Any], **kwargs: Any):
        self.executions.append(tool_name)
        return await super().execute(tool_name, arguments, **kwargs)


def counting_registry() -> CountingRegistry:
    base = create_default_registry()
    registry = CountingRegistry()
    for tool in base._tools.values():
        registry.register(tool)
    return registry


def call(tool_name: str, **arguments: Any) -> PlannerDecision:
    return PlannerDecision(
        action="call_tool",
        tool_name=tool_name,
        arguments=arguments,
        reason="Needed for the research.",
    )


def finish() -> PlannerDecision:
    return PlannerDecision(action="finish", reason="Done.")


def quote() -> PlannerDecision:
    return call("get_stock_quote", symbol="AAPL")


def news() -> PlannerDecision:
    return call("get_recent_news", symbol="AAPL", maximum_items=2)


def make_graph(decisions, registry=None, saver=None):
    registry = registry or counting_registry()
    saver = saver or InMemorySaver()
    graph = create_research_graph(
        registry=registry,
        decision_model=FakeDecisionModel(decisions),
        checkpointer=saver,
    )
    return graph, registry, saver


async def run_until_quote_pause(graph, thread_id="research-aapl-001", **overrides):
    return await graph.ainvoke(
        initial_graph_input(symbol="AAPL", **overrides),
        thread_config(thread_id),
        interrupt_after=["execute_tool"],
    )


# ---- Part 1: checkpointing -------------------------------------------------


@pytest.mark.asyncio
async def test_checkpoint_is_created_for_thread_id() -> None:
    graph, _, saver = make_graph([quote(), finish()])

    await graph.ainvoke(initial_graph_input(symbol="AAPL"), thread_config("t-1"))

    assert saver.get_tuple(thread_config("t-1")) is not None


@pytest.mark.asyncio
async def test_checkpoint_contains_quote_then_news() -> None:
    graph, _, _ = make_graph([quote(), news(), finish()])
    config = thread_config("t-1")

    await graph.ainvoke(
        initial_graph_input(symbol="AAPL"), config, interrupt_after=["execute_tool"]
    )
    snapshot = await graph.aget_state(config)
    assert snapshot.values["research"].quote is not None
    assert snapshot.values["research"].news is None

    await graph.ainvoke(None, config, interrupt_after=["execute_tool"])
    snapshot = await graph.aget_state(config)
    assert snapshot.values["research"].news is not None


@pytest.mark.asyncio
async def test_different_thread_ids_do_not_share_state() -> None:
    graph, _, _ = make_graph([quote(), finish(), finish()])

    await run_until_quote_pause(graph, "thread-a")
    other = await graph.aget_state(thread_config("thread-b"))

    assert other.values == {}
    assert other.next == ()


@pytest.mark.asyncio
async def test_checkpoint_contains_no_provider_secrets(monkeypatch) -> None:
    monkeypatch.setenv("GROQ_API_KEY", "gsk_checkpoint_secret_value")
    graph, _, saver = make_graph([quote(), news(), finish()])

    await graph.ainvoke(initial_graph_input(symbol="AAPL"), thread_config("t-1"))
    snapshot = await graph.aget_state(thread_config("t-1"))
    serialized = json.dumps(
        {
            "research": snapshot.values["research"].model_dump(mode="json"),
            "report": snapshot.values["report"].model_dump(mode="json"),
        }
    ).lower()
    stored = repr(saver.storage).lower()

    for text in (serialized, stored):
        assert "gsk_" not in text
        assert "authorization" not in text
        assert "api_key" not in text


@pytest.mark.asyncio
async def test_graph_without_checkpointer_behaves_as_before() -> None:
    state, report = await run_research_graph(
        symbol="AAPL",
        registry=create_default_registry(),
        decision_model=FakeDecisionModel([quote(), finish()]),
    )

    assert state.quote is not None
    assert report.status == "completed"


# ---- Part 2: genuine pause and resume --------------------------------------


@pytest.mark.asyncio
async def test_first_run_pauses_after_quote() -> None:
    graph, registry, _ = make_graph([quote(), news(), finish()])
    config = thread_config("research-aapl-001")

    await run_until_quote_pause(graph)
    snapshot = await graph.aget_state(config)

    assert snapshot.next != ()  # workflow is incomplete
    assert snapshot.config["configurable"]["thread_id"] == "research-aapl-001"
    assert snapshot.values["research"].quote is not None
    assert snapshot.values["research"].news is None
    assert snapshot.values["report"] is None
    assert registry.executions == ["get_stock_quote"]


@pytest.mark.asyncio
async def test_resume_keeps_quote_and_runs_news_once() -> None:
    graph, registry, _ = make_graph([quote(), news(), finish()])
    config = thread_config("research-aapl-001")
    await run_until_quote_pause(graph)

    final = await graph.ainvoke(None, config)

    research = final["research"]
    assert research.quote is not None
    assert research.news is not None
    assert registry.executions == ["get_stock_quote", "get_recent_news"]
    assert (await graph.aget_state(config)).next == ()


@pytest.mark.asyncio
async def test_resume_does_not_repeat_quote_even_if_model_asks_again() -> None:
    graph, registry, _ = make_graph([quote(), quote(), news(), finish()])
    await run_until_quote_pause(graph)

    final = await graph.ainvoke(None, thread_config("research-aapl-001"))

    assert registry.executions.count("get_stock_quote") == 1
    assert [s.status for s in final["research"].steps].count("skipped") == 1


@pytest.mark.asyncio
async def test_resumed_report_is_deterministic_and_hides_checkpoint_internals() -> None:
    graph, _, _ = make_graph([quote(), news(), finish()])
    await run_until_quote_pause(graph)

    final = await graph.ainvoke(None, thread_config("research-aapl-001"))
    report = final["report"]

    assert report.status == "completed"
    assert report.agent_used is True
    assert report.news_count == 2
    dumped = json.dumps(report.model_dump(mode="json")).lower()
    assert "thread" not in dumped
    assert "checkpoint" not in dumped


@pytest.mark.asyncio
async def test_unknown_thread_cannot_access_another_workflow() -> None:
    graph, registry, _ = make_graph([quote(), news(), finish()])
    await run_until_quote_pause(graph, "thread-a")

    with pytest.raises(EmptyInputError):
        await graph.ainvoke(None, thread_config("thread-unknown"))

    assert registry.executions == ["get_stock_quote"]
    paused = await graph.aget_state(thread_config("thread-a"))
    assert paused.values["research"].news is None


@pytest.mark.asyncio
async def test_limits_persist_across_resume() -> None:
    graph, _, _ = make_graph([quote(), news(), finish()])
    config = thread_config("t-limit")
    await run_until_quote_pause(graph, "t-limit", maximum_tool_calls=1)

    final = await graph.ainvoke(None, config)

    assert final["research"].error.code == "TOOL_CALL_LIMIT_REACHED"
    assert final["research"].tool_call_count == 1



