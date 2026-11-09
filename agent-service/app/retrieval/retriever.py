from __future__ import annotations

import re
from typing import Any, Protocol

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.models.financial_document import FinancialCitation, FinancialDocument, FinancialDocumentChunk, deterministic_citation_id
from app.retrieval.document_chunker import DocumentChunker


STOP_WORDS = {
    "the", "a", "an", "and", "or", "of", "to", "in", "for", "on", "with", "as", "by",
    "from", "at", "it", "is", "was", "are", "be", "this", "that", "these", "those",
    "we", "our", "their", "they", "his", "her", "its", "if", "then", "than", "into",
    "over", "under", "after", "before", "during", "about", "through", "because", "while",
}


class RetrievedFinancialChunk(BaseModel):
    model_config = ConfigDict(extra="forbid")

    chunk: FinancialDocumentChunk
    score: float = Field(ge=0.0, le=1.0)
    matched_terms: list[str] = Field(default_factory=list)
    citation: Any = Field(...)

    @field_validator("citation", mode="before")
    @classmethod
    def coerce_citation(cls, value: Any) -> Any:
        if value is None:
            raise ValueError("citation is required")
        if isinstance(value, FinancialCitation):
            return value
        if isinstance(value, dict):
            return FinancialCitation.model_validate(value)
        if hasattr(value, "chunk_id") and hasattr(value, "document_id") and hasattr(value, "symbol"):
            payload = {
                "citation_id": getattr(value, "citation_id", None) or f"cite_{getattr(value, 'chunk_id', 'unknown')}",
                "chunk_id": value.chunk_id,
                "document_id": value.document_id,
                "symbol": value.symbol,
                "title": value.title,
                "document_type": value.document_type,
                "section_name": getattr(value, "section_name", "General"),
                "published_at": value.published_at,
                "source_name": value.source_name,
                "source_url": getattr(value, "source_url", None),
                "accession_number": getattr(value, "accession_number", None),
            }
            return FinancialCitation.model_validate(payload)
        raise ValueError("citation must be a FinancialCitation or a citation-like object")


class FinancialDocumentRetriever(Protocol):
    async def search(
        self,
        *,
        symbol: str,
        query: str,
        maximum_results: int,
        allowed_document_types: list[str] | None = None,
    ) -> list[RetrievedFinancialChunk]: ...


class LexicalFinancialDocumentRetriever:
    def __init__(self, *, documents: list[FinancialDocument] | None = None, chunker: DocumentChunker | None = None) -> None:
        self._documents = list(documents or [])
        self._chunker = chunker or DocumentChunker()

    def set_documents(self, documents: list[FinancialDocument]) -> None:
        self._documents = list(documents)

    def _allowed_document_types(self, allowed_document_types: list[str] | None) -> set[str] | None:
        if allowed_document_types is None:
            return None
        return {item.strip().lower() for item in allowed_document_types if item and item.strip()}

    def _normalize_query(self, text: str) -> str:
        return re.sub(r"[^a-z0-9\s]", " ", text.lower())

    def _tokenize(self, text: str) -> list[str]:
        tokens = self._normalize_query(text).split()
        return [token for token in tokens if token and token not in STOP_WORDS]

    def _phrase_match(self, text: str, query: str) -> bool:
        normalized_text = self._normalize_query(text)
        normalized_query = self._normalize_query(query)
        return normalized_query in normalized_text

    def _build_citation(self, chunk: FinancialDocumentChunk) -> FinancialCitation:
        return FinancialCitation(
            citation_id=deterministic_citation_id(
                document_id=chunk.document_id,
                section_name=chunk.section_name,
                chunk_index=chunk.chunk_index,
                symbol=chunk.symbol,
            ),
            chunk_id=chunk.chunk_id,
            document_id=chunk.document_id,
            symbol=chunk.symbol,
            title=chunk.title,
            document_type=chunk.document_type,
            section_name=chunk.section_name,
            published_at=chunk.published_at,
            source_name=chunk.source_name,
            source_url=chunk.source_url,
            accession_number=chunk.accession_number,
        )

    async def search(
        self,
        *,
        symbol: str,
        query: str,
        maximum_results: int,
        allowed_document_types: list[str] | None = None,
    ) -> list[RetrievedFinancialChunk]:
        normalized_symbol = symbol.strip().upper()
        maximum_results = max(1, min(int(maximum_results), 10))
        allowed = self._allowed_document_types(allowed_document_types)
        q_tokens = self._tokenize(query)
        if not q_tokens:
            return []

        results: list[RetrievedFinancialChunk] = []
        for document in self._documents:
            if document.symbol != normalized_symbol:
                continue
            if allowed is not None and document.document_type.value not in allowed:
                continue
            for chunk in self._chunker.chunk_document(document):
                combined = f"{document.title} {chunk.section_name} {chunk.text}".lower()
                matched = []
                for term in q_tokens:
                    if term in combined:
                        matched.append(term)
                if not matched:
                    continue
                title_tokens = self._tokenize(document.title)
                section_tokens = self._tokenize(chunk.section_name)
                score = 0.0
                coverage = len(set(matched)) / max(1, len(set(q_tokens)))
                score += coverage * 0.55
                score += (len(set(title_tokens) & set(q_tokens)) / max(1, len(set(q_tokens)))) * 0.20
                score += (len(set(section_tokens) & set(q_tokens)) / max(1, len(set(q_tokens)))) * 0.15
                if self._phrase_match(chunk.text, query) or self._phrase_match(document.title, query):
                    score += 0.10
                score = min(1.0, max(0.0, score))
                if score <= 0.0:
                    continue
                citation = self._build_citation(chunk)
                results.append(
                    RetrievedFinancialChunk(
                        chunk=chunk,
                        score=score,
                        matched_terms=sorted(set(matched)),
                        citation=citation,
                    )
                )

        results.sort(key=lambda item: (-item.score, -item.chunk.published_at.timestamp(), item.chunk.document_id))
        return results[:maximum_results]
