import json
import logging

import httpx
import pytest

from app.errors.decision_provider_error import DecisionProviderError
from app.models.research_state import ResearchState
from app.services.groq_decision_model import GroqDecisionModel
from app.tools.default_registry import create_default_registry

API_KEY = "test-secret-key-123"


def decision_payload(**overrides) -> dict:
    payload = {
        "action": "call_tool",
        "tool_name": "get_stock_quote",
        "arguments": {"symbol": "AAPL"},
        "reason": "Need the quote.",
    }
    payload.update(overrides)
    return payload


def completion(content) -> dict:
    return {"choices": [{"message": {"role": "assistant", "content": content}}]}


def make_model(handler, **kwargs) -> GroqDecisionModel:
    return GroqDecisionModel(
        api_key=API_KEY,
        transport=httpx.MockTransport(handler),
        **kwargs,
    )


async def run(model: GroqDecisionModel):
    return await model.decide(
        ResearchState(symbol="AAPL"),
        create_default_registry().tool_definitions(),
    )


def json_handler(content, status: int = 200):
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status, json=completion(content))

    return handler


@pytest.mark.asyncio
async def test_returns_typed_decision() -> None:
    model = make_model(json_handler(json.dumps(decision_payload())))

    decision = await run(model)

    assert decision.action == "call_tool"
    assert decision.tool_name == "get_stock_quote"
    assert decision.arguments == {"symbol": "AAPL"}


@pytest.mark.asyncio
async def test_returns_finish_decision() -> None:
    content = json.dumps({"action": "finish", "reason": "Done."})

    decision = await run(make_model(json_handler(content)))

    assert decision.action == "finish"


@pytest.mark.asyncio
@pytest.mark.parametrize("fence", ["```json\n{body}\n```", "```\n{body}\n```", "  ```JSON\n{body}\n```  "])
async def test_accepts_markdown_fenced_json(fence: str) -> None:
    content = fence.format(body=json.dumps(decision_payload()))

    decision = await run(make_model(json_handler(content)))

    assert decision.tool_name == "get_stock_quote"


@pytest.mark.asyncio
async def test_request_shape() -> None:
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["method"] = request.method
        captured["auth"] = request.headers.get("authorization")
        captured["body"] = json.loads(request.content)
        return httpx.Response(200, json=completion(json.dumps(decision_payload())))

    model = make_model(
        handler,
        base_url="https://example.test/openai/v1/",
        model="openai/gpt-oss-20b",
    )

    await run(model)

    body = captured["body"]
    assert captured["method"] == "POST"
    assert captured["url"] == "https://example.test/openai/v1/chat/completions"
    assert captured["auth"] == f"Bearer {API_KEY}"
    assert body["model"] == "openai/gpt-oss-20b"
    assert body["response_format"] == {"type": "json_object"}
    assert body["temperature"] == 0.1
    assert body["max_completion_tokens"] > 0
    assert body["reasoning_effort"] == "low"
    assert [m["role"] for m in body["messages"]] == ["system", "user"]
    assert API_KEY not in json.dumps(body)


@pytest.mark.asyncio
async def test_reasoning_effort_only_for_gpt_oss_models() -> None:
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["body"] = json.loads(request.content)
        return httpx.Response(200, json=completion(json.dumps(decision_payload())))

    await run(make_model(handler, model="llama-3.3-70b-versatile"))

    assert "reasoning_effort" not in captured["body"]


@pytest.mark.asyncio
async def test_uses_injected_client() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=completion(json.dumps(decision_payload())))

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        model = GroqDecisionModel(api_key=API_KEY, http_client=client)
        decision = await run(model)

    assert decision.action == "call_tool"


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("status", "code", "http_status"),
    [
        (401, "DECISION_PROVIDER_AUTH_FAILED", 503),
        (403, "DECISION_PROVIDER_AUTH_FAILED", 503),
        (429, "DECISION_PROVIDER_RATE_LIMITED", 503),
        (500, "DECISION_PROVIDER_UNAVAILABLE", 503),
        (503, "DECISION_PROVIDER_UNAVAILABLE", 503),
        (400, "DECISION_PROVIDER_INVALID_RESPONSE", 502),
    ],
)
async def test_http_status_mapping(status: int, code: str, http_status: int) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status, json={"error": {"message": "PROVIDER-BODY-LEAK"}})

    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(handler))

    assert raised.value.code == code
    assert raised.value.status_code == http_status
    assert raised.value.provider_status == status
    assert "PROVIDER-BODY-LEAK" not in raised.value.safe_message
    assert API_KEY not in raised.value.safe_message


