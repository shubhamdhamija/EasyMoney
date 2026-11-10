"""Redacted audit events for portfolio research.

Only an allowlist of metadata keys is ever written. Symbols, holdings, quantities,
values, balances, account names, provider identifiers, consent contents, prompts,
JWTs and credentials are never audited.
"""

from __future__ import annotations

import logging
from typing import Any, Protocol

logger = logging.getLogger(__name__)

PORTFOLIO_RESEARCH_CONSENT_GRANTED = "PORTFOLIO_RESEARCH_CONSENT_GRANTED"
PORTFOLIO_RESEARCH_CONSENT_REVOKED = "PORTFOLIO_RESEARCH_CONSENT_REVOKED"
PORTFOLIO_RESEARCH_STARTED = "PORTFOLIO_RESEARCH_STARTED"
PORTFOLIO_TOOL_EXECUTED = "PORTFOLIO_TOOL_EXECUTED"
PORTFOLIO_RESEARCH_COMPLETED = "PORTFOLIO_RESEARCH_COMPLETED"
PORTFOLIO_RESEARCH_FAILED = "PORTFOLIO_RESEARCH_FAILED"
PORTFOLIO_ACCESS_DENIED = "PORTFOLIO_ACCESS_DENIED"

ALLOWED_METADATA_KEYS = frozenset(
    {
        "portfolio_id",
        "source",
        "snapshot_at",
        "categories",
        "position_count",
        "currency_codes",
        "duration_ms",
    }
)


def sanitize_metadata(metadata: dict[str, Any] | None) -> dict[str, Any]:
    clean: dict[str, Any] = {}
    for key, value in (metadata or {}).items():
        if key not in ALLOWED_METADATA_KEYS:
            continue
        if isinstance(value, (str, int, float, bool)) or value is None:
            clean[key] = value
        elif isinstance(value, (list, tuple)) and all(isinstance(v, str) for v in value):
            clean[key] = list(value)
    return clean


class AuditSink(Protocol):
    def append(self, **kwargs: Any) -> None: ...


class InMemoryAuditSink:
    def __init__(self) -> None:
        self.events: list[dict[str, Any]] = []

    def append(self, **kwargs: Any) -> None:
        self.events.append(dict(kwargs))


class PortfolioAudit:
    def __init__(self, sink: AuditSink | None = None) -> None:
        self._sink = sink

    def record(
        self,
        event_type: str,
        outcome: str,
        *,
        request_id: str | None = None,
        tenant_id: str | None = None,
        user_id: str | None = None,
        tool_name: str | None = None,
        safe_error_code: str | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> None:
        clean = sanitize_metadata(metadata)
        logger.info(
            "portfolio_audit event=%s outcome=%s request_id=%s tool=%s error_code=%s",
            event_type,
            outcome,
            request_id,
            tool_name,
            safe_error_code,
        )
        if self._sink is None:
            return
        try:
            self._sink.append(
                event_type=event_type,
                outcome=outcome,
                request_id=request_id,
                tenant_id=tenant_id,
                user_id=user_id,
                tool_name=tool_name,
                safe_error_code=safe_error_code,
                metadata=clean,
            )
        except Exception as error:  # auditing must never break the request
            logger.warning("portfolio_audit_failed type=%s", type(error).__name__)
