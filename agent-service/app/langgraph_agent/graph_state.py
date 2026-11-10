from typing import TypedDict

from app.models.planner_decision import PlannerDecision
from app.models.research_report import ResearchReport
from app.models.research_state import ResearchState
from app.models.tool_approval import PendingApproval
from app.models.tool_execution_context import ToolExecutionContext


class GraphState(TypedDict, total=False):
    """State passed between LangGraph nodes and saved in checkpoints.

    Only safe workflow data lives here: the research state, the current
    decision, the final report, limits, and approval bookkeeping. Keys, headers,
    prompts, raw provider responses, and reasoning are never stored.
    """

    research: ResearchState
    decision: PlannerDecision | None
    report: ResearchReport | None
    maximum_news_items: int
    # Prepared write awaiting a human decision; None when nothing is pending.
    pending_approval: PendingApproval | None
    # Approval IDs that were already decided. Makes approvals single-use.
    consumed_approval_ids: list[str]
    # Application-issued trusted identity for portfolio tools (never model-controlled).
    # Holds tenant/user IDs and opaque portfolio/consent IDs only: no JWT, no secrets.
    execution_context: ToolExecutionContext | None
