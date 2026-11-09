from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


class AuthenticatedPrincipal(BaseModel):
    model_config = ConfigDict(extra="forbid")

    user_id: str = Field(min_length=1, max_length=128)
    tenant_id: str = Field(min_length=1, max_length=128)
    scopes: set[str] = Field(default_factory=set)
    token_id: str | None = Field(default=None, max_length=128)
    expires_at: datetime

    @field_validator("user_id", "tenant_id")
    @classmethod
    def _non_blank(cls, value: str) -> str:
        if not value or not value.strip():
            raise ValueError("must not be blank")
        return value

    @field_validator("scopes")
    @classmethod
    def _normalize_scopes(cls, value: set[str]) -> set[str]:
        normalized: set[str] = set()
        for item in value:
            if not item or not item.strip():
                continue
            normalized.add(item.strip())
        return normalized

