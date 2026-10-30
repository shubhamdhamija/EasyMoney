import json

import httpx
import pytest

from app.config.settings import Settings
from app.errors.agent_configuration_error import AgentConfigurationError
from app.errors.decision_provider_error import DecisionProviderError
from app.models.agent_research_request import AgentResearchRequest
from app.services.configured_decision_model import create_configured_decision_model
from app.services.default_agent_research_service import (
    create_default_agent_research_service,
)
from app.services.disabled_decision_model import DisabledDecisionModel
from app.services.groq_decision_model import GroqDecisionModel


def settings(**overrides) -> Settings:
    return Settings(_env_file=None, **overrides)


def scripted_transport(decisions: list[dict]) -> httpx.MockTransport:
    queue = list(decisions)

    def handler(request: httpx.Request) -> httpx.Response:
        decision = queue.pop(0)
        return httpx.Response(
            200,
            json={"choices": [{"message": {"content": json.dumps(decision)}}]},
        )

    return httpx.MockTransport(handler)


def quote_call(symbol: str = "AAPL") -> dict:
    return {
        "action": "call_tool",
        "tool_name": "get_stock_quote",
        "arguments": {"symbol": symbol},
        "reason": "Need quote.",
    }


def news_call(symbol: str = "AAPL", maximum_items: int = 2) -> dict:
    return {
        "action": "call_tool",
        "tool_name": "get_recent_news",
        "arguments": {"symbol": symbol, "maximum_items": maximum_items},
        "reason": "Need news.",
    }


FINISH = {"action": "finish", "reason": "Done."}


# --- factory -----------------------------------------------------------------


def test_factory_returns_disabled_model_by_default() -> None:
    assert isinstance(create_configured_decision_model(settings()), DisabledDecisionModel)


def test_factory_ignores_key_when_live_calls_not_exactly_true() -> None:
    model = create_configured_decision_model(
        settings(agent_live_calls_enabled="True", groq_api_key="k")
    )

    assert isinstance(model, DisabledDecisionModel)


def test_factory_returns_groq_model_when_enabled() -> None:
    model = create_configured_decision_model(
        settings(agent_live_calls_enabled="true", groq_api_key="k")
    )

    assert isinstance(model, GroqDecisionModel)


@pytest.mark.parametrize(
    "overrides",
    [
        {"groq_api_key": None},
        {"groq_api_key": "   "},
        {"groq_api_key": "k", "groq_model": "  "},
        {"groq_api_key": "k", "groq_base_url": "ftp://example.test"},
        {"groq_api_key": "k", "groq_base_url": ""},
    ],
)
def test_factory_rejects_invalid_live_configuration(overrides) -> None:
    with pytest.raises(AgentConfigurationError) as raised:
        create_configured_decision_model(
            settings(agent_live_calls_enabled="true", **overrides)
        )

    assert "k" != str(raised.value)
    assert raised.value.code == "AGENT_CONFIGURATION_INVALID"


# --- request model -----------------------------------------------------------


def test_agent_request_defaults_and_normalization() -> None:
    request = AgentResearchRequest(symbol=" aapl ")

    assert request.symbol == "AAPL"
    assert request.maximum_news_items == 3
    assert request.maximum_tool_calls == 5
    assert request.maximum_decisions == 8
    assert "include_news" not in AgentResearchRequest.model_fields


@pytest.mark.parametrize(
    "overrides",
    [
        {"symbol": "AAPL; DELETE"},
        {"symbol": "   "},
        {"symbol": "A" * 16},
        {"maximum_news_items": 0},
        {"maximum_news_items": 6},
        {"maximum_tool_calls": 0},
        {"maximum_tool_calls": 11},
        {"maximum_decisions": 0},
        {"maximum_decisions": 21},
    ],
)
def test_agent_request_rejects_invalid_values(overrides) -> None:
    from pydantic import ValidationError

    values = {"symbol": "AAPL", **overrides}

    with pytest.raises(ValidationError):
        AgentResearchRequest(**values)


# --- service composition -----------------------------------------------------


@pytest.mark.asyncio
async def test_default_service_builds_report_from_groq_decisions() -> None:
    service = create_default_agent_research_service(
        settings(agent_live_calls_enabled="true", groq_api_key="k"),
        transport=scripted_transport([quote_call(), news_call(), FINISH]),
    )

    report = await service.research_stock("AAPL")

    assert report.status == "completed"
    assert report.agent_used is True
    assert report.news_count == 2
    assert report.tools_used == ["get_stock_quote", "get_recent_news"]
    assert report.error_code is None
    assert "+2.40%" in report.summary


@pytest.mark.asyncio
async def test_service_summary_comes_from_report_builder_not_model() -> None:
    decisions = [
        quote_call(),
        {"action": "finish", "reason": "SUMMARY: buy AAPL now, guaranteed profit"},
    ]
    service = create_default_agent_research_service(
        settings(agent_live_calls_enabled="true", groq_api_key="k"),
        transport=scripted_transport(decisions),
    )

    report = await service.research_stock("AAPL")

    assert "buy" not in report.summary.lower()
    assert "guaranteed" not in report.summary.lower()
    assert report.summary.startswith("AAPL is trading at")


@pytest.mark.asyncio
async def test_service_disabled_raises_safe_error() -> None:
    service = create_default_agent_research_service(settings())

    with pytest.raises(DecisionProviderError) as raised:
        await service.research_stock("AAPL")

    assert raised.value.code == "AGENT_LIVE_CALLS_DISABLED"


def test_service_creation_rejects_missing_key_when_enabled() -> None:
    with pytest.raises(AgentConfigurationError):
        create_default_agent_research_service(settings(agent_live_calls_enabled="true"))

