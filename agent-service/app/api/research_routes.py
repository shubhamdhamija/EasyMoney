from fastapi import APIRouter, Depends

from app.models.research_report import ResearchReport
from app.models.research_request import ResearchRequest
from app.services.default_research_service import create_default_research_service
from app.services.research_service import ResearchService


router = APIRouter(
    prefix="/research",
    tags=["research"],
)


def get_research_service() -> ResearchService:
    return create_default_research_service()


@router.post(
    "",
    response_model=ResearchReport,
)
async def research_stock(
    request: ResearchRequest,
    service: ResearchService = Depends(get_research_service),
) -> ResearchReport:
    return await service.research_stock(
        request.symbol,
        include_news=request.include_news,
        maximum_news_items=request.maximum_news_items,
        maximum_tool_calls=request.maximum_tool_calls,
    )

