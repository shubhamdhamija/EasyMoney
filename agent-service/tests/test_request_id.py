import logging
import uuid

import httpx
import pytest
from fastapi.testclient import TestClient

from app.api.agent_research_routes import get_agent_research_service
from app.config.settings import Settings
from app.main import app
from app.middleware.request_id import resolve_request_id
from app.services.default_agent_research_service import (
    create_default_agent_research_service,
)

client = TestClient(app)


@pytest.fixture(autouse=True)
def reset_overrides():
    yield
    app.dependency_overrides.clear()


def disabled_service():
    return create_default_agent_research_service(Settings(_env_file=None))


def test_generates_uuid_request_id_when_none_supplied() -> None:
    response = client.get("/health")

    request_id = response.headers["x-request-id"]
    assert str(uuid.UUID(request_id)) == request_id


def test_each_request_gets_a_distinct_generated_id() -> None:
    first = client.get("/health").headers["x-request-id"]
    second = client.get("/health").headers["x-request-id"]

    assert first != second


def test_echoes_safe_client_request_id() -> None:
    response = client.get("/health", headers={"x-request-id": "agent-live-001"})

    assert response.headers["x-request-id"] == "agent-live-001"


@pytest.mark.parametrize(
    "candidate",
    ["", "has space", "a" * 65, "bad\tchar", "semi;colon", "új-id", "<script>"],
)
def test_replaces_unsafe_client_request_ids(candidate: str) -> None:
    resolved = resolve_request_id(candidate)

    assert resolved != candidate
    uuid.UUID(resolved)


def test_disabled_error_body_and_header_share_the_same_request_id() -> None:
    app.dependency_overrides[get_agent_research_service] = disabled_service

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    body = response.json()
    assert response.status_code == 503
    assert body["error"] == {
        "code": "AGENT_LIVE_CALLS_DISABLED",
        "message": "Agent research is currently disabled.",
    }
    assert body["request_id"] == response.headers["x-request-id"]
    uuid.UUID(body["request_id"])


def test_client_request_id_is_echoed_in_error_body() -> None:
    app.dependency_overrides[get_agent_research_service] = disabled_service

    response = client.post(
        "/agent/research",
        json={"symbol": "AAPL"},
        headers={"x-request-id": "agent-live-001"},
    )

    assert response.json()["request_id"] == "agent-live-001"
    assert response.headers["x-request-id"] == "agent-live-001"


def test_provider_errors_carry_request_id() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(429, json={"error": "PROVIDER-BODY-LEAK"})

    settings = Settings(_env_file=None, agent_live_calls_enabled="true", groq_api_key="k")
    app.dependency_overrides[get_agent_research_service] = (
        lambda: create_default_agent_research_service(
            settings, transport=httpx.MockTransport(handler)
        )
    )

    response = client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.json()["request_id"] == response.headers["x-request-id"]
    assert "PROVIDER-BODY-LEAK" not in response.text


def test_successful_response_has_header_but_unchanged_body_shape() -> None:
    response = client.post("/research", json={"symbol": "AAPL", "include_news": False})

    assert response.status_code == 200
    assert "x-request-id" in response.headers
    assert "request_id" not in response.json()


def test_validation_errors_still_carry_header() -> None:
    response = client.post("/agent/research", json={"symbol": "AAPL; DELETE"})

    assert response.status_code == 422
    assert "x-request-id" in response.headers


def test_unexpected_errors_return_safe_500_with_request_id() -> None:
    class ExplodingService:
        async def research_stock(self, *args, **kwargs):
            raise RuntimeError("SECRET-INTERNAL-DETAIL")

    app.dependency_overrides[get_agent_research_service] = lambda: ExplodingService()

    safe_client = TestClient(app, raise_server_exceptions=False)
    response = safe_client.post("/agent/research", json={"symbol": "AAPL"})

    assert response.status_code == 500
    assert response.json()["error"]["code"] == "INTERNAL_ERROR"
    assert response.json()["request_id"] == response.headers["x-request-id"]
    assert "SECRET-INTERNAL-DETAIL" not in response.text


def test_agent_run_logs_safe_fields_only(caplog) -> None:
    import json

    queue = [
        {
            "action": "call_tool",
            "tool_name": "get_stock_quote",
            "arguments": {"symbol": "AAPL"},
            "reason": "HIDDEN-REASON-TEXT",
        },
        {
            "action": "call_tool",
            "tool_name": "get_recent_news",
            "arguments": {"symbol": "AAPL", "maximum_items": 1},
            "reason": "HIDDEN-REASON-TEXT",
        },
        {"action": "finish", "reason": "HIDDEN-REASON-TEXT"},
    ]

    def handler(request: httpx.Request) -> httpx.Response:
        content = json.dumps(queue.pop(0))
        return httpx.Response(200, json={"choices": [{"message": {"content": content}}]})

    settings = Settings(
        _env_file=None, agent_live_calls_enabled="true", groq_api_key="secret-key-xyz"
    )
    app.dependency_overrides[get_agent_research_service] = (
        lambda: create_default_agent_research_service(
            settings, transport=httpx.MockTransport(handler)
        )
    )

    with caplog.at_level(logging.INFO):
        response = client.post(
            "/agent/research",
            json={"symbol": "AAPL", "maximum_news_items": 1},
            headers={"x-request-id": "agent-live-001"},
        )

    assert response.status_code == 200
    logged = caplog.text
    assert "request_id=agent-live-001" in logged
    assert "symbol=AAPL" in logged
    assert "decision_count=3" in logged
    assert "tool_call_count=2" in logged
    assert "get_stock_quote,get_recent_news" in logged
    assert "secret-key-xyz" not in logged
    assert "HIDDEN-REASON-TEXT" not in logged
    assert "Quarterly results" not in logged
    assert "Rules:" not in logged

