"""Factory for the read-only portfolio research workflow (custom planner)."""

import httpx

from app.config.settings import Settings, get_settings
from app.services.agent_research_service import AgentResearchService
from app.services.configured_decision_model import create_configured_decision_model
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.services.portfolio_runtime import PortfolioResearchRuntime
from app.tools.default_registry import create_portfolio_research_registry


def create_default_portfolio_research_service(
    runtime: PortfolioResearchRuntime,
    settings: Settings | None = None,
    *,
    transport: httpx.AsyncBaseTransport | None = None,
) -> AgentResearchService:
    resolved = settings if settings is not None else get_settings()

    quote_tool = None
    news_tool = None
    if resolved.live_market_data_enabled:
        from app.tools.live_market_tools import LiveRecentNewsTool, LiveStockQuoteTool

        quote_tool = LiveStockQuoteTool()
        news_tool = LiveRecentNewsTool()

    registry = create_portfolio_research_registry(
        runtime=runtime,
        quote_tool=quote_tool,
        news_tool=news_tool,
    )
    planner = ModelDrivenResearchPlanner(
        registry=registry,
        decision_model=create_configured_decision_model(resolved, transport=transport),
    )
    return AgentResearchService(planner=planner)
