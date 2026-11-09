from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.models.embedding import RetrievalMethod
from app.models.financial_document import DocumentType, FinancialCitation, FinancialDocumentChunk
from app.retrieval.retriever import RetrievedFinancialChunk


class FinancialDocumentSearchInput(BaseModel):
    model_config = ConfigDict(extra="forbid")

    symbol: str = Field(min_length=1, max_length=15)
    query: str = Field(min_length=1, max_length=500)
    maximum_results: int = Field(default=5, ge=1, le=10)
    allowed_document_types: list[str] | None = None

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        normalized = value.strip().upper()
        if not normalized:
            raise ValueError("symbol must not be blank")
        allowed = set("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-")
        if any(ch not in allowed for ch in normalized):
            raise ValueError("symbol contains unsupported characters")
        return normalized

    @field_validator("query")
    @classmethod
    def normalize_query(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("query must not be blank")
        return normalized

    @field_validator("allowed_document_types")
    @classmethod
    def validate_document_types(cls, value: list[str] | None) -> list[str] | None:
        if value is None:
            return None
        normalized: list[str] = []
        for item in value:
            if not isinstance(item, str):
                raise ValueError("allowed_document_types must contain strings")
            cleaned = item.strip().lower()
            if cleaned not in {member.value for member in DocumentType}:
                raise ValueError(f"unsupported document type: {item}")
            normalized.append(cleaned)
        return normalized


class FinancialDocumentSearchResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    symbol: str
    query: str
    results: list[RetrievedFinancialChunk]
    result_count: int
    evidence_sufficient: bool

    @field_validator("symbol")
    @classmethod
    def normalize_symbol(cls, value: str) -> str:
        return value.strip().upper()

    @field_validator("result_count")
    @classmethod
    def enforce_count(cls, value: int, info: Any) -> int:
        if value != len(info.data.get("results", [])):
            raise ValueError("result_count must match results length")
        return value

    @field_validator("results")
    @classmethod
    def validate_results(cls, value: list[RetrievedFinancialChunk]) -> list[RetrievedFinancialChunk]:
        if any(item.citation is None for item in value):
            raise ValueError("every result must include a citation")
        return value


class HybridRetrievedFinancialChunk(BaseModel):
    model_config = ConfigDict(extra="forbid")

    chunk: FinancialDocumentChunk
    citation: FinancialCitation
    retrieval_method: RetrievalMethod
    lexical_score: float | None = Field(default=None, ge=0.0, le=1.0)
    vector_score: float | None = Field(default=None, ge=0.0, le=1.0)
    lexical_rank: int | None = Field(default=None, ge=1)
    vector_rank: int | None = Field(default=None, ge=1)
    fused_score: float = Field(ge=0.0, le=1.0)
    matched_terms: list[str] = Field(default_factory=list)

    @property
    def score(self) -> float:
        return float(self.fused_score)

    @model_validator(mode="after")
    def validate_metadata(self) -> "HybridRetrievedFinancialChunk":
        if self.lexical_rank is None and self.vector_rank is None:
            raise ValueError("at least one source rank must be present")
        if self.citation.chunk_id != self.chunk.chunk_id:
            raise ValueError("citation must match the chunk")
        return self
