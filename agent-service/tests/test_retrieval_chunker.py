from __future__ import annotations

from app.retrieval.document_chunker import DocumentChunker
from app.retrieval.fixture_loader import load_financial_documents


FIXTURE_DIR = "tests/fixtures/financial_documents"


def test_short_section_produces_one_chunk() -> None:
    doc = load_financial_documents(FIXTURE_DIR)[0]
    chunker = DocumentChunker(target_chunk_characters=500, overlap_characters=25, maximum_chunk_characters=800)
    chunks = chunker.chunk_document(doc)
    assert len(chunks) >= 1
    assert all(chunk.text.strip() for chunk in chunks)


def test_long_section_produces_multiple_chunks() -> None:
    doc = load_financial_documents(FIXTURE_DIR)[0]
    chunker = DocumentChunker(target_chunk_characters=80, overlap_characters=10, maximum_chunk_characters=200)
    chunks = chunker.chunk_document(doc)
    assert len(chunks) > 1


def test_chunker_preserves_section_metadata() -> None:
    doc = load_financial_documents(FIXTURE_DIR)[0]
    chunker = DocumentChunker()
    chunks = chunker.chunk_document(doc)
    assert all(chunk.document_id == doc.document_id for chunk in chunks)
    assert all(chunk.symbol == doc.symbol for chunk in chunks)
    assert all(chunk.title == doc.title for chunk in chunks)


def test_chunker_is_deterministic() -> None:
    doc = load_financial_documents(FIXTURE_DIR)[0]
    chunker = DocumentChunker(target_chunk_characters=120, overlap_characters=15, maximum_chunk_characters=300)
    first = chunker.chunk_document(doc)
    second = chunker.chunk_document(doc)
    assert [chunk.chunk_id for chunk in first] == [chunk.chunk_id for chunk in second]


def test_chunker_does_not_mutate_source_document() -> None:
    doc = load_financial_documents(FIXTURE_DIR)[0]
    original = doc.model_dump(mode="json")
    DocumentChunker().chunk_document(doc)
    assert doc.model_dump(mode="json") == original


def test_chunker_never_mixes_documents() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    chunker = DocumentChunker(target_chunk_characters=100, overlap_characters=10, maximum_chunk_characters=250)
    chunks = []
    for doc in docs:
        chunks.extend(chunker.chunk_document(doc))
    assert len({chunk.document_id for chunk in chunks}) == len({doc.document_id for doc in docs})

