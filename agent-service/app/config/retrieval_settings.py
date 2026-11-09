from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, field_validator


class RetrievalMode(str, Enum):
    lexical = "lexical"
    vector = "vector"
    hybrid = "hybrid"


class RetrievalSettings(BaseModel):
    model_config = ConfigDict(extra="ignore")

    document_retrieval_mode: RetrievalMode = RetrievalMode.lexical
    embedding_model_mode: str = "fake"
    fake_embedding_dimensions: int = Field(default=64, ge=16, le=512)
    vector_minimum_score: float = Field(default=0.15, ge=0.0, le=1.0)
    hybrid_lexical_weight: float = Field(default=1.0, ge=0.0, le=10.0)
    hybrid_vector_weight: float = Field(default=1.0, ge=0.0, le=10.0)
    hybrid_rank_constant: int = Field(default=60, gt=0, le=1000)

    @field_validator("embedding_model_mode")
    @classmethod
    def validate_embedding_model_mode(cls, value: str) -> str:
        if value.strip().lower() != "fake":
            raise ValueError("only the fake embedding model is supported in this milestone")
        return value.strip().lower()

