from typing import Any, Literal

from pydantic import BaseModel, Field


class ResearchReport(BaseModel):
    symbol: str = Field(min_length=1)
    status: Literal[
        "completed",
        "partial",
        "failed",
    ]
    summary: str = Field(min_length=1)
    quote: dict[str, Any] | None = None
    news_count: int = Field(default=0, ge=0)
    tools_used: list[str] = Field(default_factory=list)
    error_code: str | None = None
    agent_used: bool = False

