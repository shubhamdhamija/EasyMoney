from __future__ import annotations

import pytest

from app.retrieval.configured_retriever import build_configured_retriever
from app.retrieval.hybrid_retriever import HybridFinancialDocumentRetriever
from app.retrieval.retriever import LexicalFinancialDocumentRetriever
from app.retrieval.vector_retriever import VectorFinancialDocumentRetriever


def test_missing_mode_selects_lexical() -> None:
    retriever = build_configured_retriever(documents=[])
    assert isinstance(retriever, LexicalFinancialDocumentRetriever)


def test_lexical_mode_returns_lexical_retriever() -> None:
    retriever = build_configured_retriever(mode="lexical", documents=[])
    assert isinstance(retriever, LexicalFinancialDocumentRetriever)


def test_vector_mode_returns_vector_retriever() -> None:
    retriever = build_configured_retriever(mode="vector", documents=[])
    assert isinstance(retriever, VectorFinancialDocumentRetriever)


def test_hybrid_mode_returns_hybrid_retriever() -> None:
    retriever = build_configured_retriever(mode="hybrid", documents=[])
    assert isinstance(retriever, HybridFinancialDocumentRetriever)


def test_unknown_mode_is_rejected() -> None:
    with pytest.raises(ValueError):
        build_configured_retriever(mode="unknown", documents=[])


def test_invalid_embedding_dimensions_are_rejected() -> None:
    with pytest.raises(ValueError):
        build_configured_retriever(mode="vector", documents=[], embedding_dimensions=15)


def test_invalid_minimum_vector_score_is_rejected() -> None:
    with pytest.raises(ValueError):
        build_configured_retriever(mode="vector", documents=[], minimum_vector_score=2.0)


def test_invalid_lexical_weight_is_rejected() -> None:
    with pytest.raises(ValueError):
        build_configured_retriever(mode="hybrid", documents=[], lexical_weight=0.0)


def test_invalid_vector_weight_is_rejected() -> None:
    with pytest.raises(ValueError):
        build_configured_retriever(mode="hybrid", documents=[], vector_weight=-1.0)


def test_invalid_rank_constant_is_rejected() -> None:
    with pytest.raises(ValueError):
        build_configured_retriever(mode="hybrid", documents=[], rank_constant=0)


def test_no_external_embedding_provider_is_accepted() -> None:
    retriever = build_configured_retriever(mode="vector", documents=[], embedding_dimensions=64)
    assert isinstance(retriever, VectorFinancialDocumentRetriever)


def test_no_api_key_is_required() -> None:
    retriever = build_configured_retriever(mode="hybrid", documents=[])
    assert isinstance(retriever, HybridFinancialDocumentRetriever)


def test_construction_makes_no_network_call(monkeypatch: pytest.MonkeyPatch) -> None:
    called = {"value": False}

    def fail_connect(*args, **kwargs):
        called["value"] = True
        raise AssertionError("network should not be used")

    monkeypatch.setattr("socket.socket.connect", fail_connect)
    build_configured_retriever(mode="hybrid", documents=[])
    assert not called["value"]


def test_model_input_cannot_override_retrieval_mode() -> None:
    retriever = build_configured_retriever(mode="vector", documents=[])
    assert isinstance(retriever, VectorFinancialDocumentRetriever)

