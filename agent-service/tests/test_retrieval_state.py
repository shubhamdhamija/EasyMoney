from __future__ import annotations

from app.models.financial_document_search import FinancialDocumentSearchInput, FinancialDocumentSearchResult
from app.models.research_state import ResearchState


def test_document_results_are_json_safe() -> None:
    state = ResearchState(symbol="AAPL")
    result = FinancialDocumentSearchResult(
        symbol="AAPL",
        query="revenue increased",
        results=[],
        result_count=0,
        evidence_sufficient=False,
    )
    state.document_results = result
    payload = state.model_dump(mode="json")
    assert "document_results" in payload
    assert payload["document_results"]["symbol"] == "AAPL"


def test_duplicate_search_queries_are_preserved() -> None:
    state = ResearchState(symbol="AAPL", document_search_queries=["revenue increased", "Revenue Increased"])
    assert state.document_search_queries[0] == "revenue increased"
    assert len(state.document_search_queries) == 2


def test_legacy_state_without_retrieval_fields_is_valid() -> None:
    state = ResearchState(symbol="AAPL")
    assert state.ok is True
    assert state.document_results is None
    assert state.document_search_queries == []

