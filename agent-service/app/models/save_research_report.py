import re

from pydantic import BaseModel, Field, field_validator

_REQUEST_ID_PATTERN = re.compile(r"^[A-Za-z0-9._-]{1,64}$")


class SaveResearchReportInput(BaseModel):
    symbol: str = Field(min_length=1, max_length=15)
    report_summary: str = Field(min_length=1, max_length=2000)
    request_id: str | None = Field(default=None, max_length=64)

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        normalized = value.strip().upper()
        if not normalized:
            raise ValueError("symbol must not be blank")
        if len(normalized) > 15:
            raise ValueError("symbol length must be between 1 and 15 characters")
        allowed_characters = set("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-")
        if any(character not in allowed_characters for character in normalized):
            raise ValueError("symbol contains unsupported characters")
        return normalized

    @field_validator("report_summary")
    @classmethod
    def normalize_summary(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("report_summary must not be blank")
        return normalized

    @field_validator("request_id")
    @classmethod
    def normalize_request_id(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("request_id must not be blank")
        if not _REQUEST_ID_PATTERN.fullmatch(normalized):
            raise ValueError("request_id contains unsupported characters")
        return normalized


class SaveResearchReportResult(BaseModel):
    symbol: str
    saved: bool
    record_id: str = Field(min_length=1, max_length=64)

    @field_validator("symbol")
    @classmethod
    def validate_symbol(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("symbol must not be blank")
        return normalized

    @field_validator("record_id")
    @classmethod
    def validate_record_id(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("record_id must not be blank")
        return normalized
