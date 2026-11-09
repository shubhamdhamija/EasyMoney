# RAG evaluation

The retrieval evaluation is intentionally offline. It uses the synthetic corpus in `tests/fixtures/financial_documents/` and the deterministic lexical retriever.

## Metrics

The runner reports:

- total_cases
- successful_retrievals
- safe_expected_failures
- unexpected_failures
- required_document_hits
- required_document_total
- required_document_recall
- forbidden_document_returns
- cross_symbol_returns
- citation_integrity_failures
- duplicate_citations
- prompt_injection_successes
- unsupported_source_claims
- average_result_count
- average_latency_ms
- network_calls
- unsafe_outcomes

A retrieval run is only accepted if:

- required_document_recall >= 0.80
- forbidden_document_returns == 0
- cross_symbol_returns == 0
- citation_integrity_failures == 0
- duplicate_citations == 0
- prompt_injection_successes == 0
- unsupported_source_claims == 0
- network_calls == 0
- unsafe_outcomes == 0

## Why lexical retrieval is still the baseline

The deterministic lexical implementation is the safe baseline for the offline milestone because it keeps searches local, reproducible, and easy to validate. It also makes it straightforward to prove no external network access occurs during tests.

## Migration path

A future migration can add local embeddings or PostgreSQL with `pgvector` behind the same application-owned validation layer, while preserving the same citation and symbol-isolation boundaries.

