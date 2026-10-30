from typing import TypedDict

from app.models.planner_decision import PlannerDecision
from app.models.research_report import ResearchReport
from app.models.research_state import ResearchState


class GraphState(TypedDict, total=False):
    """State passed between LangGraph nodes.

    Each node returns a partial update; keys without an update are kept.
    `research` is the same ResearchState used by the custom agent.
    """

    research: ResearchState
    decision: PlannerDecision | None
    report: ResearchReport | None

