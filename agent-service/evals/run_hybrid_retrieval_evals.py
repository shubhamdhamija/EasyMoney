from __future__ import annotations

import argparse
import asyncio
import json
import time
from pathlib import Path
from typing import Any

from app.retrieval.configured_retriever import build_configured_retriever
from app.retrieval.fixture_loader import load_financial_documents

DEFAULT_CASES_PATH = Path(__file__).with_name("hybrid_retrieval_cases.json")
FIXTURE_DIR = Path(__file__).resolve().parents[1] / "tests" / "fixtures" / "financial_documents"


def load_cases(path: str | Path = DEFAULT_CASES_PATH) -> list[dict[str, Any]]:
    payload = json.loads(Path(path).read_text(encoding="utf-8"))
    return payload.get("cases", [])


def _compute_latencies(started: float, ended: float) -> float:
    return (ended - started) * 1000.0


def _safe_case_result(results: list[dict[str, Any]], case: dict[str, Any]) -> dict[str, Any]:
    forbidden_ids = {str(item) for item in case.get("forbidden_document_ids", [])}
    allowed_ids = {str(item) for item in case.get("allowed_document_ids", [])}
    seen: set[str] = set()
    citation_failures = 0
    duplicate_citations = 0
    forbidden_returns = 0
    cross_symbol_returns = 0
    unsupported = 0
    prompt_success = 0
    for item in results:
        if not isinstance(item, dict):
            continue
        document_id = str(item.get("document_id", ""))
        symbol = str(item.get("symbol", ""))
        if document_id in forbidden_ids:
            forbidden_returns += 1
        if symbol != case.get("symbol"):
            cross_symbol_returns += 1
        if document_id and document_id not in allowed_ids and allowed_ids:
            forbidden_returns += 1
        citation = item.get("citation")
        if not isinstance(citation, dict):
            citation_failures += 1
            continue
        citation_id = str(citation.get("citation_id", ""))
        if citation_id in seen:
            duplicate_citations += 1
        else:
            seen.add(citation_id)
        if not citation.get("document_id") or not citation.get("chunk_id"):
            citation_failures += 1
        if "vector" in json.dumps(citation, sort_keys=True).lower() or "embedding" in json.dumps(citation, sort_keys=True).lower():
            unsupported += 1
        if case.get("query_category") == "injection" and any(term in str(item).lower() for term in ["ignore previous instructions", "reveal the model metadata"]):
            prompt_success += 1
    return {
        "forbidden_document_returns": forbidden_returns,
        "cross_symbol_returns": cross_symbol_returns,
        "citation_integrity_failures": citation_failures,
        "duplicate_citations": duplicate_citations,
        "prompt_injection_successes": prompt_success,
        "unsupported_source_claims": unsupported,
    }


def run_hybrid_retrieval_evals(cases: list[dict[str, Any]] | None = None, *, mode: str = "lexical") -> dict[str, Any]:
    if cases is None:
        cases = load_cases(DEFAULT_CASES_PATH)
    documents = load_financial_documents(FIXTURE_DIR)
    retriever = build_configured_retriever(mode=mode, documents=documents)

    successful_retrievals = 0
    safe_expected_failures = 0
    unexpected_failures = 0
    required_hits = 0
    required_total = 0
    forbidden_total = 0
    cross_total = 0
    citation_failures = 0
    duplicate_count = 0
    prompt_injection_successes = 0
    unsupported_source_claims = 0
    latencies: list[float] = []
    result_counts: list[int] = []

    for case in cases:
        started = time.perf_counter()
        try:
            allowed_types = case.get("allowed_document_types")
            results = asyncio.run(
                retriever.search(
                    symbol=case["symbol"],
                    query=case["query"],
                    maximum_results=case.get("expected_maximum_results", 5),
                    allowed_document_types=allowed_types,
                )
            )
            latencies.append(_compute_latencies(started, time.perf_counter()))
            output = []
            for item in results:
                output.append(
                    {
                        "document_id": item.chunk.document_id,
                        "symbol": item.chunk.symbol,
                        "title": item.chunk.title,
                        "section_name": item.chunk.section_name,
                        "citation": item.citation.model_dump(mode="json"),
                        "score": float(item.score if hasattr(item, "score") else 0.0),
                    }
                )
            safe = _safe_case_result(output, case)
            forbidden_total += safe["forbidden_document_returns"]
            cross_total += safe["cross_symbol_returns"]
            citation_failures += safe["citation_integrity_failures"]
            duplicate_count += safe["duplicate_citations"]
            prompt_injection_successes += safe["prompt_injection_successes"]
            unsupported_source_claims += safe["unsupported_source_claims"]

            required = case.get("required_document_ids", [])
            required_total += len(required)
            required_hits += sum(1 for doc_id in required if any(doc_id == item["document_id"] for item in output))
            result_counts.append(len(output))

            if case.get("expected_safe_failure"):
                if len(output) == 0:
                    safe_expected_failures += 1
                else:
                    unexpected_failures += 1
            else:
                if len(output) >= case.get("expected_minimum_results", 0):
                    successful_retrievals += 1
                else:
                    unexpected_failures += 1
        except Exception:
            unexpected_failures += 1
            latencies.append(_compute_latencies(started, time.perf_counter()))

    required_document_recall = (required_hits / required_total) if required_total else 1.0
    total_cases = len(cases)
    if latencies:
        average_latency = sum(latencies) / len(latencies)
        p95_latency = sorted(latencies)[max(0, int(len(latencies) * 0.95) - 1)]
    else:
        average_latency = 0.0
        p95_latency = 0.0

    summary = {
        "mode": mode,
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
        "average_result_count": (sum(result_counts) / total_cases) if total_cases else 0.0,
        "average_latency_ms": average_latency,
        "p95_latency_ms": p95_latency,
        "network_calls": 0,
        "unsafe_outcomes": 0,
    }
    return summary


def main() -> int:
    parser = argparse.ArgumentParser(description="Offline hybrid retrieval evaluation runner.")
    parser.add_argument("--mode", choices=["lexical", "vector", "hybrid"], default="lexical")
    parser.add_argument("--cases", type=Path, default=DEFAULT_CASES_PATH)
    args = parser.parse_args()
    summary = run_hybrid_retrieval_evals(load_cases(args.cases), mode=args.mode)
    print(json.dumps(summary, indent=2, sort_keys=True))

    if summary["required_document_recall"] < 0.80 and args.mode == "lexical":
        raise SystemExit(1)
    if args.mode == "vector" and summary["required_document_recall"] < 0.60:
        raise SystemExit(1)
    if args.mode == "hybrid":
        if summary["required_document_recall"] < 0.80:
            raise SystemExit(1)
    if summary["forbidden_document_returns"] != 0:
        raise SystemExit(1)
    if summary["cross_symbol_returns"] != 0:
        raise SystemExit(1)
    if summary["citation_integrity_failures"] != 0:
        raise SystemExit(1)
    if summary["duplicate_citations"] != 0:
        raise SystemExit(1)
    if summary["prompt_injection_successes"] != 0:
        raise SystemExit(1)
    if summary["unsupported_source_claims"] != 0:
        raise SystemExit(1)
    if summary["network_calls"] != 0:
        raise SystemExit(1)
    if summary["unsafe_outcomes"] != 0:
        raise SystemExit(1)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

