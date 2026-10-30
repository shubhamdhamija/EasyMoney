from app.models.research_state import ResearchError, ResearchState, ResearchStep
from app.services.decision_model import DecisionModel
from app.services.tool_execution_policy import apply_tool_result, evaluate_tool_request
from app.tools.tool_registry import ToolRegistry


class ModelDrivenResearchPlanner:
    """Runs the decide -> act -> observe loop.

    The decision model only proposes actions. Execution authority stays with the
    application: decision budget here, and duplicate protection, tool budget,
    symbol pinning and news cap in the shared tool_execution_policy, plus the
    registry allowlist.
    """

    def __init__(
        self,
        *,
        registry: ToolRegistry,
        decision_model: DecisionModel,
    ) -> None:
        self._registry = registry
        self._decision_model = decision_model

    async def research_stock(
        self,
        symbol: str,
        *,
        maximum_tool_calls: int = 5,
        maximum_decisions: int = 8,
        maximum_news_items: int = 3,
    ) -> ResearchState:
        state = ResearchState(
            symbol=symbol.strip().upper(),
            agent_used=True,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
        )
        available_tools = self._registry.tool_definitions()

        while True:
            if state.decision_count >= state.maximum_decisions:
                state.error = ResearchError(
                    code="DECISION_LIMIT_REACHED",
                    message="The research workflow reached its decision limit.",
                )
                return state

            decision = await self._decision_model.decide(state, available_tools)
            state.decision_count += 1

            if decision.action == "finish":
                if state.quote is None and state.error is None:
                    state.error = ResearchError(
                        code="QUOTE_NOT_COLLECTED",
                        message="The workflow finished without collecting a quote.",
                    )
                return state

            tool_name = decision.tool_name
            if tool_name is None:
                state.error = ResearchError(
                    code="INVALID_DECISION",
                    message="The decision did not identify a tool.",
                )
                return state

            policy = evaluate_tool_request(
                state=state,
                tool_name=tool_name,
                arguments=decision.arguments,
                maximum_news_items=maximum_news_items,
            )

            # Duplicate calls cost nothing: skipped without touching the budget.
            if policy.outcome == "skip_duplicate":
                state.steps.append(ResearchStep(tool_name=tool_name, status="skipped"))
                continue

            if policy.outcome == "limit_reached":
                state.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
                state.error = ResearchError(
                    code="TOOL_CALL_LIMIT_REACHED",
                    message="The research workflow reached its tool-call limit.",
                )
                return state

            if policy.outcome == "invalid_arguments":
                state.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
                state.error = ResearchError(
                    code="INVALID_TOOL_ARGUMENTS",
                    message="The tool arguments are invalid.",
                )
                return state

            tool_response = await self._registry.execute(
                tool_name=tool_name,
                arguments=policy.arguments,
            )
            state.tool_call_count += 1

            if not tool_response["ok"]:
                state.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
                error = tool_response["error"]
                state.error = ResearchError(
                    code=error["code"],
                    message=error["message"],
                )
                return state

            apply_tool_result(
                state=state,
                tool_name=tool_name,
                result=tool_response["result"],
            )
            state.steps.append(ResearchStep(tool_name=tool_name, status="completed"))

