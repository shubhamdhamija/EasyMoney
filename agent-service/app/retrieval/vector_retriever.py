from __future__ import annotations

from app.models.financial_document import FinancialDocument, FinancialDocumentChunk, deterministic_citation_id
from app.retrieval.document_chunker import DocumentChunker
from app.retrieval.embedding_model import EmbeddingModel
from app.retrieval.fake_embedding_model import DeterministicFakeEmbeddingModel
from app.retrieval.retriever import RetrievedFinancialChunk
from app.retrieval.vector_index import InMemoryFinancialDocumentVectorIndex
from app.retrieval.vector_math import normalized_similarity


class VectorFinancialDocumentRetriever:
    def __init__(
        self,
        *,
        documents: list[FinancialDocument] | None = None,
        chunker: DocumentChunker | None = None,
        embedding_model: EmbeddingModel | None = None,
        index: InMemoryFinancialDocumentVectorIndex | None = None,
        minimum_vector_score: float = 0.15,
    ) -> None:
        self._documents = list(documents or [])
        self._chunker = chunker or DocumentChunker()
        self._embedding_model = embedding_model or DeterministicFakeEmbeddingModel(dimensions=64)
        self._index = index or InMemoryFinancialDocumentVectorIndex()
        self._minimum_vector_score = float(minimum_vector_score)
        self.search_calls: list[str] = []

    async def build_index(self, documents: list[FinancialDocument]) -> None:
        if not isinstance(documents, list):
            raise ValueError("documents must be a list")
        self._documents = list(documents)
        chunks = []
        for document in documents:
            for chunk in self._chunker.chunk_document(document):
                embedding = await self._embedding_model.embed_text(chunk.text)
                chunks.append(type("EmbeddedChunk", (), {"chunk": chunk, "embedding": embedding})())
        indexed: list[object] = []
        for item in chunks:
            from app.models.embedding import EmbeddedFinancialChunk
            indexed.append(EmbeddedFinancialChunk(chunk=item.chunk, embedding=item.embedding))
        self._index.clear()
        self._index.replace(indexed)

    @staticmethod
    def _build_citation(chunk: FinancialDocumentChunk):
        from app.models.financial_document import FinancialCitation

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
        if not isinstance(symbol, str) or not symbol.strip():
            raise ValueError("symbol must not be blank")
        if not isinstance(query, str) or not query.strip():
            raise ValueError("query must not be blank")

        normalized_symbol = symbol.strip().upper()
        maximum_results = max(1, min(int(maximum_results), 10))
        self.search_calls.append(f"{normalized_symbol}:{query}")

        query_embedding = await self._embedding_model.embed_text(query)
        allowed = None if allowed_document_types is None else {item.strip().lower() for item in allowed_document_types if item and item.strip()}

        candidates = self._index.search(
            query_embedding=query_embedding.values,
            symbol=normalized_symbol,
            maximum_results=maximum_results,
            allowed_document_types=list(allowed) if allowed is not None else None,
        )

        results: list[RetrievedFinancialChunk] = []
        for embedded, raw_score in candidates:
            score = normalized_similarity(raw_score)
            if score < self._minimum_vector_score:
                continue
            citation = self._build_citation(embedded.chunk)
            results.append(
                RetrievedFinancialChunk(
                    chunk=embedded.chunk,
                    score=score,
                    matched_terms=[part.lower() for part in query.split() if part.strip()],
                    citation=citation,
                )
            )
        return results[:maximum_results]