@pytest.mark.asyncio
async def test_timeout_mapping() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("slow", request=request)

    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(handler))

    assert raised.value.code == "DECISION_PROVIDER_TIMEOUT"
    assert raised.value.status_code == 504


@pytest.mark.asyncio
async def test_network_error_mapping() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused", request=request)

    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(handler))

    assert raised.value.code == "DECISION_PROVIDER_NETWORK_ERROR"
    assert raised.value.status_code == 503


@pytest.mark.asyncio
@pytest.mark.parametrize("content", [None, "", "   \n"])
async def test_empty_content_mapping(content) -> None:
    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(json_handler(content)))

    assert raised.value.code == "DECISION_PROVIDER_EMPTY_RESPONSE"
    assert raised.value.status_code == 502


@pytest.mark.asyncio
async def test_unreadable_provider_body_mapping() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, text="<html>not json</html>")

    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(handler))

    assert raised.value.code == "DECISION_PROVIDER_INVALID_RESPONSE"
    assert raised.value.status_code == 502


@pytest.mark.asyncio
@pytest.mark.parametrize("body", [{}, {"choices": []}, {"choices": [{}]}, {"choices": [{"message": {}}]}])
async def test_missing_choices_mapping(body) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=body)

    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(handler))

    assert raised.value.code == "DECISION_PROVIDER_INVALID_RESPONSE"


@pytest.mark.asyncio
async def test_non_string_content_mapping() -> None:
    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(json_handler(["not", "a", "string"])))

    assert raised.value.code == "DECISION_PROVIDER_INVALID_RESPONSE"


@pytest.mark.asyncio
async def test_malformed_json_mapping() -> None:
    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(json_handler("{not valid json RAW-MODEL-OUTPUT")))

    assert raised.value.code == "DECISION_PROVIDER_INVALID_JSON"
    assert raised.value.status_code == 502
    assert "RAW-MODEL-OUTPUT" not in raised.value.safe_message


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "content",
    [
        json.dumps({"action": "delete_everything", "reason": "x"}),
        json.dumps({"action": "call_tool", "reason": "missing tool"}),
        json.dumps({"action": "finish", "tool_name": "get_stock_quote", "reason": "x"}),
        json.dumps({"action": "finish"}),
        json.dumps(["not", "an", "object"]),
        json.dumps("just a string"),
    ],
)
async def test_invalid_decision_output_mapping(content: str) -> None:
    with pytest.raises(DecisionProviderError) as raised:
        await run(make_model(json_handler(content)))

    assert raised.value.code == "DECISION_OUTPUT_INVALID"
    assert raised.value.status_code == 502


@pytest.mark.asyncio
async def test_no_sensitive_data_is_logged(caplog) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(429, json={"error": "PROVIDER-BODY-LEAK"})

    with caplog.at_level(logging.DEBUG):
        with pytest.raises(DecisionProviderError):
            await run(make_model(handler))

        for content in ("RAW-MODEL-OUTPUT-LEAK", "{broken"):
            with pytest.raises(DecisionProviderError):
                await run(make_model(json_handler(content)))

    logged = caplog.text
    assert API_KEY not in logged
    assert "Bearer" not in logged
    assert "PROVIDER-BODY-LEAK" not in logged
    assert "RAW-MODEL-OUTPUT-LEAK" not in logged
    assert "research-agent-v2" not in logged
    assert "Rules:" not in logged


def test_repr_does_not_expose_key() -> None:
    model = GroqDecisionModel(api_key=API_KEY)

    assert API_KEY not in repr(model)
