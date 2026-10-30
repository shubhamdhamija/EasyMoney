from datetime import datetime

from pydantic import BaseModel, Field, field_validator


class StockQuoteInput(BaseModel):
    symbol: str = Field(
        min_length=1,
        max_length=15,
        description="Stock symbol to retrieve.",
    )

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


class StockQuoteResult(BaseModel):
    symbol: str
    current_price: float = Field(ge=0)
    price_change_percent: float
    currency: str = Field(min_length=3, max_length=3)
    data_timestamp: datetime

