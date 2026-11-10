"""LangGraph-backed research service with checkpointing and human approval.

Checkpoints live in memory only: they are lost when the process restarts, and
there is no authentication, so this is a learning implementation and not a
production approval system.
"""

import asyncio
import logging
import re
import time
import uuid
from dataclasses import dataclass
from typing import Any

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import Command

from app.errors.approval_error import (
    approval_already_used,
    unknown_approval,
    unknown_thread,
)
from app.errors.decision_provider_error import DecisionProviderError
from app.langgraph_agent.research_graph import (
    create_research_graph,
    initial_graph_input,
    initial_portfolio_graph_input,
    recursion_limit_for,
    thread_config,
)
from app.models.research_report import ResearchReport
from app.models.tool_approval import ToolApprovalDecision, ToolApprovalRequest
from app.models.tool_execution_context import ToolExecutionContext
from app.request_context import get_request_id
from app.services.decision_model import DecisionModel
from app.services.disabled_decision_model import DisabledDecisionModel
from app.tools.tool_registry import ToolRegistry

logger = logging.getLogger(__name__)

# Opaque thread IDs generated here always match this shape.
THREAD_ID_PATTERN = re.compile(r"^[A-Za-z0-9._-]{1,64}$")


@dataclass(frozen=True)
class ApprovalRequiredResult:
    thread_id: str
    approval: ToolApprovalRequest


class GraphAgentResearchService:
    def __init__(
        self,
        *,
        registry: ToolRegistry,
        decision_model: DecisionModel,
        checkpointer: Any = None,
    ) -> None:
        self._decision_model = decision_model
        self._checkpointer = checkpointer if checkpointer is not None else InMemorySaver()
        self._graph = create_research_graph(
            registry=registry,
            decision_model=decision_model,
            checkpointer=self._checkpointer,
        )
        # Serializes approvals so one approval cannot be consumed twice by
        # concurrent requests.
        self._lock = asyncio.Lock()

    async def research_stock(
        self,
        symbol: str,
        *,
        maximum_news_items: int = 3,
        maximum_tool_calls: int = 5,
        maximum_decisions: int = 8,
    ) -> ResearchReport | ApprovalRequiredResult:
        started = time.perf_counter()
        thread_id = f"thr-{uuid.uuid4().hex}"
        config = self._config(thread_id, maximum_decisions)

        await self._graph.ainvoke(
            initial_graph_input(
                symbol=symbol,
                maximum_tool_calls=maximum_tool_calls,
                maximum_decisions=maximum_decisions,
                maximum_news_items=maximum_news_items,
            ),
            config,
        )
        return await self._outcome(thread_id, started)

    async def research_portfolio(
        self,
        execution_context: ToolExecutionContext,
        *,
        maximum_news_items: int = 3,
        maximum_tool_calls: int = 5,
        maximum_decisions: int = 8,
    ) -> ResearchReport | ApprovalRequiredResult:
        """Read-only portfolio research. The trusted context is issued by the
        application after JWT authentication and consent verification."""
        started = time.perf_counter()
        thread_id = f"thr-{uuid.uuid4().hex}"
        config = self._config(thread_id, maximum_decisions)

        await self._graph.ainvoke(
            initial_portfolio_graph_input(
                execution_context=execution_context,
                maximum_tool_calls=maximum_tool_calls,
                maximum_decisions=maximum_decisions,
                maximum_news_items=maximum_news_items,
            ),
            config,
        )
        return await self._outcome(thread_id, started)

    async def resume_with_approval(
        self,
        thread_id: str,
        decision: ToolApprovalDecision,
    ) -> ResearchReport | ApprovalRequiredResult:
        started = time.perf_counter()
        self._ensure_enabled()

        if not THREAD_ID_PATTERN.match(thread_id):
            raise unknown_thread()

        async with self._lock:
            config = thread_config(thread_id)
            snapshot = await self._graph.aget_state(config)
            values = snapshot.values

            if not values:
                raise unknown_thread()

            pending = values.get("pending_approval")
            consumed = values.get("consumed_approval_ids", [])

            if pending is None or not snapshot.next:
                if decision.approval_id in consumed:
                    raise approval_already_used()
                raise unknown_approval()

            if pending.request.approval_id != decision.approval_id:
                if decision.approval_id in consumed:
                    raise approval_already_used()
                raise unknown_approval()

            research = values["research"]
            config = self._config(thread_id, research.maximum_decisions)
            await self._graph.ainvoke(
                Command(resume=decision.model_dump(mode="json")),
                config,
            )
            return await self._outcome(thread_id, started)

    def _ensure_enabled(self) -> None:
        if isinstance(self._decision_model, DisabledDecisionModel):
            raise DecisionProviderError(
                code="AGENT_LIVE_CALLS_DISABLED",
                safe_message="Agent research is currently disabled.",
                status_code=503,
            )

    @staticmethod
    def _config(thread_id: str, maximum_decisions: int) -> dict[str, Any]:
        config = thread_config(thread_id)
        config["recursion_limit"] = recursion_limit_for(maximum_decisions)
        return config

    async def _outcome(
        self,
        thread_id: str,
        started: float,
    ) -> ResearchReport | ApprovalRequiredResult:
        snapshot = await self._graph.aget_state(thread_config(thread_id))
        values = snapshot.values
        pending = values.get("pending_approval")
        research = values["research"]

        # Safe fields only: never prompts, tool output text, provider responses,
        # headers, keys, summaries, or model reasons.
        if snapshot.next and pending is not None:
            logger.info(
                "agent_research_paused request_id=%s symbol=%s tool=%s "
                "decision_count=%s tool_call_count=%s latency_ms=%.1f",
                get_request_id(),
                research.symbol,
                pending.request.tool_name,
                research.decision_count,
                research.tool_call_count,
                (time.perf_counter() - started) * 1000,
            )
            return ApprovalRequiredResult(thread_id=thread_id, approval=pending.request)

        report: ResearchReport = values["report"]
        logger.info(
            "agent_research_finished request_id=%s symbol=%s status=%s "
            "decision_count=%s tool_call_count=%s tools=%s error_code=%s "
            "latency_ms=%.1f",
            get_request_id(),
            research.symbol,
            report.status,
            research.decision_count,
            research.tool_call_count,
            ",".join(step.tool_name for step in research.steps),
            report.error_code,
            (time.perf_counter() - started) * 1000,
        )
        return report
