import json
import logging
import re
from typing import Any

import httpx
from pydantic import ValidationError

from app.errors.decision_provider_error import DecisionProviderError
from app.models.planner_decision import PlannerDecision
from app.models.research_state import ResearchState
from app.models.tool_definition import ToolDefinition
from app.services.agent_prompt import build_system_instruction, build_user_message

logger = logging.getLogger(__name__)

_FENCE_PATTERN = re.compile(
    r"^\s*```(?:json)?\s*(.*?)\s*```\s*$",
    re.DOTALL | re.IGNORECASE,
)

MAX_COMPLETION_TOKENS = 1024


class GroqDecisionModel:
    """Asks Groq (OpenAI-compatible API) for the next PlannerDecision.

    The model only proposes; it never executes. Nothing sensitive (key,
    headers, prompts, raw responses) is logged or placed in raised errors.
    """

    def __init__(
        self,
        *,
        api_key: str,
        base_url: str = "https://api.groq.com/openai/v1",
        model: str = "openai/gpt-oss-20b",
        timeout_seconds: float = 30,
        http_client: httpx.AsyncClient | None = None,
        transport: httpx.AsyncBaseTransport | None = None,
    ) -> None:
        self._api_key = api_key
        self._base_url = base_url.rstrip("/")
        self._model = model
        self._timeout_seconds = timeout_seconds
        self._http_client = http_client
        self._transport = transport

    def __repr__(self) -> str:
        return f"GroqDecisionModel(model={self._model!r})"

    async def decide(
        self,
        state: ResearchState,
        available_tools: list[ToolDefinition] | None = None,
    ) -> PlannerDecision:
        body: dict[str, Any] = {
            "model": self._model,
            "messages": [
                {"role": "system", "content": build_system_instruction(state.portfolio_mode)},
                {
                    "role": "user",
                    "content": build_user_message(state, available_tools or []),
                },
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.1,
            "max_completion_tokens": MAX_COMPLETION_TOKENS,
        }

        if "gpt-oss" in self._model.lower():
            body["reasoning_effort"] = "low"

        response = await self._post(body)
        content = self._extract_content(response)
        return self._parse_decision(content)

    async def _post(self, body: dict[str, Any]) -> httpx.Response:
        url = f"{self._base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }

        try:
            if self._http_client is not None:
                response = await self._http_client.post(
                    url,
                    json=body,
                    headers=headers,
                    timeout=self._timeout_seconds,
                )
            else:
                async with httpx.AsyncClient(
                    transport=self._transport,
                    timeout=self._timeout_seconds,
                ) as client:
                    response = await client.post(url, json=body, headers=headers)
        except httpx.TimeoutException as error:
            logger.warning("Decision provider timed out.")
            raise DecisionProviderError(
                code="DECISION_PROVIDER_TIMEOUT",
                safe_message="The decision provider timed out.",
                status_code=504,
                cause=error,
            ) from None
        except httpx.RequestError as error:
            logger.warning("Decision provider network error.")
            raise DecisionProviderError(
                code="DECISION_PROVIDER_NETWORK_ERROR",
                safe_message="The decision provider could not be reached.",
                status_code=503,
                cause=error,
            ) from None

        self._raise_for_status(response)
        return response

    @staticmethod
    def _raise_for_status(response: httpx.Response) -> None:
        status = response.status_code

        if status < 400:
            return

        logger.warning("Decision provider returned HTTP %s.", status)

        if status in (401, 403):
            raise DecisionProviderError(
                code="DECISION_PROVIDER_AUTH_FAILED",
                safe_message="The decision provider rejected the credentials.",
                status_code=503,
                provider_status=status,
            )

        if status == 429:
            raise DecisionProviderError(
                code="DECISION_PROVIDER_RATE_LIMITED",
                safe_message="The decision provider is rate limited.",
                status_code=503,
                provider_status=status,
            )

        if status >= 500:
            raise DecisionProviderError(
                code="DECISION_PROVIDER_UNAVAILABLE",
                safe_message="The decision provider is unavailable.",
                status_code=503,
                provider_status=status,
            )

        raise DecisionProviderError(
            code="DECISION_PROVIDER_INVALID_RESPONSE",
            safe_message="The decision provider returned an unusable response.",
            status_code=502,
            provider_status=status,
        )

    @staticmethod
    def _extract_content(response: httpx.Response) -> str:
        try:
            payload = response.json()
            content = payload["choices"][0]["message"]["content"]
        except (ValueError, KeyError, IndexError, TypeError):
            raise DecisionProviderError(
                code="DECISION_PROVIDER_INVALID_RESPONSE",
                safe_message="The decision provider returned an unreadable response.",
                status_code=502,
            ) from None

        if content is None:
            raise DecisionProviderError(
                code="DECISION_PROVIDER_EMPTY_RESPONSE",
                safe_message="The decision provider returned an empty response.",
                status_code=502,
            )

        if not isinstance(content, str):
            raise DecisionProviderError(
                code="DECISION_PROVIDER_INVALID_RESPONSE",
                safe_message="The decision provider returned an unreadable response.",
                status_code=502,
            )

        if not content.strip():
            raise DecisionProviderError(
                code="DECISION_PROVIDER_EMPTY_RESPONSE",
                safe_message="The decision provider returned an empty response.",
                status_code=502,
            )

        return content

    @staticmethod
    def _parse_decision(content: str) -> PlannerDecision:
        text = content.strip()
        fenced = _FENCE_PATTERN.match(text)
        if fenced:
            text = fenced.group(1)

        try:
            parsed = json.loads(text)
        except ValueError:
            raise DecisionProviderError(
                code="DECISION_PROVIDER_INVALID_JSON",
                safe_message="The decision provider returned malformed JSON.",
                status_code=502,
            ) from None

        try:
            return PlannerDecision.model_validate(parsed)
        except ValidationError:
            raise DecisionProviderError(
                code="DECISION_OUTPUT_INVALID",
                safe_message="The decision provider returned an invalid decision.",
                status_code=502,
            ) from None
