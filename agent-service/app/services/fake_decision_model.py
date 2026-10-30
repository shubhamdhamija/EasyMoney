from app.models.planner_decision import PlannerDecision
from app.models.research_state import ResearchState
from app.models.tool_definition import ToolDefinition


class FakeDecisionModel:
    def __init__(self, decisions: list[PlannerDecision]) -> None:
        self._decisions = list(decisions)
        self.calls: list[ResearchState] = []
        self.available_tools_seen: list[list[ToolDefinition]] = []

    async def decide(
        self,
        state: ResearchState,
        available_tools: list[ToolDefinition] | None = None,
    ) -> PlannerDecision:
        self.calls.append(state.model_copy(deep=True))
        self.available_tools_seen.append(
            [tool.model_copy(deep=True) for tool in (available_tools or [])]
        )

        if not self._decisions:
            return PlannerDecision(
                action="finish",
                reason="No additional decisions are configured.",
            )

        return self._decisions.pop(0)




