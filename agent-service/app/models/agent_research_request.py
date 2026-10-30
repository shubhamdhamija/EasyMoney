from pydantic import BaseModel, Field, field_validator


class AgentResearchRequest(BaseModel):
    symbol: str = Field(min_length=1, max_length=15)
    maximum_news_items: int = Field(default=3, ge=1, le=5)
    maximum_tool_calls: int = Field(default=5, ge=1, le=10)
    maximum_decisions: int = Field(default=8, ge=1, le=20)

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        normalized = value.strip().upper()

        if not normalized:
            raise ValueError("symbol must not be blank")

        allowed_characters = set("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-")

        if any(character not in allowed_characters for character in normalized):
            raise ValueError("symbol contains unsupported characters")

        return normalized

