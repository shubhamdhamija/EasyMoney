import json

import httpx
import pytest
from fastapi.testclient import TestClient

from app.api.agent_research_routes import get_agent_research_service
from app.config.settings import Settings
from app.main import app
from app.services.default_agent_research_service import (
    create_default_agent_research_service,
)

API_KEY = "test-secret-key-123"


def live_settings() -> Settings:
    return Settings(_env_file=None, agent_live_calls_enabled="true", groq_api_key=API_KEY)


def completion(content: str) -> dict:
    return {"choices": [{"message": {"content": content}}]}


def scripted(decisions: list[dict]) -> httpx.MockTransport:
    queue = list(decisions)

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=completion(json.dumps(queue.pop(0))))

    return httpx.MockTransport(handler)


def override_with(transport: httpx.MockTransport, settings: Settings | None = None):
    resolved = settings or live_settings()
    app.dependency_overrides[get_agent_research_service] = (
        lambda: create_default_agent_research_service(resolved, transport=transport)
    )


def quote_call() -> dict:
    return {
        "action": "call_tool",
        "tool_name": "get_stock_quote",
        "arguments": {"symbol": "AAPL"},
        "reason": "Need quote.",
    }


def news_call(maximum_items: int = 2) -> dict:
    return {
        "action": "call_tool",
        "tool_name": "get_recent_news",
        "arguments": {"symbol": "AAPL", "maximum_items": maximum_items},
        "reason": "Need news.",
    }


FINISH = {"action": "finish", "reason": "Done."}


@pytest.fixture(autouse=True)
def reset_overrides():
    yield
    app.dependency_overrides.clear()


client = TestClient(app)


