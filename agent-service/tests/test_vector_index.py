from __future__ import annotations

import pytest

from app.models.embedding import EmbeddingVector, EmbeddedFinancialChunk
from app.models.financial_document import DocumentType, FinancialCitation, FinancialDocumentChunk
from app.retrieval.vector_index import InMemoryFinancialDocumentVectorIndex


def _chunk(document_id: str, symbol: str, document_type: str, chunk_index: int, text: str) -> FinancialDocumentChunk:
    return FinancialDocumentChunk(
        chunk_id=f"{document_id}-{chunk_index}",
        document_id=document_id,
        symbol=symbol,
        document_type=DocumentType(document_type),
        title=f"{symbol} {document_type}",
        section_name="Revenue",
        chunk_index=chunk_index,
        text=text,
        published_at="2024-01-01T00:00:00Z",
        source_name="Fixture",
        source_url=None,
        accession_number=None,
        checksum="a" * 64,
    )


def _embedded(chunk: FinancialDocumentChunk, values: list[float]) -> EmbeddedFinancialChunk:
    return EmbeddedFinancialChunk(
        chunk=chunk,
        embedding=EmbeddingVector(values=values, dimensions=len(values), model_name="test-model", model_version="v1"),
    )


def test_empty_index_returns_no_results() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    assert index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=5) == []


def test_replace_stores_all_entries() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    chunks = [_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0]), _embedded(_chunk("doc-2", "AAPL", "quarterly_report", 0, "guidance"), [0.0, 1.0])]
    index.replace(chunks)
    assert index.count() == 2


def test_count_is_correct() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    chunks = [_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0]), _embedded(_chunk("doc-2", "AAPL", "quarterly_report", 0, "guidance"), [0.0, 1.0])]
    index.replace(chunks)
    assert index.count() == 2


def test_replacement_removes_previous_entries() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([_embedded(_chunk("old", "AAPL", "annual_report", 0, "old"), [1.0, 0.0])])
    index.replace([_embedded(_chunk("new", "AAPL", "annual_report", 0, "new"), [0.0, 1.0])])
    assert index.count() == 1
    assert index.search(query_embedding=[0.0, 1.0], symbol="AAPL", maximum_results=5)[0][0].chunk.document_id == "new"


def test_replacement_is_atomic_if_validation_fails() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    valid = [_embedded(_chunk("good", "AAPL", "annual_report", 0, "good"), [1.0, 0.0])]
    index.replace(valid)
    bad = [_embedded(_chunk("bad", "AAPL", "annual_report", 0, "bad"), [1.0])]  # dimension mismatch
    with pytest.raises(ValueError):
        index.replace(bad)
    assert index.count() == 1
    assert index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=5)[0][0].chunk.document_id == "good"


def test_mixed_embedding_dimensions_are_rejected() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    with pytest.raises(ValueError):
        index.replace([
            _embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0]),
            _embedded(_chunk("doc-2", "AAPL", "annual_report", 0, "risk"), [1.0, 0.0, 0.0]),
        ])


def test_query_dimension_mismatch_is_rejected() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0])])
    with pytest.raises(ValueError):
        index.search(query_embedding=[1.0], symbol="AAPL", maximum_results=2)


def test_aapl_search_returns_only_aapl_chunks() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([
        _embedded(_chunk("a-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0]),
        _embedded(_chunk("a-2", "MSFT", "annual_report", 0, "revenue"), [1.0, 0.0]),
        _embedded(_chunk("a-3", "AAPL", "quarterly_report", 1, "risk"), [0.0, 1.0]),
    ])
    results = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=10)
    assert all(item[0].chunk.symbol == "AAPL" for item in results)


def test_symbol_normalization_works() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([_embedded(_chunk("doc-1", "aapl", "annual_report", 0, "revenue"), [1.0, 0.0])])
    results = index.search(query_embedding=[1.0, 0.0], symbol=" aapl ", maximum_results=5)
    assert results and results[0][0].chunk.symbol == "AAPL"


def test_document_type_filtering_works() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([
        _embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0]),
        _embedded(_chunk("doc-2", "AAPL", "quarterly_report", 0, "revenue"), [0.0, 1.0]),
    ])
    results = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=10, allowed_document_types=["annual_report"])
    assert all(item[0].chunk.document_type.value == "annual_report" for item in results)


def test_maximum_results_is_enforced() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([
        _embedded(_chunk(f"doc-{i}", "AAPL", "annual_report", 0, f"chunk {i}"), [1.0, 0.0])
        for i in range(5)
    ])
    results = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=2)
    assert len(results) == 2


def test_maximum_results_outside_range_is_rejected() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    with pytest.raises(ValueError):
        index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=0)
    with pytest.raises(ValueError):
        index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=11)


def test_results_sort_by_descending_similarity() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([
        _embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "quarterly revenue"), [1.0, 0.0]),
        _embedded(_chunk("doc-2", "AAPL", "annual_report", 1, "risk"), [0.9, 0.0]),
    ])
    results = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=10)
    assert results[0][1] >= results[1][1]


def test_equal_scores_use_document_id_and_chunk_index_for_stable_tie_breaking() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([
        _embedded(_chunk("doc-b", "AAPL", "annual_report", 1, "same"), [1.0, 0.0]),
        _embedded(_chunk("doc-a", "AAPL", "annual_report", 0, "same"), [1.0, 0.0]),
    ])
    results = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=10)
    assert results[0][0].chunk.document_id == "doc-a"
    assert results[1][0].chunk.document_id == "doc-b"


def test_returned_values_cannot_mutate_internal_state() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    chunk = _chunk("doc-1", "AAPL", "annual_report", 0, "revenue")
    index.replace([_embedded(chunk, [1.0, 0.0])])
    result = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=5)[0]
    result[0].chunk.document_id = "changed"
    assert index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=5)[0][0].chunk.document_id == "doc-1"


def test_clear_removes_everything() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0])])
    index.clear()
    assert index.count() == 0


def test_index_does_not_generate_citations() -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    index.replace([_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0])])
    result = index.search(query_embedding=[1.0, 0.0], symbol="AAPL", maximum_results=5)
    assert all(not hasattr(item[0], "citation") for item in result)


def test_no_disk_write_occurs(monkeypatch: pytest.MonkeyPatch) -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    original = __import__("builtins").open

    def guard(*args, **kwargs):
        raise AssertionError("filesystem access is not allowed")

    monkeypatch.setattr("builtins.open", guard)
    index.replace([_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0])])
    assert index.count() == 1


def test_no_network_call_occurs(monkeypatch: pytest.MonkeyPatch) -> None:
    index = InMemoryFinancialDocumentVectorIndex()
    called = {"value": False}

    def fail_connect(*args, **kwargs):
        called["value"] = True
        raise AssertionError("network should not be used")

    monkeypatch.setattr("socket.socket.connect", fail_connect)
    index.replace([_embedded(_chunk("doc-1", "AAPL", "annual_report", 0, "revenue"), [1.0, 0.0])])
    assert not called["value"]

