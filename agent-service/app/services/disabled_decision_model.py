from app.errors.decision_provider_error import DecisionProviderError
from app.models.planner_decision import PlannerDecision
from app.models.research_state import ResearchState
from app.models.tool_definition import ToolDefinition


class DisabledDecisionModel:
    async def decide(
        self,
        state: ResearchState,
        available_tools: list[ToolDefinition] | None = None,
    ) -> PlannerDecision:
        raise DecisionProviderError(
            code="AGENT_LIVE_CALLS_DISABLED",
            safe_message="Agent research is currently disabled.",
            status_code=503,
        )

