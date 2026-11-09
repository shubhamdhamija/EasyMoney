from __future__ import annotations

import asyncio

import pytest

from app.models.financial_document_search import FinancialDocumentSearchInput
from app.retrieval.fixture_loader import load_financial_documents
from app.retrieval.retriever import LexicalFinancialDocumentRetriever
from app.tools.search_financial_documents_tool import SearchFinancialDocumentsTool

FIXTURE_DIR = "tests/fixtures/financial_documents"


def test_search_tool_has_expected_metadata() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    tool = SearchFinancialDocumentsTool(LexicalFinancialDocumentRetriever(documents=docs))

    assert tool.name == "search_financial_documents"
    assert tool.input_model is FinancialDocumentSearchInput
    assert tool.requires_approval is False
    assert tool.description
    assert "read-only" in tool.description.lower()
    assert "untrusted" in tool.description.lower()


def test_search_tool_successful_revenue_search() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    tool = SearchFinancialDocumentsTool(LexicalFinancialDocumentRetriever(documents=docs))

    result = asyncio.run(
        tool.execute(
            FinancialDocumentSearchInput(
                symbol="AAPL",
                query="revenue increased during the measured period",
                maximum_results=3,
            )
        )
    )

    assert result.result_count >= 1
    assert result.evidence_sufficient is True
    assert result.results[0].citation.document_id == "aapl-quarterly-controlled-001"


def test_search_tool_no_result_response() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    tool = SearchFinancialDocumentsTool(LexicalFinancialDocumentRetriever(documents=docs))

    result = asyncio.run(
        tool.execute(
            FinancialDocumentSearchInput(
                symbol="AAPL",
                query="spacecraft launch trajectory metrics",
                maximum_results=3,
            )
        )
    )

    assert result.result_count == 0
    assert result.evidence_sufficient is False


def test_search_tool_respects_maximum_result_limit() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    tool = SearchFinancialDocumentsTool(LexicalFinancialDocumentRetriever(documents=docs))

    result = asyncio.run(
        tool.execute(
            FinancialDocumentSearchInput(
                symbol="AAPL",
                query="revenue",
                maximum_results=1,
            )
        )
    )

    assert len(result.results) <= 1


def test_search_tool_validates_symbol_and_query() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    tool = SearchFinancialDocumentsTool(LexicalFinancialDocumentRetriever(documents=docs))

    with pytest.raises(ValueError):
        FinancialDocumentSearchInput(symbol="BAD$", query="revenue")

    with pytest.raises(ValueError):
        FinancialDocumentSearchInput(symbol="AAPL", query="")


def test_search_tool_records_calls() -> None:
    docs = load_financial_documents(FIXTURE_DIR)
    tool = SearchFinancialDocumentsTool(LexicalFinancialDocumentRetriever(documents=docs))

    asyncio.run(tool.execute(FinancialDocumentSearchInput(symbol="AAPL", query="revenue", maximum_results=2)))

    assert len(tool.calls) == 1
    assert tool.calls[0].symbol == "AAPL"

