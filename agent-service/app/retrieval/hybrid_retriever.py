from __future__ import annotations

from app.models.financial_document_search import HybridRetrievedFinancialChunk
from app.retrieval.rank_fusion import reciprocal_rank_fusion
from app.retrieval.retriever import FinancialDocumentRetriever, RetrievedFinancialChunk


class HybridFinancialDocumentRetriever:
    def __init__(
        self,
        *,
        lexical_retriever: FinancialDocumentRetriever,
        vector_retriever,
        lexical_candidate_count: int = 10,
        vector_candidate_count: int = 10,
        final_maximum_results: int = 5,
        lexical_weight: float = 1.0,
        vector_weight: float = 1.0,
        rank_constant: int = 60,
    ) -> None:
        self._lexical = lexical_retriever
        self._vector = vector_retriever
        self._lexical_candidate_count = max(1, int(lexical_candidate_count))
        self._vector_candidate_count = max(1, int(vector_candidate_count))
        self._final_maximum_results = max(1, min(int(final_maximum_results), 10))
        self._lexical_weight = float(lexical_weight)
        self._vector_weight = float(vector_weight)
        self._rank_constant = int(rank_constant)

    async def search(
        self,
        *,
        symbol: str,
        query: str,
        maximum_results: int,
        allowed_document_types: list[str] | None = None,
    ) -> list[HybridRetrievedFinancialChunk]:
        normalized_symbol = symbol.strip().upper()
        if not normalized_symbol:
            raise ValueError("symbol must not be blank")
        if not query.strip():
            raise ValueError("query must not be blank")

        lexical_candidates = min(self._lexical_candidate_count, max(1, int(maximum_results)))
        vector_candidates = min(self._vector_candidate_count, max(1, int(maximum_results)))

        lexical_results = await self._lexical.search(
            symbol=normalized_symbol,
            query=query,
            maximum_results=lexical_candidates,
            allowed_document_types=allowed_document_types,
        )
        vector_results = await self._vector.search(
            symbol=normalized_symbol,
            query=query,
            maximum_results=vector_candidates,
            allowed_document_types=allowed_document_types,
        )

        fused = reciprocal_rank_fusion(
            lexical_results,
            vector_results,
            lexical_weight=self._lexical_weight,
            vector_weight=self._vector_weight,
            rank_constant=self._rank_constant,
            maximum_results=max(1, min(int(maximum_results), 10)),
        )

        filtered: list[HybridRetrievedFinancialChunk] = []
        for item in fused:
            if item.chunk.symbol != normalized_symbol:
                continue
            if allowed_document_types is not None:
                allowed = {doc_type.lower() for doc_type in allowed_document_types}
                if item.chunk.document_type.value.lower() not in allowed:
                    continue
            filtered.append(item)

        return filtered[: max(1, min(int(maximum_results), 10))]

