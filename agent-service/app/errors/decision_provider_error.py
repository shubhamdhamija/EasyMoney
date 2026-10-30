from typing import Literal

DecisionProviderErrorCode = Literal[
    "AGENT_LIVE_CALLS_DISABLED",
    "DECISION_PROVIDER_AUTH_FAILED",
    "DECISION_PROVIDER_RATE_LIMITED",
    "DECISION_PROVIDER_TIMEOUT",
    "DECISION_PROVIDER_UNAVAILABLE",
    "DECISION_PROVIDER_NETWORK_ERROR",
    "DECISION_PROVIDER_EMPTY_RESPONSE",
    "DECISION_PROVIDER_INVALID_JSON",
    "DECISION_PROVIDER_INVALID_RESPONSE",
    "DECISION_OUTPUT_INVALID",
]


class DecisionProviderError(Exception):
    """Safe, structured failure from a decision provider.

    Only `code` and `safe_message` may be shown to HTTP clients. `cause` and
    `provider_status` are for server-side diagnostics and must never carry
    prompts, keys, headers, provider bodies, or raw model output.
    """

    def __init__(
        self,
        *,
        code: DecisionProviderErrorCode,
        safe_message: str,
        status_code: int,
        provider_status: int | None = None,
        cause: BaseException | None = None,
    ) -> None:
        super().__init__(safe_message)
        self.code = code
        self.safe_message = safe_message
        self.status_code = status_code
        self.provider_status = provider_status
        self.cause = cause

    def __str__(self) -> str:
        return self.safe_message

