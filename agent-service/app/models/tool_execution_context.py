from pydantic import BaseModel, ConfigDict, Field, field_validator


class ToolExecutionContext(BaseModel):
    model_config = ConfigDict(extra="forbid")

    approval_granted: bool = False
    approval_id: str | None = Field(default=None, min_length=1, max_length=64)
    approved_tool_name: str | None = None
    approved_arguments_hash: str | None = None

    @field_validator("approval_id")
    @classmethod
    def _clean_approval_id(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("approval_id must not be blank")
        return normalized

    @field_validator("approved_tool_name")
    @classmethod
    def _clean_tool_name(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("approved_tool_name must not be blank")
        return normalized

    @field_validator("approved_arguments_hash")
    @classmethod
    def _clean_hash(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if not normalized:
            raise ValueError("approved_arguments_hash must not be blank")
        return normalized
