from typing import Protocol

from app.models.planner_decision import PlannerDecision
from app.models.research_state import ResearchState
from app.models.tool_definition import ToolDefinition


class DecisionModel(Protocol):
    async def decide(
        self,
        state: ResearchState,
        available_tools: list[ToolDefinition],
    ) -> PlannerDecision: ...