def test_agent_endpoint_returns_deterministic_report() -> None:
    override_with(scripted([quote_call(), news_call(), FINISH]))

    response = client.post(
        "/agent/research",
        json={"symbol": " aapl ", "maximum_news_items": 2},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["symbol"] == "AAPL"
    assert body["status"] == "completed"
    assert body["agent_used"] is True
    assert body["news_count"] == 2
    assert body["tools_used"] == ["get_stock_quote", "get_recent_news"]
    assert body["error_code"] is None
    assert body["summary"].startswith("AAPL is trading at")
    assert set(body) == {
        "symbol",
        "status",
        "summary",
        "quote",
        "news_count",
        "tools_used",
        "error_code",
        "agent_used",
        "headlines",
    }
    assert [item["source"] for item in body["headlines"]]
    assert all(item["headline"] for item in body["headlines"])


def test_agent_endpoint_caps_news_by_request_limit() -> None:
    override_with(scripted([quote_call(), news_call(maximum_items=5), FINISH]))

    response = client.post(
        "/agent/research",
        json={"symbol": "AAPL", "maximum_news_items": 1},
    )

    assert response.status_code == 200
    assert response.json()["news_count"] == 1


def test_agent_endpoint_rejects_unknown_tool_from_model() -> None:
    hostile = {
        "action": "call_tool",
        "tool_name": "delete_database",
        "arguments": {},
        "reason": "Delete stored data.",
    }
    override_with(scripted([hostile]))

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "failed"
    assert body["error_code"] == "UNKNOWN_TOOL"
    assert body["quote"] is None
    assert body["tools_used"] == []


def test_agent_endpoint_honors_tool_limit() -> None:
    override_with(scripted([quote_call(), news_call(), FINISH]))

    response = client.post(
        "/agent/research",
        json={"symbol": "AAPL", "maximum_tool_calls": 1},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["status"] == "partial"
    assert body["error_code"] == "TOOL_CALL_LIMIT_REACHED"
    assert body["tools_used"] == ["get_stock_quote"]


def test_agent_endpoint_honors_decision_limit() -> None:
    override_with(scripted([quote_call(), quote_call(), quote_call(), FINISH]))

    response = client.post(
        "/agent/research",
        json={"symbol": "AAPL", "maximum_decisions": 3},
    )

    body = response.json()
    assert response.status_code == 200
    assert body["status"] == "partial"
    assert body["error_code"] == "DECISION_LIMIT_REACHED"


def test_agent_endpoint_validates_request_before_service_runs() -> None:
    calls = []

    class RecordingService:
        async def research_stock(self, *args, **kwargs):
            calls.append((args, kwargs))
            raise AssertionError("service must not run for invalid requests")

    app.dependency_overrides[get_agent_research_service] = lambda: RecordingService()

    for body in (
        {"symbol": "AAPL; DELETE"},
        {"symbol": "AAPL", "maximum_decisions": 21},
        {"symbol": "AAPL", "maximum_tool_calls": 0},
        {"symbol": "AAPL", "maximum_news_items": 6},
    ):
        assert client.post("/agent/research", json=body).status_code == 422

    assert calls == []


def test_agent_endpoint_disabled_returns_safe_503() -> None:
    app.dependency_overrides[get_agent_research_service] = (
        lambda: create_default_agent_research_service(Settings(_env_file=None))
    )

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == 503
    body = response.json()
    request_id = body.pop("request_id")
    assert body == {
        "error": {
            "code": "AGENT_LIVE_CALLS_DISABLED",
            "message": "Agent research is currently disabled.",
        }
    }
    assert request_id
    assert response.headers["x-request-id"] == request_id


def test_agent_endpoint_invalid_configuration_returns_safe_503() -> None:
    app.dependency_overrides[get_agent_research_service] = (
        lambda: create_default_agent_research_service(
            Settings(_env_file=None, agent_live_calls_enabled="true")
        )
    )

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == 503
    assert response.json()["error"]["code"] == "AGENT_CONFIGURATION_INVALID"


@pytest.mark.parametrize(
    ("status", "code", "expected_http"),
    [
        (401, "DECISION_PROVIDER_AUTH_FAILED", 503),
        (429, "DECISION_PROVIDER_RATE_LIMITED", 503),
        (500, "DECISION_PROVIDER_UNAVAILABLE", 503),
    ],
)
def test_provider_http_errors_are_safe(status: int, code: str, expected_http: int) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status, json={"error": "PROVIDER-BODY-LEAK"})

    override_with(httpx.MockTransport(handler))

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == expected_http
    assert response.json()["error"]["code"] == code
    assert "PROVIDER-BODY-LEAK" not in response.text
    assert API_KEY not in response.text


def test_provider_timeout_returns_504() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("slow", request=request)

    override_with(httpx.MockTransport(handler))

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == 504
    assert response.json()["error"]["code"] == "DECISION_PROVIDER_TIMEOUT"


def test_malformed_model_output_returns_safe_502() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=completion("{RAW-MODEL-OUTPUT"))

    override_with(httpx.MockTransport(handler))

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == 502
    assert response.json()["error"]["code"] == "DECISION_PROVIDER_INVALID_JSON"
    assert "RAW-MODEL-OUTPUT" not in response.text


def test_default_dependency_is_disabled_without_configuration(monkeypatch) -> None:
    from app.config.settings import get_settings

    monkeypatch.delenv("AGENT_LIVE_CALLS_ENABLED", raising=False)
    monkeypatch.delenv("GROQ_API_KEY", raising=False)
    get_settings.cache_clear()
    monkeypatch.setitem(Settings.model_config, "env_file", None)

    try:
        response = client.post("/agent/research", json={"symbol": "AAPL"})
    finally:
        get_settings.cache_clear()

    assert response.status_code == 503
    assert response.json()["error"]["code"] == "AGENT_LIVE_CALLS_DISABLED"


def test_existing_research_endpoint_is_unchanged() -> None:
    response = client.post(
        "/research",
        json={"symbol": "AAPL", "include_news": True, "maximum_news_items": 2},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["agent_used"] is False
    assert body["news_count"] == 2


