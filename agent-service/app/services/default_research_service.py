from app.services.research_planner import DeterministicResearchPlanner
from app.services.research_service import ResearchService
from app.tools.default_registry import create_default_registry


def create_default_research_service() -> ResearchService:
    registry = create_default_registry()

    planner = DeterministicResearchPlanner(
        registry=registry,
    )

    return ResearchService(
        planner=planner,
    )

