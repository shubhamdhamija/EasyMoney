import httpx
from langgraph.checkpoint.memory import InMemorySaver

from app.config.settings import Settings, get_settings
from app.services.configured_decision_model import create_configured_decision_model
from app.services.graph_agent_research_service import GraphAgentResearchService
from app.tools.default_registry import create_configured_registry

# One in-memory checkpointer per process so a workflow can be resumed by a later
# HTTP request. State is lost on restart.
_PROCESS_CHECKPOINTER = InMemorySaver()


def create_default_graph_agent_service(
    settings: Settings | None = None,
    *,
    transport: httpx.AsyncBaseTransport | None = None,
) -> GraphAgentResearchService:
    """Read-only registry: the default service has no write tools registered."""
    resolved_settings = settings if settings is not None else get_settings()

    return GraphAgentResearchService(
        registry=create_configured_registry(resolved_settings),
        decision_model=create_configured_decision_model(
            resolved_settings,
            transport=transport,
        ),
        checkpointer=_PROCESS_CHECKPOINTER,
    )

