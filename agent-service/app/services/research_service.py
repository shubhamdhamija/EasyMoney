from app.models.research_report import ResearchReport
from app.services.research_planner import DeterministicResearchPlanner
from app.services.research_report_builder import build_research_report


class ResearchService:
    def __init__(
        self,
        planner: DeterministicResearchPlanner,
    ) -> None:
        self._planner = planner

    async def research_stock(
        self,
        symbol: str,
        *,
        include_news: bool | None = None,
        maximum_news_items: int = 3,
        maximum_tool_calls: int = 5,
    ) -> ResearchReport:
        state = await self._planner.research_stock(
            symbol,
            include_news=include_news,
            maximum_news_items=maximum_news_items,
            maximum_tool_calls=maximum_tool_calls,
        )

        return build_research_report(state)
