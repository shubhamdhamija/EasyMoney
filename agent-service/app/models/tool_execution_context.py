from pydantic import BaseModel, ConfigDict, Field, field_validator


class ToolExecutionContext(BaseModel):
    model_config = ConfigDict(extra="forbid")

    approval_granted: bool = False
    approval_id: str | None = Field(default=None, min_length=1, max_length=64)
    approved_tool_name: str | None = None
    approved_arguments_hash: str | None = None

    # Application-owned portfolio identity. Populated only by application code from
    # the verified JWT principal and a verified consent. Never read from model
    # tool arguments (tool input models forbid unknown fields), so a model-supplied
    # "portfolio_consent_granted" has no effect.
    authenticated: bool = False
    tenant_id: str | None = Field(default=None, max_length=128)
    user_id: str | None = Field(default=None, max_length=128)
    request_id: str | None = Field(default=None, max_length=128)
    selected_portfolio_id: str | None = Field(default=None, max_length=64)
    portfolio_consent_id: str | None = Field(default=None, max_length=64)
    portfolio_consent_granted: bool = False
    selected_account_ids: list[str] = Field(default_factory=list, max_length=20)
    maximum_positions: int = Field(default=20, ge=1, le=50)
    include_positions: bool = True
    include_cash: bool = True
    include_book_value: bool = False

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
