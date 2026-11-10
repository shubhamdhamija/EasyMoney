from typing import Any, Literal

from pydantic import BaseModel, Field, model_serializer

from app.models.financial_document import FinancialCitation

_PORTFOLIO_FIELDS = (
    "portfolio_snapshot_at",
    "portfolio_source",
    "portfolio_currency_summaries",
    "portfolio_positions",
    "portfolio_concentration",
    "portfolio_warnings",
    "portfolio_disclaimer",
)


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
    evidence_summary: list[str] = Field(default_factory=list)
    citations: list[FinancialCitation] = Field(default_factory=list)
    # Headlines of the news articles that were actually retrieved (max 5).
    headlines: list[dict[str, Any]] = Field(default_factory=list)

    # Portfolio research (optional, backward compatible). Built only from
    # tool-returned data: safe display fields, no provider identifiers, no
    # account numbers, no consent internals, no credentials.
    portfolio_snapshot_at: str | None = None
    portfolio_source: str | None = None
    portfolio_currency_summaries: list[dict[str, Any]] = Field(default_factory=list)
    portfolio_positions: list[dict[str, Any]] = Field(default_factory=list)
    portfolio_concentration: list[dict[str, Any]] = Field(default_factory=list)
    portfolio_warnings: list[dict[str, Any]] = Field(default_factory=list)
    portfolio_disclaimer: str | None = None

    @model_serializer(mode="wrap")
    def _omit_empty_portfolio_fields(self, handler):
        """Stock reports keep their original shape: portfolio keys appear only
        for portfolio research."""
        data = handler(self)
        if self.portfolio_source is None and self.portfolio_disclaimer is None:
            for key in _PORTFOLIO_FIELDS:
                data.pop(key, None)
        return data
