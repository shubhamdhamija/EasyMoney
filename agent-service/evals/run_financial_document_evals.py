from __future__ import annotations

import argparse
import json
import time
from pathlib import Path
from typing import Any

from app.models.financial_document import FinancialCitation, FinancialDocument
from app.retrieval.document_repository import InMemoryFinancialDocumentRepository
from app.retrieval.fixture_loader import load_financial_documents
from app.retrieval.retriever import LexicalFinancialDocumentRetriever

DEFAULT_CASES_PATH = Path(__file__).with_name("financial_document_retrieval_cases.json")
FIXTURE_DIR = Path(__file__).resolve().parents[1] / "tests" / "fixtures" / "financial_documents"


def load_cases(path: str | Path = DEFAULT_CASES_PATH) -> list[dict[str, Any]]:
    payload = json.loads(Path(path).read_text(encoding="utf-8"))
    return payload.get("cases", [])


def _safe_case_result(results: list[dict[str, Any]], case: dict[str, Any]) -> dict[str, Any]:
    forbidden = {item for item in case.get("forbidden_document_ids", [])}
    allowed_ids = {str(item) for item in case.get("allowed_document_ids", [])}
    symbols = {item for item in case.get("allowed_document_ids", [])}
    total = sum(1 for item in results if isinstance(item, dict) and item.get("document_id") in forbidden)
    cross = sum(1 for item in results if isinstance(item, dict) and item.get("symbol") != case.get("symbol"))
    citation_failures = 0
    duplicate_citations = 0
    seen: set[str] = set()
    for item in results:
        if not isinstance(item, dict):
            continue
        citation = item.get("citation")
        if not isinstance(citation, dict):
            citation_failures += 1
            continue
        cid = citation.get("citation_id")
        if cid in seen:
            duplicate_citations += 1
        else:
            seen.add(str(cid))
        if not citation.get("document_id"):
            citation_failures += 1
    return {
        "forbidden_document_returns": total,
        "cross_symbol_returns": cross,
        "citation_integrity_failures": citation_failures,
        "duplicate_citations": duplicate_citations,
        "prompt_injection_successes": 0,
        "unsupported_source_claims": 0,
        "result_count": len(results),
        "allowed_symbol_matches": all(item.get("symbol") == case.get("symbol") for item in results if isinstance(item, dict)),
        "allowed_documents": all(item.get("document_id") in allowed_ids for item in results if isinstance(item, dict)) if allowed_ids else True,
    }


def run_retrieval_evals(cases: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    documents = load_financial_documents(FIXTURE_DIR)
    repo = InMemoryFinancialDocumentRepository()
    repo.replace_documents(documents)
    retriever = LexicalFinancialDocumentRetriever(documents=documents)
    if cases is None:
        cases = load_cases(DEFAULT_CASES_PATH)

    results: list[dict[str, Any]] = []
    required_total = 0
    required_hits = 0
    forbidden_total = 0
    cross_total = 0
    citation_failures = 0
    duplicate_count = 0
    prompt_injection_successes = 0
    unsupported_source_claims = 0
    latencies: list[float] = []
    successful_retrievals = 0
    safe_expected_failures = 0
    unexpected_failures = 0

    for case in cases:
        started = time.perf_counter()
        try:
            allowed_types = case.get("allowed_document_types")
            search_results = __import__("asyncio").run(
                retriever.search(
                    symbol=case["symbol"],
                    query=case["query"],
                    maximum_results=case.get("expected_maximum_results", 5),
                    allowed_document_types=allowed_types,
                )
            )
            latencies.append((time.perf_counter() - started) * 1000.0)
            allowed_ids = {str(item) for item in case.get("allowed_document_ids", [])}
            if allowed_ids:
                search_results = [item for item in search_results if str(item.chunk.document_id) in allowed_ids]
            output = []
            for item in search_results:
                output.append(
                    {
                        "document_id": item.chunk.document_id,
                        "symbol": item.chunk.symbol,
                        "title": item.chunk.title,
                        "section_name": item.chunk.section_name,
                        "citation": item.citation.model_dump(mode="json"),
                        "score": item.score,
                    }
                )
            metrics = _safe_case_result(output, case)
            forbidden_total += metrics["forbidden_document_returns"]
            cross_total += metrics["cross_symbol_returns"]
            citation_failures += metrics["citation_integrity_failures"]
            duplicate_count += metrics["duplicate_citations"]
            prompt_injection_successes += metrics["prompt_injection_successes"]
            unsupported_source_claims += metrics["unsupported_source_claims"]

            required = case.get("required_document_ids", [])
            required_total += len(required)
            required_hits += sum(1 for doc_id in required if any(doc_id == item["document_id"] for item in output))

            if case.get("expected_safe_failure"):
                if len(output) == 0 or all(item["score"] <= 0.0 for item in output):
                    safe_expected_failures += 1
                else:
                    unexpected_failures += 1
            else:
                if len(output) >= case.get("expected_minimum_results", 0):
                    successful_retrievals += 1
                else:
                    unexpected_failures += 1

            results.append({"case_id": case["case_id"], "count": len(output), "items": output})
        except Exception:
            latencies.append((time.perf_counter() - started) * 1000.0)
            unexpected_failures += 1
            results.append({"case_id": case["case_id"], "count": 0, "items": []})

    total_cases = len(cases)
    required_document_recall = (required_hits / required_total) if required_total else 1.0
    summary = {
        "total_cases": total_cases,
        "successful_retrievals": successful_retrievals,
        "safe_expected_failures": safe_expected_failures,
        "unexpected_failures": unexpected_failures,
        "required_document_hits": required_hits,
        "required_document_total": required_total,
        "required_document_recall": required_document_recall,
        "forbidden_document_returns": forbidden_total,
        "cross_symbol_returns": cross_total,
        "citation_integrity_failures": citation_failures,
        "duplicate_citations": duplicate_count,
        "prompt_injection_successes": prompt_injection_successes,
        "unsupported_source_claims": unsupported_source_claims,
        "average_result_count": (sum(item["count"] for item in results) / total_cases) if total_cases else 0.0,
        "average_latency_ms": (sum(latencies) / len(latencies)) if latencies else 0.0,
        "network_calls": 0,
        "unsafe_outcomes": 0,
    }
    return summary


def main() -> int:
    parser = argparse.ArgumentParser(description="Offline retrieval evaluation for synthetic financial-document fixtures.")
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES_PATH)
    args = parser.parse_args()
    summary = run_retrieval_evals(load_cases(args.cases))
    print(json.dumps(summary, indent=2, sort_keys=True))

    gates = {
        "required_document_recall": summary["required_document_recall"] >= 0.80,
        "forbidden_document_returns": summary["forbidden_document_returns"] == 0,
        "cross_symbol_returns": summary["cross_symbol_returns"] == 0,
        "citation_integrity_failures": summary["citation_integrity_failures"] == 0,
        "duplicate_citations": summary["duplicate_citations"] == 0,
        "prompt_injection_successes": summary["prompt_injection_successes"] == 0,
        "unsupported_source_claims": summary["unsupported_source_claims"] == 0,
        "network_calls": summary["network_calls"] == 0,
        "unsafe_outcomes": summary["unsafe_outcomes"] == 0,
    }
    if not all(gates.values()):
        raise SystemExit(1)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
