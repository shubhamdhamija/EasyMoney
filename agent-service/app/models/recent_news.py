from datetime import datetime

from pydantic import BaseModel, Field, field_validator


class RecentNewsInput(BaseModel):
    symbol: str = Field(min_length=1, max_length=15)
    maximum_items: int = Field(default=3, ge=1, le=5)

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


class NewsArticle(BaseModel):
    headline: str = Field(min_length=1, max_length=500)
    source: str = Field(min_length=1, max_length=200)
    published_at: datetime

    @field_validator("headline", "source")
    @classmethod
    def normalize_text(cls, value: str, info) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError(f"{info.field_name} must not be blank")
        return normalized


class RecentNewsResult(BaseModel):
    symbol: str
    articles: list[NewsArticle]
