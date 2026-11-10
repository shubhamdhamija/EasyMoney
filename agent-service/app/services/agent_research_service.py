import logging
import time

from app.models.research_report import ResearchReport
from app.models.tool_execution_context import ToolExecutionContext
from app.request_context import get_request_id
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.services.research_report_builder import build_research_report

logger = logging.getLogger(__name__)


class AgentResearchService:
    def __init__(self, planner: ModelDrivenResearchPlanner) -> None:
        self._planner = planner

    async def research_portfolio(
        self,
        execution_context: ToolExecutionContext,
        *,
        maximum_news_items: int = 3,
        maximum_tool_calls: int = 5,
        maximum_decisions: int = 8,
    ) -> ResearchReport:
        started = time.perf_counter()
        state = await self._planner.research_portfolio(
            execution_context,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
            maximum_news_items=maximum_news_items,
        )
        report = build_research_report(state)

        # Safe fields only: no symbols, holdings, values, IDs, prompts or model reasons.
        logger.info(
            "agent_portfolio_research_finished request_id=%s status=%s decision_count=%s "
            "tool_call_count=%s tools=%s error_code=%s latency_ms=%.1f",
            get_request_id(),
            report.status,
            state.decision_count,
            state.tool_call_count,
            ",".join(step.tool_name for step in state.steps),
            report.error_code,
            (time.perf_counter() - started) * 1000,
        )
        return report

    async def research_stock(
        self,
        symbol: str,
        *,
        maximum_news_items: int = 3,
        maximum_tool_calls: int = 5,
        maximum_decisions: int = 8,
    ) -> ResearchReport:
        started = time.perf_counter()

        state = await self._planner.research_stock(
            symbol,
            maximum_tool_calls=maximum_tool_calls,
            maximum_decisions=maximum_decisions,
            maximum_news_items=maximum_news_items,
        )

        report = build_research_report(state)

        # Safe fields only: never prompts, tool output text, raw provider
        # responses, headers, keys, or model reasons.
        logger.info(
            "agent_research_finished request_id=%s symbol=%s status=%s "
            "decision_count=%s tool_call_count=%s tools=%s error_code=%s "
            "latency_ms=%.1f",
            get_request_id(),
            state.symbol,
            report.status,
            state.decision_count,
            state.tool_call_count,
            ",".join(step.tool_name for step in state.steps),
            report.error_code,
            (time.perf_counter() - started) * 1000,
        )

        return report
