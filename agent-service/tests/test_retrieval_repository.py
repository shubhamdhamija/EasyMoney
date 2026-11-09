from __future__ import annotations

import pytest

from app.models.financial_document import FinancialDocument
from app.retrieval.document_repository import InMemoryFinancialDocumentRepository
from app.retrieval.fixture_loader import load_financial_documents

FIXTURE_DIR = "tests/fixtures/financial_documents"


def test_repository_is_empty_by_default() -> None:
    repo = InMemoryFinancialDocumentRepository()
    assert repo.list_documents() == []
    assert repo.get_document("missing") is None


def test_repository_adds_and_retrieves_documents() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)

    fetched = repo.get_document(docs[0].document_id)
    assert fetched is not None
    assert fetched.document_id == docs[0].document_id


def test_repository_filters_by_normalized_symbol() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)

    assert [doc.symbol for doc in repo.list_documents(symbol=" aapl ")] == ["AAPL", "AAPL", "AAPL"]


def test_repository_unknown_symbol_returns_empty_list() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)

    assert repo.list_documents(symbol="NVDA") == []


def test_repository_rejects_duplicate_ids() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.add_document(docs[0])

    with pytest.raises(ValueError):
        repo.add_document(docs[0])


def test_repository_rejects_checksum_mismatch() -> None:
    repo = InMemoryFinancialDocumentRepository()
    doc = load_financial_documents(FIXTURE_DIR)[0].model_copy(update={"content": "tampered"})

    with pytest.raises(ValueError):
        repo.add_document(doc)


def test_repository_replace_and_clear_work() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)
    repo.clear()
    assert repo.list_documents() == []


def test_repository_returns_stable_ordering() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)

    ordered = repo.list_documents()
    assert [doc.document_id for doc in ordered] == sorted([doc.document_id for doc in docs])


def test_repository_returns_copies_not_mutable_references() -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)

    fetched = repo.get_document(docs[0].document_id)
    assert fetched is not None
    fetched.company_name = "Mutated"
    assert repo.get_document(docs[0].document_id).company_name == docs[0].company_name


def test_repository_does_not_write_disk_or_network(monkeypatch: pytest.MonkeyPatch) -> None:
    repo = InMemoryFinancialDocumentRepository()
    docs = load_financial_documents(FIXTURE_DIR)
    repo.replace_documents(docs)

    called = False

    def fail(*args, **kwargs):
        nonlocal called
        called = True
        raise AssertionError("network called")

    monkeypatch.setattr("socket.socket.connect", fail, raising=False)
    repo.list_documents()
    assert called is False
