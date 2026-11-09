# RAG architecture

The agent decides whether retrieval is needed. The application controls the corpus, the symbol constraints, the chunking strategy, the ranker, the citation rules, and the final evidence presentation.

## Synthetic corpus

The retrieval milestone uses a small synthetic financial-document corpus under `tests/fixtures/financial_documents/`. Every document is synthetic, offline, and deterministic. The data set exists only for automated tests and evaluation. It deliberately excludes real SEC filing text and does not provide investment advice.

## Fixture validation

`app/retrieval/fixture_loader.py` validates the supplied directory, rejects unsupported extensions, rejects invalid JSON, rejects duplicate IDs, checks output shape, validates the `FinancialDocument` model, and verifies or computes checksums deterministically.

## Deterministic chunking

`DocumentChunker` preserves section boundaries when possible, prefers paragraph boundaries, splits oversized paragraphs safely, and never creates blank chunks. Chunk IDs are generated from the document id, section name, chunk index, and normalized payload text.

## Lexical retrieval

`LexicalFinancialDocumentRetriever` performs a bounded lexical search over symbol-specific documents. Query normalization strips punctuation, removes a small default stop-word set, and scores based on query coverage, title relevance, section-name relevance, and exact phrase matches. Scores are clamped to the [0, 1] interval.

## Citation construction

The retriever builds citations only from document metadata already present in the source chunk. Citation IDs are deterministic and are never created by the model. The final report builder may include only those citations it received from retrieval.

## Prompt injection

Document text is treated as untrusted data. The workflow does not follow instructions found inside the fixture text, and document content is never allowed to affect tool selection, approval, registry authorization, or citation metadata.

## Deterministic evidence reporting

The application-owned report builder assembles a concise evidence summary from validated retrieved chunks and attaches only deterministic citations. It never exposes full document content, checkpoint state, raw prompts, or hidden reasoning.

## Limitations and future path

This is a lexical-only system intended for offline, deterministic tests. It is intentionally not a live SEC-ingestion system, not a general-purpose RAG stack, and not a live pricing engine. The future path can include local embeddings, PostgreSQL + pgvector, or a controlled SEC EDGAR ingestion pipeline behind the same application-owned validation layer.

