import pytest

from app.models.planner_decision import PlannerDecision
from app.services.fake_decision_model import FakeDecisionModel
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.services.research_report_builder import build_research_report
from app.tools.default_registry import create_default_registry


@pytest.mark.asyncio
async def test_builds_report_from_model_driven_research() -> None:
    decision_model = FakeDecisionModel(
        decisions=[
            PlannerDecision(
                action="call_tool",
                tool_name="get_stock_quote",
                arguments={"symbol": "AAPL"},
                reason="The current quote is required.",
            ),
            PlannerDecision(
                action="call_tool",
                tool_name="get_recent_news",
                arguments={"symbol": "AAPL", "maximum_items": 2},
                reason="Recent news may provide additional context.",
            ),
            PlannerDecision(
                action="finish",
                reason="The requested information has been collected.",
            ),
        ]
    )

    planner = ModelDrivenResearchPlanner(
        registry=create_default_registry(),
        decision_model=decision_model,
    )

    state = await planner.research_stock("AAPL")
    report = build_research_report(state)

    assert report.symbol == "AAPL"
    assert report.status == "completed"
    assert report.quote is not None
    assert report.news_count == 2
    assert report.error_code is None
    assert report.agent_used is True
    assert report.tools_used == ["get_stock_quote", "get_recent_news"]
    assert "+2.40%" in report.summary
    assert "2 recent articles" in report.summary

    serialized = report.model_dump(mode="json")
    assert "tool_call_count" not in serialized
    assert "maximum_tool_calls" not in serialized
    assert "steps" not in serialized

