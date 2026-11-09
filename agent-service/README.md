# EasyMoney agent-service

Bounded research agent (decide -> authorize -> validate -> execute -> observe -> finish)
with a deterministic tool registry, fake provider transport, offline evaluations, and a
synthetic financial-document retrieval layer.

Live agent calls remain **disabled by default**.

## Retrieval milestone

The agent may choose to use `search_financial_documents` when company disclosures,
operating data, risk factors, or guidance are relevant. The application owns the document
corpus, the symbol validation, the chunking, the ranking, and the citations. The model
never creates or mutates document IDs, chunk IDs, citation IDs, titles, filing types,
source URLs, accession numbers, publication dates, or source names.

The agent decides whether retrieval is needed.
The application controls the corpus.
The retriever returns bounded validated chunks.
The application creates citations from document metadata.
The deterministic report builder presents only retrieved evidence.

## Verification status

### Completed

- 318+ automated tests
- 10 fake evaluation cases
- retrieval evaluation runner for synthetic documents
- offline synthetic corpus validation
- deterministic chunking and lexical retrieval
- safe citation construction and evidence summaries
- zero network calls in automated tests
- zero prompt-injection successes in retrieval and evaluation
- disabled-mode HTTP verification
- request-ID propagation and sanitization

### Pending

- Controlled Groq smoke test
- Live agent evaluation
- Manual review of live tool-selection behaviour

Live runs need a key you create and keep in your local, git-ignored `agent-service/.env`.
Never put it in chat, source, tests, screenshots, evaluation reports or shell history.

## Evaluations

See `evals/README.md` and `docs/RAG_EVALUATION.md`.

## LangGraph comparison

`app/langgraph_agent/` re-implements the same loop as a LangGraph graph for learning and
comparison. The custom `ModelDrivenResearchPlanner` remains the reference implementation.
