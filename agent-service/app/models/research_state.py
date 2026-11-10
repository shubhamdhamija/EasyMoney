from typing import Any, Literal

from pydantic import BaseModel, Field

from app.models.financial_document_search import FinancialDocumentSearchResult


class ResearchStep(BaseModel):
    tool_name: str = Field(min_length=1)
    status: Literal[
        "planned",
        "completed",
        "skipped",
        "failed",
        "rejected",
    ]


class ResearchError(BaseModel):
    code: str = Field(min_length=1)
    message: str = Field(min_length=1)


class ResearchState(BaseModel):
    symbol: str = Field(min_length=1)
    steps: list[ResearchStep] = Field(default_factory=list)
    quote: dict[str, Any] | None = None
    news: dict[str, Any] | None = None
    document_results: FinancialDocumentSearchResult | None = None
    document_search_queries: list[str] = Field(default_factory=list)
    error: ResearchError | None = None

    agent_used: bool = False

    tool_call_count: int = Field(default=0, ge=0)
    maximum_tool_calls: int = Field(default=5, ge=1, le=10)

    decision_count: int = Field(default=0, ge=0)
    maximum_decisions: int = Field(default=8, ge=1, le=20)

    # Portfolio research (all optional, backward compatible). Only opaque IDs and
    # bounded tool results: never credentials, provider connection IDs, raw
    # account numbers, account hashes, raw CSV, file URIs, device IDs, JWTs or raw
    # provider responses. Full holdings beyond the bounded tool results are never kept.
    selected_portfolio_id: str | None = Field(default=None, max_length=64)
    portfolio_source: str | None = Field(default=None, max_length=32)
    portfolio_snapshot_at: str | None = Field(default=None, max_length=40)
    portfolio_consent_id: str | None = Field(default=None, max_length=64)
    portfolio_summary: dict[str, Any] | None = None
    portfolio_position_results: list[dict[str, Any]] = Field(default_factory=list, max_length=10)
    portfolio_concentration_results: list[dict[str, Any]] = Field(default_factory=list, max_length=5)
    # Hashes of completed portfolio tool requests (duplicate protection across resume).
    portfolio_tool_requests: list[str] = Field(default_factory=list, max_length=20)

    @property
    def portfolio_mode(self) -> bool:
        return self.selected_portfolio_id is not None

    @property
    def ok(self) -> bool:
        return self.error is None

    def __getitem__(self, key: str) -> Any:
        if key == "ok":
            return self.ok
        if key == "symbol":
            return self.symbol
        if key == "steps":
            return [step.model_dump(mode="json") for step in self.steps]
        if key == "quote":
            return self.quote
        if key == "news":
            return self.news
        if key == "document_results":
            return None if self.document_results is None else self.document_results.model_dump(mode="json")
        if key == "document_search_queries":
            return list(self.document_search_queries)
        if key == "error":
            return None if self.error is None else self.error.model_dump(mode="json")
        if key == "agent_used":
            return self.agent_used
        raise KeyError(key)
