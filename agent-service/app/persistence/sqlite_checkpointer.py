from __future__ import annotations

import json
import sqlite3
import uuid
from collections.abc import Sequence
from datetime import datetime, timezone
from typing import Any

from langgraph.checkpoint.base import BaseCheckpointSaver, Checkpoint, CheckpointMetadata, ChannelVersions, RunnableConfig


class SQLiteCheckpointSaver(BaseCheckpointSaver[str]):
    def __init__(self, path: str, *, serde: Any | None = None):
        super().__init__(serde=serde)
        self.path = path
        self._conn = sqlite3.connect(path, timeout=30, isolation_level=None)
        self._conn.row_factory = sqlite3.Row
        self._conn.execute("PRAGMA foreign_keys = ON")
        self._conn.execute("PRAGMA journal_mode = WAL")
        self._conn.execute("PRAGMA busy_timeout = 5000")
        self._conn.execute(
            """
            CREATE TABLE IF NOT EXISTS checkpoints (
                thread_id TEXT NOT NULL,
                checkpoint_ns TEXT NOT NULL DEFAULT '',
                checkpoint_id TEXT NOT NULL,
                parent_id TEXT,
                checkpoint_json TEXT NOT NULL,
                metadata_json TEXT NOT NULL,
                PRIMARY KEY (thread_id, checkpoint_ns, checkpoint_id)
            )
            """
        )
        self._conn.execute(
            """
            CREATE TABLE IF NOT EXISTS checkpoint_writes (
                thread_id TEXT NOT NULL,
                checkpoint_ns TEXT NOT NULL DEFAULT '',
                checkpoint_id TEXT NOT NULL,
                task_id TEXT NOT NULL,
                task_path TEXT NOT NULL DEFAULT '',
                channel TEXT NOT NULL,
                value_json TEXT NOT NULL,
                version TEXT NOT NULL,
                created_at TEXT NOT NULL,
                PRIMARY KEY (thread_id, checkpoint_ns, checkpoint_id, task_id, channel)
            )
            """
        )

    def _config_key(self, config: RunnableConfig) -> tuple[str, str]:
        configurable = config.get("configurable", {})
        thread_id = str(configurable.get("thread_id", ""))
        checkpoint_ns = str(configurable.get("checkpoint_ns", ""))
        return thread_id, checkpoint_ns

    def _row_to_checkpoint(self, row: sqlite3.Row) -> CheckpointTuple:
        checkpoint = json.loads(row["checkpoint_json"])
        metadata = json.loads(row["metadata_json"])
        config = {"configurable": {"thread_id": row["thread_id"], "checkpoint_ns": row["checkpoint_ns"]}}
        parent_config = None
        if row["parent_id"]:
            parent_config = {"configurable": {"thread_id": row["thread_id"], "checkpoint_ns": row["checkpoint_ns"], "checkpoint_id": row["parent_id"]}}
        pending_writes = []
        writes = self._conn.execute(
            "SELECT channel, value_json, version, task_id, task_path FROM checkpoint_writes WHERE thread_id = ? AND checkpoint_ns = ? AND checkpoint_id = ? ORDER BY created_at",
            (row["thread_id"], row["checkpoint_ns"], row["checkpoint_id"]),
        ).fetchall()
        for write in writes:
            pending_writes.append((write["channel"], self.serde.loads_typed(("json", write["value_json"].encode('utf-8')))))
        return CheckpointTuple(config=config, checkpoint=checkpoint, metadata=metadata, parent_config=parent_config, pending_writes=pending_writes)

    def get_tuple(self, config: RunnableConfig) -> CheckpointTuple | None:
        thread_id, checkpoint_ns = self._config_key(config)
        checkpoint_id = config.get("configurable", {}).get("checkpoint_id")
        if checkpoint_id is None:
            row = self._conn.execute(
                "SELECT * FROM checkpoints WHERE thread_id = ? AND checkpoint_ns = ? ORDER BY checkpoint_id DESC LIMIT 1",
                (thread_id, checkpoint_ns),
            ).fetchone()
            if row is None:
                return None
            return self._row_to_checkpoint(row)
        row = self._conn.execute(
            "SELECT * FROM checkpoints WHERE thread_id = ? AND checkpoint_ns = ? AND checkpoint_id = ?",
            (thread_id, checkpoint_ns, checkpoint_id),
        ).fetchone()
        return self._row_to_checkpoint(row) if row is not None else None

    async def aget_tuple(self, config: RunnableConfig) -> CheckpointTuple | None:
        return self.get_tuple(config)

    def put(self, config: RunnableConfig, checkpoint: Checkpoint, metadata: CheckpointMetadata, new_versions: ChannelVersions) -> RunnableConfig:
        thread_id, checkpoint_ns = self._config_key(config)
        checkpoint_id = str(uuid.uuid4())
        parent_id = config.get("configurable", {}).get("checkpoint_id")
        self._conn.execute(
            "INSERT INTO checkpoints (thread_id, checkpoint_ns, checkpoint_id, parent_id, checkpoint_json, metadata_json) VALUES (?, ?, ?, ?, ?, ?)",
            (
                thread_id,
                checkpoint_ns,
                checkpoint_id,
                parent_id,
                json.dumps(checkpoint, sort_keys=True),
                json.dumps(metadata, sort_keys=True),
            ),
        )
        return {**config, "configurable": {**config.get("configurable", {}), "checkpoint_id": checkpoint_id}}

    async def aput(self, config: RunnableConfig, checkpoint: Checkpoint, metadata: CheckpointMetadata, new_versions: ChannelVersions) -> RunnableConfig:
        return self.put(config, checkpoint, metadata, new_versions)

    def put_writes(self, config: RunnableConfig, writes: Sequence[tuple[str, Any]], task_id: str, task_path: str = "") -> None:
        thread_id, checkpoint_ns = self._config_key(config)
        checkpoint_id = config.get("configurable", {}).get("checkpoint_id")
        if not checkpoint_id:
            raise ValueError("checkpoint_id required")
        for channel, value in writes:
            self._conn.execute(
                "INSERT INTO checkpoint_writes (thread_id, checkpoint_ns, checkpoint_id, task_id, task_path, channel, value_json, version, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    thread_id,
                    checkpoint_ns,
                    checkpoint_id,
                    task_id,
                    task_path,
                    channel,
                    json.dumps(value, sort_keys=True, default=str),
                    str(uuid.uuid4()),
                    datetime.now(timezone.utc).isoformat(),
                ),
            )

    async def aput_writes(self, config: RunnableConfig, writes: Sequence[tuple[str, Any]], task_id: str, task_path: str = "") -> None:
        self.put_writes(config, writes, task_id, task_path)

    def list(self, config: RunnableConfig | None, *, filter: dict[str, Any] | None = None, before: RunnableConfig | None = None, limit: int | None = None):
        query = "SELECT * FROM checkpoints"
        params: list[Any] = []
        if config is not None:
            thread_id = config.get("configurable", {}).get("thread_id")
            if thread_id is not None:
                query += " WHERE thread_id = ?"
                params.append(thread_id)
        rows = self._conn.execute(query, params).fetchall()
        for row in rows[: limit if limit else len(rows)]:
            yield self._row_to_checkpoint(row)

    async def alist(self, config: RunnableConfig | None, *, filter: dict[str, Any] | None = None, before: RunnableConfig | None = None, limit: int | None = None):
        for item in self.list(config, filter=filter, before=before, limit=limit):
            yield item

    def delete_thread(self, thread_id: str) -> None:
        self._conn.execute("DELETE FROM checkpoint_writes WHERE thread_id = ?", (thread_id,))
        self._conn.execute("DELETE FROM checkpoints WHERE thread_id = ?", (thread_id,))

    def copy_thread(self, source_thread_id: str, target_thread_id: str) -> None:
        rows = self._conn.execute("SELECT * FROM checkpoints WHERE thread_id = ?", (source_thread_id,)).fetchall()
        for row in rows:
            self._conn.execute(
                "INSERT OR REPLACE INTO checkpoints (thread_id, checkpoint_ns, checkpoint_id, parent_id, checkpoint_json, metadata_json) VALUES (?, ?, ?, ?, ?, ?)",
                (target_thread_id, row["checkpoint_ns"], row["checkpoint_id"], row["parent_id"], row["checkpoint_json"], row["metadata_json"]),
            )
        writes = self._conn.execute("SELECT * FROM checkpoint_writes WHERE thread_id = ?", (source_thread_id,)).fetchall()
        for row in writes:
            self._conn.execute(
                "INSERT OR REPLACE INTO checkpoint_writes (thread_id, checkpoint_ns, checkpoint_id, task_id, task_path, channel, value_json, version, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (target_thread_id, row["checkpoint_ns"], row["checkpoint_id"], row["task_id"], row["task_path"], row["channel"], row["value_json"], row["version"], row["created_at"]),
            )

    def prune(self, thread_ids: Sequence[str], *, strategy: str = "keep_latest") -> None:
        for thread_id in thread_ids:
            self.delete_thread(thread_id)

