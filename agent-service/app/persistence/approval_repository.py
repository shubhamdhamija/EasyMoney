from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from typing import Any


class ApprovalRepository:
    def __init__(self, connection: sqlite3.Connection) -> None:
        self._connection = connection

    def create_approval(
        self,
        *,
        approval_id: str,
        thread_id: str,
        tenant_id: str,
        user_id: str,
        tool_name: str,
        prepared_arguments_hash: str,
        safe_arguments_json: dict[str, Any],
        reason: str,
        status: str,
        created_at: datetime,
        expires_at: datetime,
        approval_json: dict[str, Any],
    ) -> None:
        self._connection.execute(
            """
            INSERT INTO approvals (
                approval_id, thread_id, tenant_id, user_id, tool_name,
                prepared_arguments_hash, safe_arguments_json, reason, status,
                created_at, expires_at, decided_at, consumed_at, reviewer_note,
                approval_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                approval_id,
                thread_id,
                tenant_id,
                user_id,
                tool_name,
                prepared_arguments_hash,
                json.dumps(safe_arguments_json, sort_keys=True),
                reason,
                status,
                created_at.isoformat(),
                expires_at.isoformat(),
                None,
                None,
                None,
                json.dumps(approval_json, sort_keys=True),
            ),
        )

    def get_by_id(self, approval_id: str) -> dict[str, Any] | None:
        row = self._connection.execute(
            "SELECT * FROM approvals WHERE approval_id = ?",
            (approval_id,),
        ).fetchone()
        if row is None:
            return None
        return dict(row)

    def update_status(
        self,
        approval_id: str,
        *,
        status: str,
        decided_at: datetime | None = None,
        consumed_at: datetime | None = None,
        reviewer_note: str | None = None,
    ) -> None:
        values: list[Any] = [status]
        if decided_at is not None:
            values.append(decided_at.isoformat())
        else:
            values.append(None)
        if consumed_at is not None:
            values.append(consumed_at.isoformat())
        else:
            values.append(None)
        if reviewer_note is not None:
            values.append(reviewer_note)
        else:
            values.append(None)
        values.append(approval_id)
        self._connection.execute(
            "UPDATE approvals SET status = ?, decided_at = ?, consumed_at = ?, reviewer_note = ? WHERE approval_id = ?",
            values,
        )

    def list_expired_pending(self, now: datetime | None = None) -> list[str]:
        cutoff = (now or datetime.now(UTC)).isoformat()
        rows = self._connection.execute(
            "SELECT approval_id FROM approvals WHERE status = 'PENDING' AND expires_at <= ?",
            (cutoff,),
        ).fetchall()
        return [row["approval_id"] for row in rows]

    def mark_expired(self, approval_id: str, now: datetime | None = None) -> None:
        self.update_status(approval_id, status="EXPIRED", decided_at=now or datetime.now(UTC))

    def mark_cancelled(self, approval_id: str, now: datetime | None = None) -> None:
        self.update_status(approval_id, status="CANCELLED", decided_at=now or datetime.now(UTC))

    def consume(self, approval_id: str, *, now: datetime | None = None) -> bool:
        cutoff = now or datetime.now(UTC)
        cursor = self._connection.execute(
            "UPDATE approvals SET status = 'CONSUMED', consumed_at = ?, decided_at = COALESCE(decided_at, ?) WHERE approval_id = ? AND status IN ('APPROVED', 'PENDING')",
            (cutoff.isoformat(), cutoff.isoformat(), approval_id),
        )
        return cursor.rowcount > 0

    def get_for_thread(self, thread_id: str) -> list[dict[str, Any]]:
        rows = self._connection.execute(
            "SELECT * FROM approvals WHERE thread_id = ? ORDER BY created_at DESC",
            (thread_id,),
        ).fetchall()
        return [dict(row) for row in rows]

