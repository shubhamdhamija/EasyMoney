from __future__ import annotations

import sqlite3
import time
from datetime import UTC, datetime, timedelta
from typing import Any

from app.auth.auth_models import AuthenticatedPrincipal
from app.persistence.audit_repository import AuditRepository
from app.portfolio_connections.capability_policy import Capability, PortfolioProviderCapabilityPolicy
from app.portfolio_connections.config import SnapTradeConfig
from app.portfolio_connections.errors import (
    PortfolioConnectionError,
    connection_incomplete,
    connection_not_found,
    invalid_request,
    no_snapshot,
    session_consumed,
    session_expired,
    session_not_found,
)
from app.portfolio_connections.provider import PortfolioConnectionProvider
from app.portfolio_connections.store import (
    ENVIRONMENT,
    PROVIDER,
    PortfolioConnectionStore,
    SecretBox,
)
from app.portfolio_connections.sync_service import PortfolioSyncService, connection_view

SESSION_TTL = timedelta(minutes=10)


class PortfolioConnectionService:
    """Orchestrates read-only connection lifecycle. Owner of the SnapTrade integration."""

    def __init__(
        self,
        *,
        config: SnapTradeConfig,
        provider: PortfolioConnectionProvider,
        db: sqlite3.Connection,
    ) -> None:
        self._config = config
        self._provider = provider
        self._store = PortfolioConnectionStore(db)
        self._box = SecretBox(config)
        self._audit = AuditRepository(db)
        self._sync = PortfolioSyncService(
            config=config, provider=provider, store=self._store, box=self._box, audit=self._audit
        )

    # -- helpers ----------------------------------------------------------

    def _owned(self, principal: AuthenticatedPrincipal, connection_id: str) -> dict[str, Any]:
        row = self._store.get_connection(connection_id, principal.tenant_id, principal.user_id)
        if row is None:
            raise connection_not_found()
        return row

    async def _credentials(self, principal: AuthenticatedPrincipal):
        creds = self._store.get_provider_user(principal.tenant_id, principal.user_id, self._box)
        if creds is None:
            creds = await self._provider.create_provider_user(
                self._box.provider_user_id(principal.tenant_id, principal.user_id)
            )
            self._store.save_provider_user(principal.tenant_id, principal.user_id, creds, self._box)
        return creds

    # -- sessions ---------------------------------------------------------

    async def create_session(
        self, principal: AuthenticatedPrincipal, redirect_uri: str
    ) -> dict[str, Any]:
        PortfolioProviderCapabilityPolicy.require(Capability.CREATE_READ_CONNECTION)
        if redirect_uri != self._config.redirect_uri:
            raise invalid_request()
        started = time.monotonic()
        try:
            creds = await self._credentials(principal)
            known = [c.provider_connection_id for c in await self._provider.get_connections(creds)]
            portal = await self._provider.create_connection_portal(creds)
            expires_at = min(datetime.now(UTC) + SESSION_TTL, portal.expires_at)
            session_id = self._store.create_session(
                tenant_id=principal.tenant_id,
                user_id=principal.user_id,
                purpose="CONNECT",
                connection_id=None,
                known_provider_connection_ids=known,
                expires_at=expires_at,
            )
        except PortfolioConnectionError as error:
            self._sync.audit(
                "PORTFOLIO_CONNECTION_FAILED", principal, None, "failure", started, error.code
            )
            raise
        self._sync.audit("PORTFOLIO_CONNECTION_SESSION_CREATED", principal, None, "success", started)
        # The portal URL is returned once and never stored.
        return {
            "connectionSessionId": session_id,
            "portalUrl": portal.portal_url,
            "expiresAt": expires_at.isoformat(),
        }

    async def create_reconnect_session(
        self, principal: AuthenticatedPrincipal, connection_id: str
    ) -> dict[str, Any]:
        PortfolioProviderCapabilityPolicy.require(Capability.CREATE_READ_CONNECTION)
        row = self._owned(principal, connection_id)
        if row["status"] == "DISCONNECTED":
            raise connection_not_found()
        started = time.monotonic()
        try:
            creds = await self._credentials(principal)
            portal = await self._provider.create_connection_portal(
                creds, reconnect_provider_connection_id=row["provider_connection_id"]
            )
            expires_at = min(datetime.now(UTC) + SESSION_TTL, portal.expires_at)
            session_id = self._store.create_session(
                tenant_id=principal.tenant_id,
                user_id=principal.user_id,
                purpose="RECONNECT",
                connection_id=connection_id,
                known_provider_connection_ids=[],
                expires_at=expires_at,
            )
        except PortfolioConnectionError as error:
            self._sync.audit(
                "PORTFOLIO_CONNECTION_FAILED", principal, connection_id, "failure", started, error.code
            )
            raise
        self._sync.audit(
            "PORTFOLIO_CONNECTION_SESSION_CREATED", principal, connection_id, "success", started
        )
        return {
            "connectionSessionId": session_id,
            "portalUrl": portal.portal_url,
            "expiresAt": expires_at.isoformat(),
        }

    async def complete(
        self, principal: AuthenticatedPrincipal, session_id: str
    ) -> dict[str, Any]:
        """The Android deep link is NOT proof of success. SnapTrade is the source of truth."""
        started = time.monotonic()
        session = self._store.get_session(session_id, principal.tenant_id, principal.user_id)
        if session is None:
            raise session_not_found()
        if session["consumed_at"] is not None:
            raise session_consumed()
        if datetime.fromisoformat(session["expires_at"]) <= datetime.now(UTC):
            raise session_expired()

        creds = self._store.get_provider_user(principal.tenant_id, principal.user_id, self._box)
        if creds is None:
            raise session_not_found()
        try:
            provider_connections = await self._provider.get_connections(creds)
            if session["purpose"] == "RECONNECT":
                row = self._owned(principal, session["connection_id"])
                match = [
                    c
                    for c in provider_connections
                    if c.provider_connection_id == row["provider_connection_id"] and not c.disabled
                ]
                if not match:
                    raise connection_incomplete()
                self._store.set_status(row["connection_id"], "CONNECTED")
                connection_id = row["connection_id"]
            else:
                known = set(session["known_provider_connection_ids"])
                candidates = [
                    c
                    for c in provider_connections
                    if c.provider_connection_id not in known
                    and not c.disabled
                    and self._store.find_by_provider_connection(
                        c.provider_connection_id, principal.tenant_id, principal.user_id
                    )
                    is None
                ]
                if not candidates:
                    raise connection_incomplete()
                created = self._store.create_connection(
                    tenant_id=principal.tenant_id,
                    user_id=principal.user_id,
                    provider_connection_id=candidates[0].provider_connection_id,
                    institution_name=candidates[0].institution_name or "SnapTrade Sandbox",
                )
                connection_id = created["connection_id"]
        except PortfolioConnectionError as error:
            self._sync.audit(
                "PORTFOLIO_CONNECTION_FAILED", principal, None, "failure", started, error.code
            )
            raise

        self._store.consume_session(session_id)
        self._sync.audit("PORTFOLIO_CONNECTION_COMPLETED", principal, connection_id, "success", started)

        portfolio: dict[str, Any] | None = None
        try:
            # One bounded sync. No explicit provider refresh right after connect.
            row, portfolio = await self._sync.sync_connection(
                principal, connection_id, refresh=False
            )
        except PortfolioConnectionError:
            row = self._owned(principal, connection_id)
        return {"connection": connection_view(row, self._store.get_snapshot(connection_id)),
                "portfolio": portfolio}

    # -- reads / sync -----------------------------------------------------

    def list_connections(self, principal: AuthenticatedPrincipal) -> list[dict[str, Any]]:
        PortfolioProviderCapabilityPolicy.require(Capability.READ_CONNECTION_STATUS)
        rows = self._store.list_connections(principal.tenant_id, principal.user_id)
        return [connection_view(r, self._store.get_snapshot(r["connection_id"])) for r in rows]

    def get_portfolio(self, principal: AuthenticatedPrincipal, connection_id: str) -> dict[str, Any]:
        row = self._owned(principal, connection_id)
        if row["status"] == "DISCONNECTED":
            raise connection_not_found()
        snapshot = self._store.get_snapshot(connection_id)
        if snapshot is None:
            raise no_snapshot()
        return {"connection": connection_view(row, snapshot), "portfolio": snapshot["payload"]}

    async def sync(self, principal: AuthenticatedPrincipal, connection_id: str) -> dict[str, Any]:
        row, portfolio = await self._sync.sync_connection(principal, connection_id, refresh=True)
        return {
            "connection": connection_view(row, self._store.get_snapshot(connection_id)),
            "portfolio": portfolio,
        }

    # -- disconnect / delete ---------------------------------------------

    async def disconnect(self, principal: AuthenticatedPrincipal, connection_id: str) -> None:
        PortfolioProviderCapabilityPolicy.require(Capability.DISCONNECT_CONNECTION)
        row = self._owned(principal, connection_id)
        started = time.monotonic()
        if row["status"] != "DISCONNECTED":
            creds = self._store.get_provider_user(principal.tenant_id, principal.user_id, self._box)
            if creds is not None:
                await self._provider.disconnect_connection(creds, row["provider_connection_id"])
            self._store.set_status(connection_id, "DISCONNECTED")
        self._sync.audit("PORTFOLIO_DISCONNECTED", principal, connection_id, "success", started)

    def delete_data(
        self, principal: AuthenticatedPrincipal, connection_id: str, *, confirm: bool
    ) -> None:
        PortfolioProviderCapabilityPolicy.require(Capability.DELETE_IMPORTED_DATA)
        row = self._owned(principal, connection_id)
        if confirm is not True:
            raise invalid_request()
        self._store.delete_snapshot(connection_id)
        if row["status"] == "READY":
            self._store.set_status(connection_id, "CONNECTED")
        self._sync.audit("PORTFOLIO_DATA_DELETED", principal, connection_id, "success")


__all__ = ["PortfolioConnectionService", "ENVIRONMENT", "PROVIDER"]
