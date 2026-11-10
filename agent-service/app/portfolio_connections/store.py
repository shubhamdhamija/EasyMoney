from __future__ import annotations

import hashlib
import hmac
import json
import sqlite3
import uuid
from datetime import UTC, datetime
from typing import Any

from cryptography.fernet import Fernet, InvalidToken

from app.portfolio_connections.config import SnapTradeConfig
from app.portfolio_connections.errors import snaptrade_disabled
from app.portfolio_connections.provider import ProviderUserCredentials

PROVIDER = "SNAPTRADE"
ENVIRONMENT = "sandbox"

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS portfolio_provider_users (
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    provider TEXT NOT NULL,
    provider_user_id TEXT NOT NULL,
    user_secret_encrypted TEXT NOT NULL,
    connection_status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    PRIMARY KEY (tenant_id, user_id, provider)
);

CREATE TABLE IF NOT EXISTS portfolio_connection_sessions (
    session_id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    provider TEXT NOT NULL,
    environment TEXT NOT NULL,
    purpose TEXT NOT NULL,
    connection_id TEXT,
    known_provider_connection_ids TEXT NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    consumed_at TEXT
);

CREATE TABLE IF NOT EXISTS portfolio_connections (
    connection_id TEXT PRIMARY KEY,
    provider TEXT NOT NULL,
    provider_connection_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    institution_name TEXT NOT NULL,
    status TEXT NOT NULL,
    connection_type TEXT NOT NULL CHECK (connection_type = 'READ'),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    last_successful_sync_at TEXT,
    last_sync_error_code TEXT,
    disabled_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_portfolio_connections_owner
    ON portfolio_connections (tenant_id, user_id);

CREATE TABLE IF NOT EXISTS portfolio_snapshots (
    connection_id TEXT PRIMARY KEY,
    snapshot_id TEXT NOT NULL,
    fingerprint TEXT NOT NULL,
    effective_at TEXT NOT NULL,
    payload_json TEXT NOT NULL
);
"""

CONNECTION_STATUSES = {
    "PENDING",
    "CONNECTED",
    "SYNCING",
    "READY",
    "DISABLED",
    "RECONNECT_REQUIRED",
    "ERROR",
    "DISCONNECTED",
}


def now_iso() -> str:
    return datetime.now(UTC).isoformat()


class SecretBox:
    """Encrypts provider user secrets at rest and derives opaque provider user IDs."""

    def __init__(self, config: SnapTradeConfig) -> None:
        raw = config.encryption_key.get_secret_value().strip().encode()
        try:
            self._fernet = Fernet(raw)
        except (ValueError, TypeError) as exc:
            raise snaptrade_disabled() from exc
        self._hmac_key = raw
        self._environment = config.environment

    def encrypt(self, plaintext: str) -> str:
        return self._fernet.encrypt(plaintext.encode()).decode()

    def decrypt(self, token: str) -> str:
        try:
            return self._fernet.decrypt(token.encode()).decode()
        except InvalidToken as exc:
            raise snaptrade_disabled() from exc

    def provider_user_id(self, tenant_id: str, user_id: str) -> str:
        """easymoney_<environment>_<opaque-hash>. No raw tenant/user in the output."""
        digest = hmac.new(
            self._hmac_key,
            f"{self._environment}\x00{tenant_id}\x00{user_id}".encode(),
            hashlib.sha256,
        ).hexdigest()[:40]
        return f"easymoney_{self._environment}_{digest}"


class PortfolioConnectionStore:
    def __init__(self, connection: sqlite3.Connection) -> None:
        self._db = connection
        self._db.executescript(SCHEMA_SQL)

    # -- provider users ---------------------------------------------------

    def get_provider_user(
        self, tenant_id: str, user_id: str, box: SecretBox
    ) -> ProviderUserCredentials | None:
        row = self._db.execute(
            "SELECT provider_user_id, user_secret_encrypted FROM portfolio_provider_users "
            "WHERE tenant_id=? AND user_id=? AND provider=?",
            (tenant_id, user_id, PROVIDER),
        ).fetchone()
        if row is None:
            return None
        return ProviderUserCredentials(
            provider_user_id=row["provider_user_id"],
            user_secret=box.decrypt(row["user_secret_encrypted"]),
        )

    def save_provider_user(
        self, tenant_id: str, user_id: str, creds: ProviderUserCredentials, box: SecretBox
    ) -> None:
        now = now_iso()
        self._db.execute(
            "INSERT OR REPLACE INTO portfolio_provider_users "
            "(tenant_id,user_id,provider,provider_user_id,user_secret_encrypted,"
            "connection_status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",
            (
                tenant_id,
                user_id,
                PROVIDER,
                creds.provider_user_id,
                box.encrypt(creds.user_secret),
                "PENDING",
                now,
                now,
            ),
        )

    # -- sessions ---------------------------------------------------------

    def create_session(
        self,
        *,
        tenant_id: str,
        user_id: str,
        purpose: str,
        connection_id: str | None,
        known_provider_connection_ids: list[str],
        expires_at: datetime,
    ) -> str:
        session_id = "cs_" + uuid.uuid4().hex
        self._db.execute(
            "INSERT INTO portfolio_connection_sessions (session_id,tenant_id,user_id,provider,"
            "environment,purpose,connection_id,known_provider_connection_ids,created_at,"
            "expires_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
            (
                session_id,
                tenant_id,
                user_id,
                PROVIDER,
                ENVIRONMENT,
                purpose,
                connection_id,
                json.dumps(sorted(known_provider_connection_ids)),
                now_iso(),
                expires_at.isoformat(),
            ),
        )
        return session_id

    def get_session(self, session_id: str, tenant_id: str, user_id: str) -> dict[str, Any] | None:
        row = self._db.execute(
            "SELECT * FROM portfolio_connection_sessions WHERE session_id=? AND tenant_id=? "
            "AND user_id=? AND provider=? AND environment=?",
            (session_id, tenant_id, user_id, PROVIDER, ENVIRONMENT),
        ).fetchone()
        if row is None:
            return None
        data = dict(row)
        data["known_provider_connection_ids"] = json.loads(data["known_provider_connection_ids"])
        return data

    def consume_session(self, session_id: str) -> bool:
        cur = self._db.execute(
            "UPDATE portfolio_connection_sessions SET consumed_at=? "
            "WHERE session_id=? AND consumed_at IS NULL",
            (now_iso(), session_id),
        )
        return cur.rowcount == 1

    # -- connections ------------------------------------------------------

    def create_connection(
        self,
        *,
        tenant_id: str,
        user_id: str,
        provider_connection_id: str,
        institution_name: str,
        status: str = "CONNECTED",
    ) -> dict[str, Any]:
        connection_id = "pc_" + uuid.uuid4().hex
        now = now_iso()
        self._db.execute(
            "INSERT INTO portfolio_connections (connection_id,provider,provider_connection_id,"
            "tenant_id,user_id,institution_name,status,connection_type,created_at,updated_at)"
            " VALUES (?,?,?,?,?,?,?,?,?,?)",
            (
                connection_id,
                PROVIDER,
                provider_connection_id,
                tenant_id,
                user_id,
                institution_name,
                status,
                "READ",
                now,
                now,
            ),
        )
        return self.get_connection(connection_id, tenant_id, user_id)  # type: ignore[return-value]

    def get_connection(
        self, connection_id: str, tenant_id: str, user_id: str
    ) -> dict[str, Any] | None:
        row = self._db.execute(
            "SELECT * FROM portfolio_connections WHERE connection_id=? AND tenant_id=? "
            "AND user_id=?",
            (connection_id, tenant_id, user_id),
        ).fetchone()
        return dict(row) if row else None

    def find_by_provider_connection(
        self, provider_connection_id: str, tenant_id: str, user_id: str
    ) -> dict[str, Any] | None:
        row = self._db.execute(
            "SELECT * FROM portfolio_connections WHERE provider_connection_id=? AND tenant_id=? "
            "AND user_id=? AND status != 'DISCONNECTED'",
            (provider_connection_id, tenant_id, user_id),
        ).fetchone()
        return dict(row) if row else None

    def list_connections(self, tenant_id: str, user_id: str) -> list[dict[str, Any]]:
        rows = self._db.execute(
            "SELECT * FROM portfolio_connections WHERE tenant_id=? AND user_id=? "
            "ORDER BY created_at",
            (tenant_id, user_id),
        ).fetchall()
        return [dict(r) for r in rows]

    def set_status(
        self,
        connection_id: str,
        status: str,
        *,
        error_code: str | None = None,
        success: bool = False,
    ) -> None:
        assert status in CONNECTION_STATUSES
        now = now_iso()
        self._db.execute(
            "UPDATE portfolio_connections SET status=?, updated_at=?, last_sync_error_code=?, "
            "last_successful_sync_at=CASE WHEN ? THEN ? ELSE last_successful_sync_at END, "
            "disabled_at=CASE WHEN ?='DISCONNECTED' OR ?='DISABLED' THEN ? ELSE disabled_at END "
            "WHERE connection_id=?",
            (status, now, error_code, 1 if success else 0, now, status, status, now, connection_id),
        )

    # -- snapshots --------------------------------------------------------

    def get_snapshot(self, connection_id: str) -> dict[str, Any] | None:
        row = self._db.execute(
            "SELECT * FROM portfolio_snapshots WHERE connection_id=?", (connection_id,)
        ).fetchone()
        if row is None:
            return None
        return {
            "snapshot_id": row["snapshot_id"],
            "fingerprint": row["fingerprint"],
            "effective_at": row["effective_at"],
            "payload": json.loads(row["payload_json"]),
        }

    def replace_snapshot_and_mark_ready(
        self,
        connection_id: str,
        *,
        snapshot_id: str,
        fingerprint: str,
        effective_at: str,
        payload: dict[str, Any],
    ) -> None:
        """Atomic: the snapshot swap and READY status commit together or not at all."""
        self._db.execute("BEGIN IMMEDIATE")
        try:
            self._db.execute(
                "INSERT OR REPLACE INTO portfolio_snapshots "
                "(connection_id,snapshot_id,fingerprint,effective_at,payload_json) "
                "VALUES (?,?,?,?,?)",
                (connection_id, snapshot_id, fingerprint, effective_at, json.dumps(payload)),
            )
            self.set_status(connection_id, "READY", success=True)
            self._db.execute("COMMIT")
        except Exception:
            self._db.execute("ROLLBACK")
            raise

    def delete_snapshot(self, connection_id: str) -> None:
        self._db.execute("DELETE FROM portfolio_snapshots WHERE connection_id=?", (connection_id,))
