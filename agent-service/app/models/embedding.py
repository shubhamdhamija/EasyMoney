from __future__ import annotations

import math
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.models.financial_document import FinancialCitation, FinancialDocumentChunk

RetrievalMethod = Literal["lexical", "vector", "hybrid"]


class EmbeddingVector(BaseModel):
    model_config = ConfigDict(extra="forbid")

    values: list[float] = Field(min_length=1)
    dimensions: int = Field(ge=1, le=4096)
    model_name: str = Field(min_length=1, max_length=200)
    model_version: str = Field(min_length=1, max_length=100)

    @field_validator("values")
    @classmethod
    def validate_values(cls, value: list[float]) -> list[float]:
        if not value:
            raise ValueError("embedding values must not be empty")
        if any(not math.isfinite(item) for item in value):
            raise ValueError("embedding values must be finite")
        return value

    @field_validator("dimensions")
    @classmethod
    def validate_dimensions(cls, value: int, info: object) -> int:
        values = getattr(info, "data", {}).get("values")
        if values is not None and len(values) != value:
            raise ValueError("dimensions must equal len(values)")
        return value

    @field_validator("model_name", "model_version")
    @classmethod
    def validate_metadata(cls, value: str, info: object) -> str:
        cleaned = value.strip()
        if not cleaned:
            raise ValueError(f"{info.field_name} must not be blank")
        return cleaned


class EmbeddedFinancialChunk(BaseModel):
    model_config = ConfigDict(extra="forbid")

    chunk: FinancialDocumentChunk
    embedding: EmbeddingVector


class VectorSearchResult(BaseModel):
    model_config = ConfigDict(extra="forbid")

    chunk: FinancialDocumentChunk
    score: float = Field(ge=0.0, le=1.0)
    citation: FinancialCitation
