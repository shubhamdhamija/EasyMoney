from __future__ import annotations

import asyncio

import pytest

from app.models.financial_document import DocumentType, FinancialDocument
from app.retrieval.configured_retriever import build_configured_retriever
from app.tools.search_financial_documents_tool import SearchFinancialDocumentsTool


@pytest.fixture
def documents() -> list[FinancialDocument]:
    return [
        FinancialDocument(
            document_id="aapl-revenue-001",
            symbol="AAPL",
            company_name="Apple Inc.",
            document_type=DocumentType.quarterly_report,
            title="AAPL Quarterly Report",
            published_at="2024-01-31T00:00:00Z",
            source_name="Fixture Source",
            source_url="https://example.com/aapl-quarterly",
            accession_number="AAPL-Q1-2024-001",
            content="# Revenue\nRevenue increased during the measured period and recurring subscriptions expanded steadily across the product portfolio.\n\n# Guidance\nGuidance remained resilient and management highlighted operating discipline.",
            checksum="a" * 64,
        ),
        FinancialDocument(
            document_id="msft-cloud-001",
            symbol="MSFT",
            company_name="Microsoft",
            document_type=DocumentType.earnings_release,
            title="MSFT Cloud Earnings",
            published_at="2024-02-02T00:00:00Z",
            source_name="Fixture Source",
            source_url="https://example.com/msft-cloud",
            accession_number="MSFT-Q1-2024-001",
            content="# Cloud Operations\nMicrosoft cloud operations expanded as Azure demand and data-center investments remained strong.\n\n# Revenue\nRevenue increased due to recurring subscriptions and enterprise demand.",
            checksum="c" * 64,
        ),
    ]


def _input(**kwargs):
    return type("Input", (), kwargs)()


def test_lexical_and_vector_and_hybrid_modes_are_compatible(documents: list[FinancialDocument]) -> None:
    for mode in ("lexical", "vector", "hybrid"):
        retriever = build_configured_retriever(mode=mode, documents=documents)
        tool = SearchFinancialDocumentsTool(retriever)
        result = asyncio.run(tool.execute(_input(symbol="AAPL", query="revenue", maximum_results=3, allowed_document_types=None)))
        assert tool.requires_approval is False
        assert result.result_count >= 0
        assert all(item.citation.document_id == item.chunk.document_id for item in result.results)
        assert all(item.chunk.symbol == "AAPL" for item in result.results)
        payload = str(result.model_dump()).lower()
        assert "vector" not in payload
        assert "rank" not in payload
        assert "model" not in payload


def test_retrieval_mode_in_model_arguments_is_rejected(documents: list[FinancialDocument]) -> None:
    retriever = build_configured_retriever(mode="hybrid", documents=documents)
    tool = SearchFinancialDocumentsTool(retriever)
    with pytest.raises(Exception):
        tool.input_model.model_validate({"symbol": "AAPL", "query": "revenue", "maximum_results": 3, "retrieval_mode": "hybrid"})


def test_embedding_model_argument_is_rejected(documents: list[FinancialDocument]) -> None:
    retriever = build_configured_retriever(mode="vector", documents=documents)
    tool = SearchFinancialDocumentsTool(retriever)
    with pytest.raises(Exception):
        tool.input_model.model_validate({"symbol": "AAPL", "query": "revenue", "maximum_results": 3, "embedding_model": "fake"})


def test_embedding_dimensions_are_rejected(documents: list[FinancialDocument]) -> None:
    retriever = build_configured_retriever(mode="vector", documents=documents)
    tool = SearchFinancialDocumentsTool(retriever)
    with pytest.raises(Exception):
        tool.input_model.model_validate({"symbol": "AAPL", "query": "revenue", "maximum_results": 3, "embedding_dimensions": 64})


def test_public_output_contains_no_vectors_or_rank_metadata(documents: list[FinancialDocument]) -> None:
    for mode in ("lexical", "vector", "hybrid"):
        retriever = build_configured_retriever(mode=mode, documents=documents)
        tool = SearchFinancialDocumentsTool(retriever)
        result = asyncio.run(tool.execute(_input(symbol="AAPL", query="revenue", maximum_results=3, allowed_document_types=None)))
        payload = result.model_dump(mode="json")
        text = str(payload).lower()
        assert "vector" not in text
        assert "rank" not in text
        assert "model" not in text


def test_citations_have_same_public_schema_in_all_modes(documents: list[FinancialDocument]) -> None:
    for mode in ("lexical", "vector", "hybrid"):
        retriever = build_configured_retriever(mode=mode, documents=documents)
        tool = SearchFinancialDocumentsTool(retriever)
        result = asyncio.run(tool.execute(_input(symbol="AAPL", query="revenue", maximum_results=3, allowed_document_types=None)))
        assert result.results
        assert all(set(item.citation.model_dump().keys()) == set(result.results[0].citation.model_dump().keys()) for item in result.results)


def test_symbol_isolation_remains_enforced(documents: list[FinancialDocument]) -> None:
    for mode in ("lexical", "vector", "hybrid"):
        retriever = build_configured_retriever(mode=mode, documents=documents)
        tool = SearchFinancialDocumentsTool(retriever)
        result = asyncio.run(tool.execute(_input(symbol="AAPL", query="revenue", maximum_results=3, allowed_document_types=None)))
        assert all(item.chunk.symbol == "AAPL" for item in result.results)


def test_no_network_call_occurs(documents: list[FinancialDocument], monkeypatch: pytest.MonkeyPatch) -> None:
    called = {"value": False}

    def fail_connect(*args, **kwargs):
        called["value"] = True
        raise AssertionError("network should not be used")

    monkeypatch.setattr("socket.socket.connect", fail_connect)
    retriever = build_configured_retriever(mode="vector", documents=documents)
    tool = SearchFinancialDocumentsTool(retriever)
    asyncio.run(tool.execute(_input(symbol="AAPL", query="revenue", maximum_results=3, allowed_document_types=None)))
    assert not called["value"]
