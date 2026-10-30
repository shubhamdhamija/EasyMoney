import httpx

from app.config.settings import Settings, get_settings
from app.services.agent_research_service import AgentResearchService
from app.services.configured_decision_model import create_configured_decision_model
from app.services.model_driven_research_planner import ModelDrivenResearchPlanner
from app.tools.default_registry import create_default_registry


def create_default_agent_research_service(
    settings: Settings | None = None,
    *,
    transport: httpx.AsyncBaseTransport | None = None,
) -> AgentResearchService:
    resolved_settings = settings if settings is not None else get_settings()

    decision_model = create_configured_decision_model(
        resolved_settings,
        transport=transport,
    )

    planner = ModelDrivenResearchPlanner(
        registry=create_default_registry(),
        decision_model=decision_model,
    )

    return AgentResearchService(planner=planner)

