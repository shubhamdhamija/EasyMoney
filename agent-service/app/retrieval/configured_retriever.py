from __future__ import annotations

import asyncio

from app.retrieval.fake_embedding_model import DeterministicFakeEmbeddingModel
from app.retrieval.hybrid_retriever import HybridFinancialDocumentRetriever
from app.retrieval.retriever import LexicalFinancialDocumentRetriever
from app.retrieval.vector_retriever import VectorFinancialDocumentRetriever


def build_configured_retriever(
    *,
    mode: str = "lexical",
    documents=None,
    lexical_retriever=None,
    embedding_dimensions: int | None = None,
    minimum_vector_score: float | None = None,
    lexical_weight: float | None = None,
    vector_weight: float | None = None,
    rank_constant: int | None = None,
):
    normalized = (mode or "lexical").strip().lower()
    if normalized == "lexical":
        return lexical_retriever or LexicalFinancialDocumentRetriever(documents=documents or [])
    if normalized == "vector":
        dimensions = int(embedding_dimensions) if embedding_dimensions is not None else 64
        if not 16 <= dimensions <= 512:
            raise ValueError("embedding dimensions must be between 16 and 512")
        score = float(minimum_vector_score) if minimum_vector_score is not None else 0.15
        if not 0.0 <= score <= 1.0:
            raise ValueError("minimum_vector_score must be between 0.0 and 1.0")
        retriever = VectorFinancialDocumentRetriever(
            documents=documents or [],
            embedding_model=DeterministicFakeEmbeddingModel(dimensions=dimensions),
            minimum_vector_score=score,
        )
        if documents:
            asyncio.run(retriever.build_index(documents))
        return retriever
    if normalized == "hybrid":
        if lexical_retriever is None:
            lexical_retriever = LexicalFinancialDocumentRetriever(documents=documents or [])
        dimensions = int(embedding_dimensions) if embedding_dimensions is not None else 64
        if not 16 <= dimensions <= 512:
            raise ValueError("embedding dimensions must be between 16 and 512")
        score = float(minimum_vector_score) if minimum_vector_score is not None else 0.15
        if not 0.0 <= score <= 1.0:
            raise ValueError("minimum_vector_score must be between 0.0 and 1.0")
        lw = float(lexical_weight) if lexical_weight is not None else 1.0
        vw = float(vector_weight) if vector_weight is not None else 1.0
        rc = int(rank_constant) if rank_constant is not None else 60
        if lw <= 0.0 or vw <= 0.0:
            raise ValueError("weights must be positive")
        if rc <= 0:
            raise ValueError("rank_constant must be greater than zero")
        vector_retriever = VectorFinancialDocumentRetriever(
            documents=documents or [],
            embedding_model=DeterministicFakeEmbeddingModel(dimensions=dimensions),
            minimum_vector_score=score,
        )
        if documents:
            asyncio.run(vector_retriever.build_index(documents))
        return HybridFinancialDocumentRetriever(
            lexical_retriever=lexical_retriever,
            vector_retriever=vector_retriever,
            lexical_candidate_count=10,
            vector_candidate_count=10,
            final_maximum_results=5,
            lexical_weight=lw,
            vector_weight=vw,
            rank_constant=rc,
        )
    raise ValueError(f"unsupported retrieval mode: {mode}")
