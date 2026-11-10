"""In-memory, request-scoped portfolio research consent.

Documented limitation: consent records live in process memory. They are lost on a
restart and are not shared between workers. That is acceptable because consent is
short-lived (15 minutes) and a missing record fails closed (consent required).

Consent is generated only by application code (the consent endpoint), is tied to
exactly one tenant, user and portfolio, expires automatically, can be revoked, and
never authorizes anything except read-only portfolio tools. It holds no secrets.
"""

from __future__ import annotations

import threading
import uuid
from collections.abc import Callable
from datetime import UTC, datetime, timedelta

from app.errors.portfolio_agent_error import consent_expired, consent_required
from app.models.portfolio_agent import PortfolioResearchConsent, PortfolioResearchDataCategory

CONSENT_LIFETIME = timedelta(minutes=15)

Clock = Callable[[], datetime]


def _utc_now() -> datetime:
    return datetime.now(UTC)


class PortfolioConsentStore:
    def __init__(self, *, clock: Clock = _utc_now, lifetime: timedelta = CONSENT_LIFETIME) -> None:
        self._clock = clock
        self._lifetime = lifetime
        self._records: dict[str, PortfolioResearchConsent] = {}
        self._lock = threading.Lock()

    def grant(
        self,
        *,
        tenant_id: str,
        user_id: str,
        portfolio_id: str,
        categories: list[PortfolioResearchDataCategory],
        request_id: str | None,
    ) -> PortfolioResearchConsent:
        now = self._clock()
        ordered = [c for c in PortfolioResearchDataCategory if c in set(categories)]
        record = PortfolioResearchConsent(
            consent_id="pcn_" + uuid.uuid4().hex,
            tenant_id=tenant_id,
            user_id=user_id,
            portfolio_id=portfolio_id,
            request_id=request_id,
            granted_at=now,
            expires_at=now + self._lifetime,
            included_data_categories=ordered,
        )
        with self._lock:
            self._purge(now)
            self._records[record.consent_id] = record
        return record

    def revoke(
        self, *, tenant_id: str, user_id: str, portfolio_id: str, consent_id: str
    ) -> PortfolioResearchConsent | None:
        """Returns the revoked record, or None when no owned record matches."""
        with self._lock:
            record = self._records.get(consent_id)
            if (
                record is None
                or record.tenant_id != tenant_id
                or record.user_id != user_id
                or record.portfolio_id != portfolio_id
            ):
                return None
            revoked = record.model_copy(update={"revoked_at": self._clock()})
            self._records[consent_id] = revoked
            return revoked

    def verify(
        self,
        *,
        tenant_id: str | None,
        user_id: str | None,
        portfolio_id: str,
        consent_id: str | None,
        category: PortfolioResearchDataCategory | None = None,
    ) -> PortfolioResearchConsent:
        """Fail closed. Raises PortfolioAgentError unless an owned, live consent matches."""
        if not tenant_id or not user_id or not consent_id:
            raise consent_required()
        with self._lock:
            record = self._records.get(consent_id)
        if (
            record is None
            or record.tenant_id != tenant_id
            or record.user_id != user_id
            or record.portfolio_id != portfolio_id
            or record.revoked_at is not None
        ):
            raise consent_required()
        if self._clock() >= record.expires_at:
            raise consent_expired()
        if category is not None and category not in record.included_data_categories:
            raise consent_required()
        return record

    def _purge(self, now: datetime) -> None:
        stale = [k for k, v in self._records.items() if now >= v.expires_at + timedelta(hours=1)]
        for key in stale:
            del self._records[key]
