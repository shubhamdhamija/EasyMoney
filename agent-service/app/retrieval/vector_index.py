from __future__ import annotations

from typing import Protocol

from app.models.embedding import EmbeddedFinancialChunk


class FinancialDocumentVectorIndex(Protocol):
    def replace(self, chunks: list[EmbeddedFinancialChunk]) -> None: ...
    def search(
        self,
        *,
        query_embedding: list[float],
        symbol: str,
        maximum_results: int,
        allowed_document_types: list[str] | None = None,
    ) -> list[tuple[EmbeddedFinancialChunk, float]]: ...
    def clear(self) -> None: ...
    def count(self) -> int: ...


class InMemoryFinancialDocumentVectorIndex:
    def __init__(self) -> None:
        self._chunks: list[EmbeddedFinancialChunk] = []
        self._dimension: int | None = None

    def replace(self, chunks: list[EmbeddedFinancialChunk]) -> None:
        if not isinstance(chunks, list):
            raise ValueError("chunks must be a list")
        validated: list[EmbeddedFinancialChunk] = []
        batch_dimensions: set[int] = set()
        for chunk in chunks:
            if chunk.embedding.dimensions != len(chunk.embedding.values):
                raise ValueError("embedding dimension mismatch")
            batch_dimensions.add(chunk.embedding.dimensions)
            validated.append(chunk.model_copy(deep=True))
        if len(batch_dimensions) > 1:
            raise ValueError("mixed embedding dimensions are not allowed")
        if self._dimension is not None and batch_dimensions and self._dimension != next(iter(batch_dimensions)):
            raise ValueError("query embedding dimension mismatch")
        if batch_dimensions:
            self._dimension = next(iter(batch_dimensions))
        self._chunks = validated

    def search(
        self,
        *,
        query_embedding: list[float],
        symbol: str,
        maximum_results: int,
        allowed_document_types: list[str] | None = None,
    ) -> list[tuple[EmbeddedFinancialChunk, float]]:
        if not isinstance(query_embedding, list) or not query_embedding:
            raise ValueError("query_embedding must be a non-empty list")
        if not 1 <= int(maximum_results) <= 10:
            raise ValueError("maximum_results must be between 1 and 10")
        maximum_results = int(maximum_results)
        if self._dimension is not None and len(query_embedding) != self._dimension:
            raise ValueError("query embedding dimension mismatch")
        normalized_symbol = symbol.strip().upper()
        if not normalized_symbol:
            raise ValueError("symbol must not be blank")

        allowed = None
        if allowed_document_types is not None:
            allowed = {item.strip().lower() for item in allowed_document_types if item and item.strip()}

        scored: list[tuple[EmbeddedFinancialChunk, float]] = []
        for item in self._chunks:
            if item.chunk.symbol != normalized_symbol:
                continue
            if allowed is not None and item.chunk.document_type.value not in allowed:
                continue
            score = 0.0
            if item.embedding.values:
                dot = sum(a * b for a, b in zip(query_embedding, item.embedding.values))
                norm_query = sum(value * value for value in query_embedding) ** 0.5
                norm_item = sum(value * value for value in item.embedding.values) ** 0.5
                if norm_query > 0.0 and norm_item > 0.0:
                    score = max(0.0, min(1.0, dot / (norm_query * norm_item)))
            scored.append((item.model_copy(deep=True), score))

        scored.sort(
            key=lambda pair: (
                -pair[1],
                pair[0].chunk.document_id,
                pair[0].chunk.chunk_index,
            )
        )
        return scored[:maximum_results]

    def clear(self) -> None:
        self._chunks.clear()
        self._dimension = None

    def count(self) -> int:
        return len(self._chunks)
