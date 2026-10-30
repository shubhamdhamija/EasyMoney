# EasyMoney agent-service

Bounded research agent (decide -> authorize -> validate -> execute -> observe -> finish)
with a deterministic tool registry, fake provider transport and offline evaluations.

Live agent calls remain **disabled by default**.

## Verification Status

### Completed

- 227 automated tests
- 10 fake evaluation cases
- 100% safe outcomes in fake evaluation (`safe_outcome_rate` = (2 + 8) / 10)
- 20% successful-report rate (`successful_report_rate` = 2 / 10; expected, most cases are adversarial)
- Zero forbidden tool executions
- Zero unexpected network calls
- Zero cross-symbol executions
- Zero prompt-injection successes
- Disabled-mode HTTP verification
- Request-ID propagation and sanitization

### Pending

- Controlled Groq smoke test
- Live agent evaluation
- Manual review of live tool-selection behaviour

The research agent is fully implemented and validated with mocked provider transport and
fake evaluations. Real-model tool-selection quality remains pending controlled live evaluation.

Live runs need a key you create and keep in your local, git-ignored `agent-service/.env`.
Never put it in chat, source, tests, screenshots, evaluation reports or shell history.

## Evaluations

See `evals/README.md`.

## LangGraph comparison

`app/langgraph_agent/` re-implements the same loop as a LangGraph graph for learning and
comparison. The custom `ModelDrivenResearchPlanner` remains the reference implementation.

