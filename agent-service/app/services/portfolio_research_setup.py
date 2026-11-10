"""Shared initial state for portfolio research (used by both orchestrators)."""

from app.models.research_state import ResearchState
from app.models.tool_execution_context import ToolExecutionContext

# Portfolio research has no single symbol; this fixed label keeps ResearchState valid.
PORTFOLIO_RESEARCH_LABEL = "PORTFOLIO"


def build_portfolio_research_state(
    context: ToolExecutionContext,
    *,
    maximum_tool_calls: int,
    maximum_decisions: int,
) -> ResearchState:
    """Only the application-issued context can enable portfolio tools. The consent ID
    is recorded only when the application has verified consent for this request."""
    return ResearchState(
        symbol=PORTFOLIO_RESEARCH_LABEL,
        agent_used=True,
        maximum_tool_calls=maximum_tool_calls,
        maximum_decisions=maximum_decisions,
        selected_portfolio_id=context.selected_portfolio_id,
        portfolio_consent_id=(
            context.portfolio_consent_id
            if context.authenticated and context.portfolio_consent_granted
            else None
        ),
    )
