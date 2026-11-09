SCHEMA_VERSION = 1

SCHEMA_SQL = """
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA busy_timeout = 5000;

CREATE TABLE IF NOT EXISTS schema_metadata (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

INSERT OR IGNORE INTO schema_metadata (key, value) VALUES ('schema_version', '1');

CREATE TABLE IF NOT EXISTS workflows (
    thread_id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    status TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    workflow_json TEXT NOT NULL,
    checkpoint_json TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS approvals (
    approval_id TEXT PRIMARY KEY,
    thread_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    prepared_arguments_hash TEXT NOT NULL,
    safe_arguments_json TEXT NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    decided_at TEXT,
    consumed_at TEXT,
    reviewer_note TEXT,
    approval_json TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id TEXT PRIMARY KEY,
    event_type TEXT NOT NULL,
    occurred_at TEXT NOT NULL,
    request_id TEXT,
    thread_id TEXT,
    approval_id TEXT,
    tenant_id TEXT,
    user_id TEXT,
    tool_name TEXT,
    outcome TEXT NOT NULL,
    safe_error_code TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_workflows_owner ON workflows (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_workflows_status ON workflows (status);
CREATE INDEX IF NOT EXISTS idx_approvals_thread ON approvals (thread_id, status);
CREATE INDEX IF NOT EXISTS idx_approvals_owner ON approvals (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_occured ON audit_events (occurred_at);
"""

