from __future__ import annotations

import json
import sqlite3
from datetime import UTC, datetime
from typing import Any

from app.models.workflow_identity import WorkflowIdentity, generate_thread_id, workflow_expiration


class WorkflowRepository:
    def __init__(self, connection: sqlite3.Connection) -> None:
        self._connection = connection

    def create_workflow(
        self,
        *,
        tenant_id: str,
        user_id: str,
        workflow_json: dict[str, Any] | None = None,
        status: str = "ACTIVE",
        expires_at: datetime | None = None,
        thread_id: str | None = None,
    ) -> WorkflowIdentity:
        now = datetime.now(UTC)
        final_thread_id = thread_id or generate_thread_id()
        if expires_at is None:
            expires_at = workflow_expiration(now, 86400)
        identity = WorkflowIdentity(
            thread_id=final_thread_id,
            tenant_id=tenant_id,
            user_id=user_id,
            created_at=now,
            updated_at=now,
            status=status,
            expires_at=expires_at,
        )
        self._connection.execute(
            """
            INSERT INTO workflows (
                thread_id, tenant_id, user_id, created_at, updated_at, status, expires_at,
                workflow_json, checkpoint_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                identity.thread_id,
                identity.tenant_id,
                identity.user_id,
                identity.created_at.isoformat(),
                identity.updated_at.isoformat(),
                identity.status,
                identity.expires_at.isoformat(),
                json.dumps(workflow_json or {}, sort_keys=True),
                json.dumps({}, sort_keys=True),
            ),
        )
        return identity

    def get_by_thread_id(self, thread_id: str) -> WorkflowIdentity | None:
        row = self._connection.execute(
            "SELECT * FROM workflows WHERE thread_id = ?",
            (thread_id,),
        ).fetchone()
        if row is None:
            return None
        return WorkflowIdentity.model_validate({
            "thread_id": row["thread_id"],
            "tenant_id": row["tenant_id"],
            "user_id": row["user_id"],
            "created_at": row["created_at"],
            "updated_at": row["updated_at"],
            "status": row["status"],
            "expires_at": row["expires_at"],
        })

    def save_checkpoint(self, thread_id: str, checkpoint_json: dict[str, Any]) -> None:
        self._connection.execute(
            "UPDATE workflows SET checkpoint_json = ?, updated_at = ? WHERE thread_id = ?",
            (json.dumps(checkpoint_json, sort_keys=True), datetime.now(UTC).isoformat(), thread_id),
        )

    def get_checkpoint(self, thread_id: str) -> dict[str, Any]:
        row = self._connection.execute(
            "SELECT checkpoint_json FROM workflows WHERE thread_id = ?",
            (thread_id,),
        ).fetchone()
        if row is None:
            return {}
        try:
            return json.loads(row["checkpoint_json"]) if row["checkpoint_json"] else {}
        except json.JSONDecodeError:
            return {}

    def update_workflow_status(self, thread_id: str, *, status: str, expires_at: datetime | None = None) -> WorkflowIdentity | None:
        now = datetime.now(UTC)
        if expires_at is not None:
            self._connection.execute(
                "UPDATE workflows SET status = ?, expires_at = ?, updated_at = ? WHERE thread_id = ?",
                (status, expires_at.isoformat(), now.isoformat(), thread_id),
            )
        else:
            self._connection.execute(
                "UPDATE workflows SET status = ?, updated_at = ? WHERE thread_id = ?",
                (status, now.isoformat(), thread_id),
            )
        return self.get_by_thread_id(thread_id)

    def list_expired(self, now: datetime | None = None) -> list[str]:
        now_value = (now or datetime.now(UTC)).isoformat()
        rows = self._connection.execute(
            "SELECT thread_id FROM workflows WHERE status IN ('ACTIVE','PENDING_APPROVAL') AND expires_at <= ?",
            (now_value,),
        ).fetchall()
        return [row["thread_id"] for row in rows]

    def delete_completed(self, older_than: datetime) -> int:
        rows = self._connection.execute(
            "DELETE FROM workflows WHERE status = 'COMPLETED' AND expires_at <= ?",
            (older_than.isoformat(),),
        )
        return rows.rowcount or 0

    def cleanup_expired(self, *, now: datetime | None = None) -> dict[str, int]:
        now_value = (now or datetime.now(UTC)).isoformat()
        expired = self._connection.execute(
            "SELECT thread_id FROM workflows WHERE status IN ('ACTIVE','PENDING_APPROVAL') AND expires_at <= ?",
            (now_value,),
        ).fetchall()
        expired_ids = [row["thread_id"] for row in expired]
        if expired_ids:
            placeholders = ", ".join("?" for _ in expired_ids)
            self._connection.execute(
                f"UPDATE workflows SET status = 'EXPIRED', updated_at = ? WHERE thread_id IN ({placeholders})",
                [now_value, *expired_ids],
            )
        return {"expired_workflows": len(expired_ids)}

