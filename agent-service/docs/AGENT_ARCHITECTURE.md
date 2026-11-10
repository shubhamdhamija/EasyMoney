# Agent architecture

The agent-service runs a bounded, tool-using research workflow. The model only **proposes** the next
action; the application decides whether it runs.

```
decide (model) -> tool_execution_policy -> ToolRegistry.execute -> apply_tool_result -> decide ... -> finish
                                                                                              |
                                                                       research_report_builder (deterministic)
```

Two orchestrators share the same public pieces and cannot drift apart:

* `ModelDrivenResearchPlanner` (reference implementation)
* `app/langgraph_agent/research_graph.py` (LangGraph, with resumable approval pauses)

Shared: `tool_execution_policy`, `ToolRegistry`, the decision model, `research_report_builder`.

## Guards

* Decision budget and tool-call budget.
* Duplicate protection, symbol pinning, news cap (stock research).
* Registry allowlist; approval-required tools need a single-use human approval.
* Tool results and news text are untrusted data.

## Portfolio research (read-only)

Portfolio mode adds, without changing the stock flow:

* `ToolExecutionContext` — application-issued trusted identity (tenant, user, request, selected portfolio,
  consent). The model cannot create or alter it. It travels beside the research state (LangGraph:
  `GraphState.execution_context`), never inside model arguments.
* Policy outcomes `consent_required` and `portfolio_rejected`, selected-portfolio pinning, bounds capping,
  and portfolio request fingerprints for duplicate skipping.
* `ResearchState` portfolio fields (bounded, JSON-safe, checkpoint-safe) and `finish_error`, which requires
  portfolio evidence instead of a quote when in portfolio mode.
* `create_portfolio_research_registry` with the four read-only portfolio tools.
* Prompt `research-agent-v2` with portfolio rules only in portfolio mode.
* A deterministic portfolio branch in `research_report_builder`.

Both orchestrators expose `research_portfolio(execution_context, ...)`. The HTTP route uses the custom
planner. See [PORTFOLIO_AGENT_TOOLS.md](PORTFOLIO_AGENT_TOOLS.md) for the tool contracts, consent,
ownership, audit and error codes.

## Boundary

No brokerage write capability exists. Nothing in this service can place, preview, cancel or replace an
order, move money, or rebalance.
