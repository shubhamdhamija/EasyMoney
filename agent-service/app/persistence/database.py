from __future__ import annotations

import sqlite3
from pathlib import Path

from app.persistence.schema import SCHEMA_SQL, SCHEMA_VERSION

DEFAULT_DB_PATH = Path(__file__).resolve().parents[1] / "data" / "agent_runtime.db"


def ensure_parent_dir(path: str | Path) -> None:
    Path(path).parent.mkdir(parents=True, exist_ok=True)


def connect_database(path: str | Path | None = None) -> sqlite3.Connection:
    target = Path(path) if path is not None else DEFAULT_DB_PATH
    ensure_parent_dir(target)
    conn = sqlite3.connect(str(target), timeout=30, isolation_level=None)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("PRAGMA journal_mode = WAL")
    conn.execute("PRAGMA busy_timeout = 5000")
    return conn


def initialize_database(path: str | Path | None = None) -> sqlite3.Connection:
    conn = connect_database(path)
    conn.executescript(SCHEMA_SQL)
    current = conn.execute(
        "SELECT value FROM schema_metadata WHERE key = 'schema_version'"
    ).fetchone()
    if current is None or int(current["value"]) != SCHEMA_VERSION:
        raise RuntimeError("database schema is incompatible")
    return conn


def database_ready(path: str | Path | None = None) -> bool:
    try:
        conn = connect_database(path)
        conn.execute("SELECT 1").fetchone()
        conn.close()
        return True
    except Exception:
        return False

