from __future__ import annotations

import asyncio
import time
from datetime import UTC, datetime
from typing import Any

from app.auth.auth_models import AuthenticatedPrincipal
from app.persistence.audit_repository import AuditRepository
from app.portfolio_connections.capability_policy import Capability, PortfolioProviderCapabilityPolicy
from app.portfolio_connections.config import SnapTradeConfig
from app.portfolio_connections.errors import (
    PortfolioConnectionError,
    connection_not_found,
    forbidden_capability,
    provider_unavailable,
    sync_in_progress,
    sync_timeout,
)
from app.portfolio_connections.normalizer import (
    NormalizedPortfolio,
    normalize_account,
    normalize_cash,
    normalize_position,
)
from app.portfolio_connections.provider import PortfolioConnectionProvider
from app.portfolio_connections.store import (
    ENVIRONMENT,
    PROVIDER,
    PortfolioConnectionStore,
    SecretBox,
)
from app.request_context import get_request_id

# Sync is bounded to a multiple of the per-request provider timeout.
SYNC_TIMEOUT_MULTIPLIER = 3


def connection_view(row: dict[str, Any], snapshot: dict[str, Any] | None = None) -> dict[str, Any]:
    """Public connection shape. Never includes provider IDs, secrets or tokens."""
    payload = (snapshot or {}).get("payload") or {}
    return {
        "connectionId": row["connection_id"],
        "provider": row["provider"],
        "institutionName": row["institution_name"],
        "status": row["status"],
        "connectionType": "READ",
        "createdAt": row["created_at"],
        "lastSuccessfulSyncAt": row["last_successful_sync_at"],
        "lastSyncErrorCode": row["last_sync_error_code"],
        "accountCount": len(payload.get("accounts", [])),
        "positionCount": len(payload.get("positions", [])),
        "cashBalanceCount": len(payload.get("cashBalances", [])),
    }


