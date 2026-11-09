from __future__ import annotations

import asyncio

from app.models.research_state import ResearchState
from app.services.tool_execution_policy import evaluate_tool_request
from app.tools.default_registry import create_default_registry, create_research_registry
from app.tools.fake_recent_news_tool import FakeRecentNewsTool
from app.tools.fake_stock_quote_tool import FakeStockQuoteTool
from app.tools.search_financial_documents_tool import SearchFinancialDocumentsTool


class DummyDocumentTool(SearchFinancialDocumentsTool):
    pass


def test_retrieval_registry_contains_expected_tools() -> None:
    registry = create_default_registry(document_search_tool=SearchFinancialDocumentsTool.__new__(SearchFinancialDocumentsTool))
    names = registry.available_tool_names()
    assert "get_stock_quote" in names
    assert "get_recent_news" in names
    assert "search_financial_documents" in names


def test_default_registry_remains_backwards_compatible() -> None:
    registry = create_default_registry()
    assert "get_stock_quote" in registry.available_tool_names()
    assert "get_recent_news" in registry.available_tool_names()


def test_search_tool_is_read_only() -> None:
    registry = create_default_registry(document_search_tool=SearchFinancialDocumentsTool.__new__(SearchFinancialDocumentsTool))
    assert registry.requires_approval("search_financial_documents") is False


def test_policy_rejects_cross_symbol_document_search() -> None:
    state = ResearchState(symbol="AAPL")
    result = evaluate_tool_request(
        state=state,
        tool_name="search_financial_documents",
        arguments={"symbol": "TSLA", "query": "revenue", "maximum_results": 3},
        maximum_news_items=3,
    )
    assert result.outcome == "invalid_arguments"


def test_policy_skips_duplicate_document_query_case_insensitive() -> None:
    state = ResearchState(symbol="AAPL", document_search_queries=["  Revenue increased  "])
    result = evaluate_tool_request(
        state=state,
        tool_name="search_financial_documents",
        arguments={"symbol": "AAPL", "query": "revenue increased", "maximum_results": 3},
        maximum_news_items=3,
    )
    assert result.outcome == "skip_duplicate"


def test_policy_caps_maximum_results() -> None:
    state = ResearchState(symbol="AAPL")
    result = evaluate_tool_request(
        state=state,
        tool_name="search_financial_documents",
        arguments={"symbol": "AAPL", "query": "revenue", "maximum_results": 50},
        maximum_news_items=3,
    )
    assert result.should_execute is True
    assert result.arguments["maximum_results"] == 10

