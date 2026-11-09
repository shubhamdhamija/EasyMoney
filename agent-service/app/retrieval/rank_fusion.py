from __future__ import annotations

from app.models.financial_document_search import HybridRetrievedFinancialChunk
from app.retrieval.retriever import RetrievedFinancialChunk


def reciprocal_rank_fusion(
    lexical_results: list[RetrievedFinancialChunk],
    vector_results: list[RetrievedFinancialChunk],
    *,
    lexical_weight: float = 1.0,
    vector_weight: float = 1.0,
    rank_constant: int = 60,
    maximum_results: int = 10,
) -> list[HybridRetrievedFinancialChunk]:
    if lexical_weight <= 0.0 or vector_weight <= 0.0:
        raise ValueError("weights must be positive")
    if rank_constant <= 0:
        raise ValueError("rank_constant must be greater than zero")
    if maximum_results <= 0:
        raise ValueError("maximum_results must be positive")

    fused: dict[str, HybridRetrievedFinancialChunk] = {}

    def _validate_citation(item: RetrievedFinancialChunk, *, chunk_id: str) -> None:
        citation = item.citation
        if getattr(citation, "chunk_id", None) != chunk_id:
            raise ValueError("citation and chunk mismatch")
        if getattr(citation, "document_id", None) != item.chunk.document_id:
            raise ValueError("citation and chunk mismatch")

    for rank, item in enumerate(lexical_results, start=1):
        _validate_citation(item, chunk_id=item.chunk.chunk_id)
        key = item.chunk.chunk_id
        if key not in fused:
            fused[key] = HybridRetrievedFinancialChunk(
                chunk=item.chunk,
                citation=item.citation,
                retrieval_method="lexical",
                lexical_score=item.score,
                vector_score=None,
                lexical_rank=rank,
                vector_rank=None,
                fused_score=0.0,
                matched_terms=item.matched_terms,
            )
        else:
            fused[key].lexical_rank = rank
            fused[key].lexical_score = item.score
            if fused[key].retrieval_method == "vector":
                fused[key].retrieval_method = "hybrid"

    for rank, item in enumerate(vector_results, start=1):
        _validate_citation(item, chunk_id=item.chunk.chunk_id)
        key = item.chunk.chunk_id
        if key not in fused:
            fused[key] = HybridRetrievedFinancialChunk(
                chunk=item.chunk,
                citation=item.citation,
                retrieval_method="vector",
                lexical_score=None,
                vector_score=item.score,
                lexical_rank=None,
                vector_rank=rank,
                fused_score=0.0,
                matched_terms=item.matched_terms,
            )
        else:
            fused[key].vector_rank = rank
            fused[key].vector_score = item.score
            if fused[key].retrieval_method == "lexical":
                fused[key].retrieval_method = "hybrid"

    if not fused:
        return []

    candidates = list(fused.values())
    for item in candidates:
        lexical_term = (
            (lexical_weight * (item.lexical_score or 0.0)) / (rank_constant + (item.lexical_rank or 0))
            if item.lexical_rank
            else 0.0
        )
        vector_term = (
            (vector_weight * (item.vector_score or 0.0)) / (rank_constant + (item.vector_rank or 0))
            if item.vector_rank
            else 0.0
        )
        item.fused_score = float(min(1.0, (1.5 * lexical_term) + (0.5 * vector_term)))

    max_score = max((item.fused_score for item in candidates), default=0.0)
    if max_score > 0.0:
        for item in candidates:
            item.fused_score = max(0.0, min(1.0, item.fused_score / max_score))

    candidates.sort(key=lambda item: (-item.fused_score, item.chunk.document_id, item.chunk.chunk_index))
    return candidates[:maximum_results]
