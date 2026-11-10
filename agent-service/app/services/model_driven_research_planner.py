from app.models.research_state import ResearchError, ResearchState, ResearchStep
from app.models.tool_execution_context import ToolExecutionContext
from app.services.decision_model import DecisionModel
from app.services.portfolio_research_setup import build_portfolio_research_state
from app.services.tool_execution_policy import (
    apply_tool_result,
    evaluate_tool_request,
    finish_error,
    policy_rejection_error,
)
from app.tools.tool_registry import ToolRegistry


class ModelDrivenResearchPlanner:
    """Runs the decide -> act -> observe loop.

    The decision model only proposes actions. Execution authority stays with the
    application: decision budget here, and duplicate protection, tool budget,
    symbol/portfolio pinning, consent and the news cap in the shared
    tool_execution_policy, plus the registry allowlist and trusted context.
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
        return await self._run(state, None, maximum_news_items)

    async def research_portfolio(
        self,
        context: ToolExecutionContext,
        *,
        maximum_tool_calls: int = 5,
        maximum_decisions: int = 8,
        maximum_news_items: int = 3,
    ) -> ResearchState:
        """Portfolio research. The trusted context is application-issued; the model
        cannot create or alter it."""
        state = build_portfolio_research_state(
            context,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
        )
        return await self._run(state, context, maximum_news_items)

    async def _run(
        self,
        state: ResearchState,
        context: ToolExecutionContext | None,
        maximum_news_items: int,
    ) -> ResearchState:
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
                state.error = finish_error(state) or state.error
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

            rejection = policy_rejection_error(policy.outcome)
            if rejection is not None:
                status = "rejected" if policy.outcome in ("consent_required", "portfolio_rejected") else "failed"
                state.steps.append(ResearchStep(tool_name=tool_name, status=status))
                state.error = rejection
                return state

            tool_response = await self._registry.execute(
                tool_name=tool_name,
                arguments=policy.arguments,
                context=context,
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
                arguments=policy.arguments,
            )
            state.steps.append(ResearchStep(tool_name=tool_name, status="completed"))
