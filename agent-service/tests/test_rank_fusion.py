from __future__ import annotations

import pytest

from app.models.financial_document import DocumentType, FinancialDocumentChunk
from app.models.financial_document_search import HybridRetrievedFinancialChunk
from app.retrieval.rank_fusion import reciprocal_rank_fusion
from app.retrieval.retriever import RetrievedFinancialChunk


def _chunk(document_id: str, section_name: str, chunk_index: int = 0) -> FinancialDocumentChunk:
    return FinancialDocumentChunk(
        chunk_id=f"{document_id}:{section_name}:{chunk_index}",
        document_id=document_id,
        symbol="AAPL",
        document_type=DocumentType.quarterly_report,
        title="AAPL Quarterly Report",
        section_name=section_name,
        chunk_index=chunk_index,
        text=f"section {section_name} content {chunk_index}",
        published_at="2024-01-01T00:00:00Z",
        source_name="Fixture",
        source_url="https://example.com/test",
        accession_number=None,
        checksum="a" * 64,
    )


def _result(chunk: FinancialDocumentChunk, score: float, matched_terms: list[str] | None = None) -> RetrievedFinancialChunk:
    return RetrievedFinancialChunk(
        chunk=chunk,
        score=score,
        matched_terms=matched_terms or ["revenue"],
        citation=type(
            "Citation",
            (),
            {"chunk_id": chunk.chunk_id, "document_id": chunk.document_id, "symbol": chunk.symbol, "title": chunk.title, "document_type": chunk.document_type, "section_name": chunk.section_name, "published_at": chunk.published_at, "source_name": chunk.source_name, "source_url": chunk.source_url, "accession_number": chunk.accession_number},
        )(),
    )


def test_lexical_only_chunk_remains() -> None:
    lexical = [_result(_chunk("doc-1", "Revenue"), 0.9)]
    fused = reciprocal_rank_fusion(lexical, [], maximum_results=5)
    assert len(fused) == 1
    assert fused[0].chunk.document_id == "doc-1"


def test_vector_only_chunk_remains() -> None:
    vector = [_result(_chunk("doc-2", "Risk"), 0.8)]
    fused = reciprocal_rank_fusion([], vector, maximum_results=5)
    assert len(fused) == 1
    assert fused[0].chunk.document_id == "doc-2"


def test_chunk_appearing_in_both_receives_contributions_from_both() -> None:
    shared = _chunk("doc-3", "Revenue")
    fused = reciprocal_rank_fusion([_result(shared, 0.9)], [_result(shared, 0.8)], maximum_results=5)
    assert fused[0].retrieval_method == "hybrid"
    assert fused[0].lexical_score == pytest.approx(0.9)
    assert fused[0].vector_score == pytest.approx(0.8)


def test_duplicate_chunk_ids_are_merged() -> None:
    shared = _chunk("doc-3", "Revenue")
    fused = reciprocal_rank_fusion([_result(shared, 0.9)], [_result(shared, 0.8)], maximum_results=5)
    assert len(fused) == 1


def test_no_duplicate_chunks_appear_in_output() -> None:
    shared = _chunk("doc-3", "Revenue")
    fused = reciprocal_rank_fusion([_result(shared, 0.9), _result(shared, 0.8)], [_result(shared, 0.7)], maximum_results=5)
    assert sum(1 for item in fused if item.chunk.chunk_id == shared.chunk_id) == 1


def test_lexical_rank_is_preserved() -> None:
    first = _chunk("doc-1", "Revenue")
    second = _chunk("doc-2", "Risk")
    fused = reciprocal_rank_fusion([_result(first, 0.9), _result(second, 0.6)], [], maximum_results=5)
    assert fused[0].lexical_rank == 1
    assert fused[1].lexical_rank == 2


def test_vector_rank_is_preserved() -> None:
    first = _chunk("doc-1", "Revenue")
    second = _chunk("doc-2", "Risk")
    fused = reciprocal_rank_fusion([], [_result(first, 0.9), _result(second, 0.6)], maximum_results=5)
    assert fused[0].vector_rank == 1
    assert fused[1].vector_rank == 2


def test_source_scores_are_preserved() -> None:
    item = _chunk("doc-1", "Revenue")
    fused = reciprocal_rank_fusion([_result(item, 0.9)], [_result(item, 0.8)], maximum_results=5)
    assert fused[0].lexical_score == pytest.approx(0.9)
    assert fused[0].vector_score == pytest.approx(0.8)


