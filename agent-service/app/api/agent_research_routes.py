from fastapi import APIRouter, Depends

from app.models.agent_research_request import AgentResearchRequest
from app.models.research_report import ResearchReport
from app.services.agent_research_service import AgentResearchService
from app.services.default_agent_research_service import (
    create_default_agent_research_service,
)


router = APIRouter(
    prefix="/agent",
    tags=["agent"],
)


def get_agent_research_service() -> AgentResearchService:
    return create_default_agent_research_service()


@router.post(
    "/research",
    response_model=ResearchReport,
)
async def agent_research_stock(
    request: AgentResearchRequest,
    service: AgentResearchService = Depends(get_agent_research_service),
) -> ResearchReport:
    return await service.research_stock(
        request.symbol,
        maximum_news_items=request.maximum_news_items,
        maximum_tool_calls=request.maximum_tool_calls,
        maximum_decisions=request.maximum_decisions,
    )

