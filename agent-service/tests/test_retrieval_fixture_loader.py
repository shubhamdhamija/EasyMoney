from __future__ import annotations

import json
from pathlib import Path

import pytest

from app.models.financial_document import FinancialDocument
from app.retrieval.fixture_loader import FixtureLoadError, load_financial_documents

FIXTURE_DIR = Path(__file__).resolve().parent / "fixtures" / "financial_documents"


def test_fixture_loader_successfully_loads_corpus() -> None:
    docs = load_financial_documents(FIXTURE_DIR)

    assert len(docs) == 5
    assert [doc.document_id for doc in docs] == [
        "aapl-annual-controlled-001",
        "aapl-injection-controlled-001",
        "aapl-quarterly-controlled-001",
        "msft-earnings-controlled-001",
        "tsla-material-controlled-001",
    ]


def test_fixture_loader_orders_documents_stably() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    ids = [doc.document_id for doc in docs]
    assert ids == sorted(ids)


def test_fixture_loader_rejects_invalid_json(tmp_path: Path) -> None:
    invalid = tmp_path / "broken.json"
    invalid.write_text("{not-valid-json}", encoding="utf-8")

    with pytest.raises(FixtureLoadError, match="invalid JSON"):
        load_financial_documents(tmp_path)


def test_fixture_loader_rejects_missing_fields(tmp_path: Path) -> None:
    payload = {"document_id": "id-1", "symbol": "AAPL"}
    (tmp_path / "missing.json").write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(FixtureLoadError):
        load_financial_documents(tmp_path)


def test_fixture_loader_rejects_invalid_document_type(tmp_path: Path) -> None:
    payload = {
        "document_id": "id-1",
        "symbol": "AAPL",
        "company_name": "Apple Inc.",
        "document_type": "not-real",
        "title": "Example",
        "published_at": "2026-01-01T00:00:00Z",
        "source_name": "EasyMoney",
        "sections": [{"section_name": "Revenue", "text": "Revenue increased."}],
    }
    (tmp_path / "bad-type.json").write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(FixtureLoadError):
        load_financial_documents(tmp_path)


def test_fixture_loader_rejects_invalid_symbol(tmp_path: Path) -> None:
    payload = {
        "document_id": "id-1",
        "symbol": "BAD$SYMBOL",
        "company_name": "Apple Inc.",
        "document_type": "annual_report",
        "title": "Example",
        "published_at": "2026-01-01T00:00:00Z",
        "source_name": "EasyMoney",
        "sections": [{"section_name": "Revenue", "text": "Revenue increased."}],
    }
    (tmp_path / "bad-symbol.json").write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(ValueError):
        load_financial_documents(tmp_path)


def test_fixture_loader_rejects_checksum_mismatch(tmp_path: Path) -> None:
    payload = {
        "document_id": "id-1",
        "symbol": "AAPL",
        "company_name": "Apple Inc.",
        "document_type": "annual_report",
        "title": "Example",
        "published_at": "2026-01-01T00:00:00Z",
        "source_name": "EasyMoney",
        "sections": [{"section_name": "Revenue", "text": "Revenue increased."}],
        "checksum": "0" * 64,
    }
    (tmp_path / "bad-checksum.json").write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(FixtureLoadError, match="checksum"):
        load_financial_documents(tmp_path)


def test_fixture_loader_rejects_duplicate_document_id(tmp_path: Path) -> None:
    payload_1 = {
        "document_id": "dup-1",
        "symbol": "AAPL",
        "company_name": "Apple Inc.",
        "document_type": "annual_report",
        "title": "Example",
        "published_at": "2026-01-01T00:00:00Z",
        "source_name": "EasyMoney",
        "sections": [{"section_name": "Revenue", "text": "Revenue increased."}],
    }
    payload_2 = {
        "document_id": "dup-1",
        "symbol": "AAPL",
        "company_name": "Apple Inc.",
        "document_type": "quarterly_report",
        "title": "Example 2",
        "published_at": "2026-01-02T00:00:00Z",
        "source_name": "EasyMoney",
        "sections": [{"section_name": "Revenue", "text": "Revenue increased again."}],
    }
    (tmp_path / "one.json").write_text(json.dumps(payload_1), encoding="utf-8")
    (tmp_path / "two.json").write_text(json.dumps(payload_2), encoding="utf-8")

    with pytest.raises(FixtureLoadError, match="duplicate document ID"):
        load_financial_documents(tmp_path)


def test_fixture_loader_rejects_unsupported_extension(tmp_path: Path) -> None:
    bad = tmp_path / "bad.txt"
    bad.write_text("hello", encoding="utf-8")

    assert load_financial_documents(tmp_path) == []


def test_fixture_loader_rejects_path_traversal(tmp_path: Path) -> None:
    with pytest.raises(Exception):
        load_financial_documents("../")


def test_fixture_loader_rejects_symbolic_link_escape(tmp_path: Path) -> None:
    external = tmp_path.parent / "external-fixtures"
    external.mkdir(exist_ok=True)
    link = tmp_path / "evil-link.json"
    link.symlink_to(external)

    with pytest.raises(FixtureLoadError):
        load_financial_documents(tmp_path)


def test_fixture_loader_never_calls_network(monkeypatch: pytest.MonkeyPatch) -> None:
    called = False

    def fail(*args, **kwargs):
        nonlocal called
        called = True
        raise AssertionError("network called")

    monkeypatch.setattr("socket.socket.connect", fail, raising=False)
    load_financial_documents(FIXTURE_DIR)
    assert called is False