class PortfolioSyncService:
    def __init__(
        self,
        *,
        config: SnapTradeConfig,
        provider: PortfolioConnectionProvider,
        store: PortfolioConnectionStore,
        box: SecretBox,
        audit: AuditRepository,
    ) -> None:
        self._config = config
        self._provider = provider
        self._store = store
        self._box = box
        self._audit = audit
        self._locks: dict[str, asyncio.Lock] = {}

    def _event(
        self,
        event_type: str,
        principal: AuthenticatedPrincipal,
        connection_id: str | None,
        outcome: str,
        started: float | None = None,
        error_code: str | None = None,
    ) -> None:
        metadata: dict[str, Any] = {"provider": PROVIDER, "environment": ENVIRONMENT}
        if connection_id:
            metadata["connection_id"] = connection_id
        if started is not None:
            metadata["duration_ms"] = int((time.monotonic() - started) * 1000)
        self._audit.append(
            event_type=event_type,
            outcome=outcome,
            request_id=get_request_id(),
            tenant_id=principal.tenant_id,
            user_id=principal.user_id,
            safe_error_code=error_code,
            metadata=metadata,
        )

    def audit(self, *args: Any, **kwargs: Any) -> None:
        self._event(*args, **kwargs)

    async def sync_connection(
        self,
        principal: AuthenticatedPrincipal,
        connection_id: str,
        *,
        refresh: bool = True,
    ) -> tuple[dict[str, Any], dict[str, Any] | None]:
        # authenticated (caller) -> tenant + user + ownership match -> read-only check
        row = self._store.get_connection(connection_id, principal.tenant_id, principal.user_id)
        if row is None or row["status"] == "DISCONNECTED":
            raise connection_not_found()
        if row["connection_type"] != "READ":
            raise forbidden_capability()
        PortfolioProviderCapabilityPolicy.require(Capability.READ_POSITIONS)

        lock = self._locks.setdefault(connection_id, asyncio.Lock())
        if lock.locked():
            raise sync_in_progress()
        async with lock:
            return await self._sync_locked(principal, row, refresh=refresh)

    async def _sync_locked(
        self, principal: AuthenticatedPrincipal, row: dict[str, Any], *, refresh: bool
    ) -> tuple[dict[str, Any], dict[str, Any] | None]:
        connection_id = row["connection_id"]
        previous = self._store.get_snapshot(connection_id)
        prior_status = row["status"]
        started = time.monotonic()
        self._event("PORTFOLIO_SYNC_STARTED", principal, connection_id, "started")
        self._store.set_status(connection_id, "SYNCING")
        try:
            portfolio = await asyncio.wait_for(
                self._collect(principal, row, refresh=refresh),
                timeout=self._config.timeout_seconds * SYNC_TIMEOUT_MULTIPLIER,
            )
        except asyncio.CancelledError:
            self._store.set_status(connection_id, prior_status)
            raise
        except TimeoutError:
            return self._fail(principal, connection_id, previous, sync_timeout(), started)
        except PortfolioConnectionError as error:
            return self._fail(principal, connection_id, previous, error, started)
        except Exception:
            return self._fail(principal, connection_id, previous, provider_unavailable(), started)

        partial = any(w.startswith("ACCOUNT_PARTIAL") for w in portfolio.warnings)
        if partial and previous is not None:
            # Never replace a valid snapshot with an incomplete one.
            self._store.set_status(
                connection_id, "READY", error_code="PARTIAL_SYNC", success=False
            )
            payload = dict(previous["payload"])
            payload["warnings"] = sorted(set(payload.get("warnings", []) + portfolio.warnings))
            return self._finish(principal, connection_id, payload, started, "partial")

        fingerprint = portfolio.fingerprint()
        if previous is not None and previous["fingerprint"] == fingerprint:
            effective_at = previous["effective_at"]
        else:
            effective_at = datetime.now(UTC).isoformat()
        snapshot_id = "snap_" + fingerprint[:24]
        payload = portfolio.to_dict(
            snapshot_id=snapshot_id, effective_at=datetime.fromisoformat(effective_at)
        )
        self._store.replace_snapshot_and_mark_ready(
            connection_id,
            snapshot_id=snapshot_id,
            fingerprint=fingerprint,
            effective_at=effective_at,
            payload=payload,
        )
        return self._finish(principal, connection_id, payload, started, "success")

    def _finish(
        self,
        principal: AuthenticatedPrincipal,
        connection_id: str,
        payload: dict[str, Any],
        started: float,
        outcome: str,
    ) -> tuple[dict[str, Any], dict[str, Any]]:
        self._event("PORTFOLIO_SYNC_COMPLETED", principal, connection_id, outcome, started)
        row = self._store.get_connection(connection_id, principal.tenant_id, principal.user_id)
        assert row is not None
        return row, payload

    def _fail(
        self,
        principal: AuthenticatedPrincipal,
        connection_id: str,
        previous: dict[str, Any] | None,
        error: PortfolioConnectionError,
        started: float,
    ) -> Any:
        if error.code == "RECONNECT_REQUIRED":
            status = "RECONNECT_REQUIRED"
            self._event(
                "PORTFOLIO_RECONNECT_REQUIRED", principal, connection_id, "failure", started, error.code
            )
        else:
            # A failed refresh never deletes a valid snapshot.
            status = "READY" if previous is not None else "ERROR"
        self._store.set_status(connection_id, status, error_code=error.code)
        self._event("PORTFOLIO_SYNC_FAILED", principal, connection_id, "failure", started, error.code)
        raise error

    async def _collect(
        self, principal: AuthenticatedPrincipal, row: dict[str, Any], *, refresh: bool
    ) -> NormalizedPortfolio:
        creds = self._store.get_provider_user(principal.tenant_id, principal.user_id, self._box)
        if creds is None:
            raise connection_not_found()
        warnings: list[str] = []
        provider_connection_id = row["provider_connection_id"]

        if refresh:
            try:
                await self._provider.refresh_connection(creds, provider_connection_id)
            except PortfolioConnectionError as error:
                if error.code in ("RECONNECT_REQUIRED", "PROVIDER_RATE_LIMITED", "SYNC_TIMEOUT"):
                    raise
                warnings.append("REFRESH_FAILED")

        provider_accounts = [
            a
            for a in await self._provider.get_accounts(creds)
            if a.provider_connection_id in (None, provider_connection_id)
        ]
        accounts = [normalize_account(a) for a in provider_accounts]
        positions = []
        cash = []
        for account in provider_accounts:
            try:
                for balance in await self._provider.get_balances(creds, account.provider_account_id):
                    cash.append(normalize_cash(account.provider_account_id, balance))
                for position in await self._provider.get_positions(creds, account.provider_account_id):
                    normalized = normalize_position(account, position, warnings)
                    if normalized is not None:
                        positions.append(normalized)
            except PortfolioConnectionError as error:
                if error.code in ("RECONNECT_REQUIRED", "PROVIDER_RATE_LIMITED", "SYNC_TIMEOUT"):
                    raise
                warnings.append(f"ACCOUNT_PARTIAL:{error.code}")
        return NormalizedPortfolio(
            accounts=accounts,
            positions=positions,
            cash_balances=cash,
            warnings=sorted(set(warnings)),
        )
