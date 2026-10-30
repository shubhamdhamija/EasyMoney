from typing import Any

from app.models.research_state import ResearchError, ResearchState, ResearchStep
from app.services.decision_model import DecisionModel
from app.tools.tool_registry import ToolRegistry


class ModelDrivenResearchPlanner:
    """Runs the decide -> act -> observe loop.

    The decision model only proposes actions. Execution authority stays here:
    decision budget, tool budget, duplicate protection, symbol pinning, and the
    registry allowlist are all enforced by the application.
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

            # Duplicate calls cost nothing: skip without touching the budget.
            if self._is_duplicate(state, tool_name):
                state.steps.append(ResearchStep(tool_name=tool_name, status="skipped"))
                continue

            if state.tool_call_count >= state.maximum_tool_calls:
                state.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
                state.error = ResearchError(
                    code="TOOL_CALL_LIMIT_REACHED",
                    message="The research workflow reached its tool-call limit.",
                )
                return state

            arguments = self._prepare_arguments(
                state=state,
                tool_name=tool_name,
                arguments=decision.arguments,
                maximum_news_items=maximum_news_items,
            )

            if arguments is None:
                state.steps.append(ResearchStep(tool_name=tool_name, status="failed"))
                state.error = ResearchError(
                    code="INVALID_TOOL_ARGUMENTS",
                    message="The tool arguments are invalid.",
                )
                return state

            tool_response = await self._registry.execute(
                tool_name=tool_name,
                arguments=arguments,
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

            self._apply_tool_result(
                state=state,
                tool_name=tool_name,
                result=tool_response["result"],
            )
            state.steps.append(ResearchStep(tool_name=tool_name, status="completed"))

    @staticmethod
    def _is_duplicate(state: ResearchState, tool_name: str) -> bool:
        if tool_name == "get_stock_quote":
            return state.quote is not None
        if tool_name == "get_recent_news":
            return state.news is not None
        return False

    @staticmethod
    def _prepare_arguments(
        *,
        state: ResearchState,
        tool_name: str,
        arguments: dict[str, Any],
        maximum_news_items: int,
    ) -> dict[str, Any] | None:
        """Apply application-owned constraints to model-proposed arguments.

        Returns None when the arguments must be rejected outright. All other
        validation is left to the registry and the tool input models.
        """
        prepared = dict(arguments)

        symbol = prepared.get("symbol")
        if isinstance(symbol, str) and symbol.strip().upper() != state.symbol:
            return None

        if tool_name == "get_recent_news":
            requested = prepared.get("maximum_items")
            if requested is None:
                prepared["maximum_items"] = maximum_news_items
            elif isinstance(requested, int) and not isinstance(requested, bool):
                prepared["maximum_items"] = min(requested, maximum_news_items)

        return prepared

    @staticmethod
    def _apply_tool_result(
        *,
        state: ResearchState,
        tool_name: str,
        result: dict[str, Any],
    ) -> None:
        if tool_name == "get_stock_quote":
            state.quote = result
            return

        if tool_name == "get_recent_news":
            state.news = result
            return

        state.error = ResearchError(
            code="UNSUPPORTED_TOOL_RESULT",
            message="The planner could not store the tool result.",
        )