def test_fused_score_remains_between_zero_and_one() -> None:
    item = _chunk("doc-1", "Revenue")
    fused = reciprocal_rank_fusion([_result(item, 0.9)], [_result(item, 0.8)], maximum_results=5)
    assert 0.0 <= fused[0].fused_score <= 1.0


def test_ordering_is_deterministic() -> None:
    first = _chunk("doc-1", "Revenue")
    second = _chunk("doc-2", "Risk")
    fused_a = reciprocal_rank_fusion([_result(first, 0.9), _result(second, 0.6)], [_result(first, 0.7), _result(second, 0.4)], maximum_results=5)
    fused_b = reciprocal_rank_fusion([_result(first, 0.9), _result(second, 0.6)], [_result(first, 0.7), _result(second, 0.4)], maximum_results=5)
    assert [item.chunk.document_id for item in fused_a] == [item.chunk.document_id for item in fused_b]


def test_equal_fused_scores_use_stable_tie_breaking() -> None:
    first = _chunk("doc-1", "Revenue")
    second = _chunk("doc-2", "Risk")
    fused = reciprocal_rank_fusion([_result(first, 0.5)], [_result(second, 0.5)], maximum_results=10)
    assert [item.chunk.document_id for item in fused] == ["doc-1", "doc-2"]


def test_result_limit_is_enforced() -> None:
    lexical = [_result(_chunk(f"doc-{idx}", "Revenue"), 0.5) for idx in range(1, 5)]
    fused = reciprocal_rank_fusion(lexical, [], maximum_results=2)
    assert len(fused) == 2


def test_increasing_lexical_weight_can_alter_ranking() -> None:
    first = _chunk("doc-1", "Revenue")
    second = _chunk("doc-2", "Risk")
    base = reciprocal_rank_fusion([_result(first, 0.9), _result(second, 0.7)], [_result(first, 0.1), _result(second, 0.9)], lexical_weight=1.0, vector_weight=1.0, maximum_results=5)
    boosted = reciprocal_rank_fusion([_result(first, 0.9), _result(second, 0.7)], [_result(first, 0.1), _result(second, 0.9)], lexical_weight=10.0, vector_weight=1.0, maximum_results=5)
    assert [item.chunk.document_id for item in base] != [item.chunk.document_id for item in boosted]


def test_increasing_vector_weight_can_alter_ranking() -> None:
    first = _chunk("doc-1", "Revenue")
    second = _chunk("doc-2", "Risk")
    base = reciprocal_rank_fusion([_result(first, 0.8), _result(second, 0.2)], [_result(first, 0.1), _result(second, 0.9)], lexical_weight=1.0, vector_weight=1.0, maximum_results=5)
    boosted = reciprocal_rank_fusion([_result(first, 0.8), _result(second, 0.2)], [_result(first, 0.1), _result(second, 0.9)], lexical_weight=1.0, vector_weight=10.0, maximum_results=5)
    assert [item.chunk.document_id for item in base] != [item.chunk.document_id for item in boosted]


def test_zero_weight_is_rejected() -> None:
    with pytest.raises(ValueError):
        reciprocal_rank_fusion([], [], lexical_weight=0.0)


def test_negative_weight_is_rejected() -> None:
    with pytest.raises(ValueError):
        reciprocal_rank_fusion([], [], vector_weight=-1.0)


def test_rank_constant_equal_to_zero_is_rejected() -> None:
    with pytest.raises(ValueError):
        reciprocal_rank_fusion([], [], rank_constant=0)


def test_negative_rank_constant_is_rejected() -> None:
    with pytest.raises(ValueError):
        reciprocal_rank_fusion([], [], rank_constant=-1)


def test_citation_and_chunk_mismatch_is_rejected() -> None:
    chunk = _chunk("doc-1", "Revenue")
    bad = _result(chunk, 0.9)
    bad.citation = type("Citation", (), {"chunk_id": "other-id", "document_id": chunk.document_id, "symbol": chunk.symbol, "title": chunk.title, "document_type": chunk.document_type, "section_name": chunk.section_name, "published_at": chunk.published_at, "source_name": chunk.source_name, "source_url": chunk.source_url, "accession_number": chunk.accession_number})()
    with pytest.raises(Exception):
        reciprocal_rank_fusion([bad], [], maximum_results=5)


def test_input_lists_are_not_mutated() -> None:
    lexical = [_result(_chunk("doc-1", "Revenue"), 0.9)]
    vector = [_result(_chunk("doc-2", "Risk"), 0.8)]
    before_lexical = list(lexical)
    before_vector = list(vector)
    reciprocal_rank_fusion(lexical, vector, maximum_results=5)
    assert lexical == before_lexical
    assert vector == before_vector

