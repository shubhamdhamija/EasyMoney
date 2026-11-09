from __future__ import annotations

import uuid
from datetime import datetime, timedelta, timezone

from pydantic import BaseModel, Field, field_validator

_ALLOWED_STATUSES = {
    "ACTIVE",
    "PENDING_APPROVAL",
    "COMPLETED",
    "FAILED",
    "CANCELLED",
    "EXPIRED",
}


class WorkflowIdentity(BaseModel):
    thread_id: str = Field(min_length=1, max_length=128)
    tenant_id: str = Field(min_length=1, max_length=128)
    user_id: str = Field(min_length=1, max_length=128)
    created_at: datetime
    updated_at: datetime
    status: str = Field(min_length=1, max_length=32)
    expires_at: datetime

    @field_validator("thread_id", "tenant_id", "user_id")
    @classmethod
    def _non_blank(cls, value: str) -> str:
        if not value or not value.strip():
            raise ValueError("must not be blank")
        return value

    @field_validator("status")
    @classmethod
    def _status(cls, value: str) -> str:
        if value not in _ALLOWED_STATUSES:
            raise ValueError(f"unknown workflow status: {value}")
        return value


def generate_thread_id() -> str:
    return str(uuid.uuid4())


def workflow_expiration(now: datetime, ttl_seconds: int) -> datetime:
    return now.astimezone(timezone.utc) + timedelta(seconds=ttl_seconds)

