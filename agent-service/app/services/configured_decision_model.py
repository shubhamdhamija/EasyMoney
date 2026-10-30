import httpx

from app.config.settings import Settings
from app.errors.agent_configuration_error import AgentConfigurationError
from app.services.decision_model import DecisionModel
from app.services.disabled_decision_model import DisabledDecisionModel
from app.services.groq_decision_model import GroqDecisionModel


def create_configured_decision_model(
    settings: Settings,
    *,
    transport: httpx.AsyncBaseTransport | None = None,
) -> DecisionModel:
    if not settings.live_calls_enabled:
        return DisabledDecisionModel()

    api_key = (
        settings.groq_api_key.get_secret_value().strip()
        if settings.groq_api_key is not None
        else ""
    )

    if not api_key:
        raise AgentConfigurationError("Agent provider credentials are not configured.")

    if not settings.groq_model.strip():
        raise AgentConfigurationError("Agent provider model is not configured.")

    base_url = settings.groq_base_url.strip()
    if not base_url.startswith(("https://", "http://")):
        raise AgentConfigurationError("Agent provider base URL is invalid.")

    return GroqDecisionModel(
        api_key=api_key,
        base_url=base_url,
        model=settings.groq_model.strip(),
        timeout_seconds=settings.groq_timeout_seconds,
        transport=transport,
    )

