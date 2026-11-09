from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from typing import Any


class AuditRepository:
    def __init__(self, connection: sqlite3.Connection) -> None:
        self._connection = connection

    def append(
        self,
        *,
        event_type: str,
        outcome: str,
        request_id: str | None = None,
        thread_id: str | None = None,
        approval_id: str | None = None,
        tenant_id: str | None = None,
        user_id: str | None = None,
        tool_name: str | None = None,
        safe_error_code: str | None = None,
        metadata: dict[str, Any] | None = None,
        occurred_at: datetime | None = None,
    ) -> None:
        metadata_json = json.dumps(metadata or {}, sort_keys=True)
        self._connection.execute(
            """
            INSERT INTO audit_events (
                event_id, event_type, occurred_at, request_id, thread_id,
                approval_id, tenant_id, user_id, tool_name, outcome,
                safe_error_code, metadata_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                f"evt-{__import__('uuid').uuid4().hex}",
                event_type,
                (occurred_at or datetime.now(UTC)).isoformat(),
                request_id,
                thread_id,
                approval_id,
                tenant_id,
                user_id,
                tool_name,
                outcome,
                safe_error_code,
                metadata_json,
            ),
        )

    def list_all(self) -> list[dict[str, Any]]:
        rows = self._connection.execute("SELECT * FROM audit_events ORDER BY occurred_at").fetchall()
        return [dict(row) for row in rows]

