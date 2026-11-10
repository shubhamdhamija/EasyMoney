# Portfolio agent tools (read-only)

EasyMoney's research agent can ground a report in the user's own portfolio through exactly four
read-only tools. The application computes every number; the model only chooses which tool to call
and never calculates totals, weights or returns.

**There is no trading, transfer, deposit, withdrawal, rebalance or order capability, not even a
disabled placeholder.** A test asserts the registry contains no such tool.

## Flow

```
Android portfolio screen
  -> "Research my portfolio" button (opens a dialog only)
  -> explicit consent dialog (Continue)
  -> [CSV only] POST /portfolio/local-context        normalized, bounded subset
  -> POST /portfolio/{id}/research-consent           ~15 minute consent
  -> POST /agent/portfolio-research                  consent + ownership verified, tools run
  -> deterministic ResearchReport
  -> DELETE /portfolio/{id}/research-consent/{cid}   revoked as soon as the run ends
```

Node (`server/routes/portfolioResearchRoutes.js`) is an allow-listed pass-through to agent-service
for exactly these four routes. The agent-service owns authentication, ownership, consent, tools
and audit.

## Tools

| Tool | Purpose | Bounds |
| --- | --- | --- |
| `get_portfolio_summary` | accounts and per-currency totals | one currency summary per currency |
| `get_portfolio_positions` | positions with sort/filter | max 50 positions, returns `total_matching_positions`, `returned_position_count`, `truncated` |
| `get_portfolio_position` | one security by symbol | identity match: provider security ID, then MIC+symbol+currency, exchange+symbol+currency, name+currency; tickers are never merged blindly |
| `analyze_portfolio_concentration` | weights, top positions, allocations | exactly one currency required; max 10 top positions |

All four have `requires_approval = False` (they are read-only) and `requires_trusted_context = True`.
The registry refuses them unless an application-issued `ToolExecutionContext` is supplied.

Concentration output is neutral: weights and counts only, no "risky", "safe" or "diversified" labels.

## Trusted execution context

`ToolExecutionContext` carries `tenant_id`, `user_id`, `request_id`, `selected_portfolio_id`,
`portfolio_consent_id` and `portfolio_consent_granted`. It is built from the verified JWT principal
and a verified consent. Tool input models use `extra="forbid"`, so a model-supplied tenant, user,
consent or other-portfolio argument is rejected (`INVALID_TOOL_ARGUMENTS`), and a model-supplied
`portfolio_id` that differs from the selected portfolio is rejected (`PORTFOLIO_ACCESS_DENIED`).

## Ownership

Every repository read is `tenant_id + user_id + portfolio_id` scoped. Portfolio, account, position and
consent IDs are identifiers, never authorization. Cross-user and cross-tenant access returns the same
non-disclosing 404 as a missing portfolio. SnapTrade Sandbox portfolios reuse the existing
`PortfolioConnectionStore` ownership check (`SnapTradePortfolioResearchRepository` reads only the
already-normalized snapshot, never the provider). There is no second ownership system.

## Sources

`PortfolioSource`: `WEALTHSIMPLE_CSV`, `SNAPTRADE_SANDBOX`, `MANUAL`, `DEMO`. `SNAPTRADE_LIVE` is not
enabled. Sources are never merged.

* **SnapTrade Sandbox** is server-owned. Android sends only the opaque connection/portfolio ID in a path.
* **CSV** stays on the device. For research Android sends a normalized subset to
  `POST /portfolio/local-context`: at most **50 positions** and **256 KB** (largest market values first),
  with symbol, name, security type, account *type*, quantity, market value, currency and (only with consent)
  book value. The strict model rejects raw CSV, file names/URIs, account numbers, account hashes and device
  IDs. The context lives in process memory for 15 minutes and is never written to disk. The Node body limit
  is the default 100 KB, which comfortably fits 50 positions.

## Consent

`PortfolioResearchConsent` is request-scoped, tied to one tenant, user and portfolio, lasts about 15
minutes, can be revoked, and covers `ACCOUNTS`, `POSITIONS`, `CASH_BALANCES`, `BOOK_VALUES`. Cash and book
values degrade gracefully: without that category they are omitted (null), never zero-filled. Every tool call
re-verifies the live consent, so expiry or revocation blocks reads mid-workflow.

**Limitation:** consent records are in-memory (not shared across workers, lost on restart). That fails
closed (consent required) and is acceptable for a 15 minute, single-process development service. Move to
SQLite or a shared store before running multiple workers.

## Calculations

`app/services/portfolio_analysis_service.py` uses `Decimal`. API money values are canonical strings;
percentages are rounded to one decimal. CAD and USD are never combined. Missing values stay `null` and
raise `POSITIONS_EXCLUDED_FROM_ALLOCATION` instead of becoming zero.

Warning codes: `STALE_SNAPSHOT`, `MIXED_CURRENCIES`, `MISSING_MARKET_VALUES`,
`POSITIONS_EXCLUDED_FROM_ALLOCATION`, `MISSING_BOOK_VALUES`, `EMPTY_PORTFOLIO`,
`NO_POSITIONS_FOR_CURRENCY`, `SNAPSHOT_SOURCE_IS_CSV`, `SNAPSHOT_SOURCE_IS_SANDBOX`,
`PARTIAL_POSITION_LIST`, `UNKNOWN_SECURITY_TYPE` (plus `AMBIGUOUS_SYMBOL`).

Disclaimer on every result: *"This analysis is based on a read-only portfolio snapshot. No trades or
account changes were made. Values may be delayed or incomplete."* Reports end with *"Portfolio
information reflects the selected snapshot. No trades or account changes were made."*

## Agent integration

* `tool_execution_policy.py` is the shared gate for both the custom planner and LangGraph: portfolio
  tools need consent in state, the selected portfolio ID is pinned, bounds are capped, and exact duplicate
  portfolio requests are skipped.
* `ResearchState` has optional, bounded, JSON-safe portfolio fields (summary, up to 10 position results,
  up to 5 concentration results, request fingerprints). They hold no IDs of providers, no account numbers
  and no raw holdings beyond the bounded tool results, so they are checkpoint-safe.
* `create_portfolio_research_registry` = quote, news, optional document search, plus the four tools. The
  save-report tool stays approval-required and is off by default.
* Prompt version is `research-agent-v2`. Portfolio rules (P1-P7) are added only in portfolio mode, and the
  prompt state contains flags and counts, never holdings.
* `ResearchReport` gained optional portfolio fields. They are omitted from the JSON of stock reports, so
  existing stock responses are byte-for-byte unchanged in shape.
* API: `POST /agent/portfolio-research` takes `portfolio_context` (`portfolio_id`, `source`,
  `consent_id`, toggles, `maximum_positions`). It rejects user/tenant IDs, provider IDs, account numbers,
  hashes, holdings, secrets and CSV (`extra="forbid"`). Stock research keeps `POST /agent/research`.

## Audit and errors

Audit events: `PORTFOLIO_RESEARCH_CONSENT_GRANTED`, `_CONSENT_REVOKED`, `_RESEARCH_STARTED`,
`PORTFOLIO_TOOL_EXECUTED`, `PORTFOLIO_RESEARCH_COMPLETED`, `_FAILED`, `PORTFOLIO_ACCESS_DENIED`. Metadata is
allow-listed (portfolio ID, source, snapshot time, categories, counts, currency codes, duration). Symbols,
quantities, values, balances, account names, consent contents, prompts, JWTs and credentials are never audited.

Safe error codes with fixed messages: `PORTFOLIO_NOT_FOUND`, `PORTFOLIO_ACCESS_DENIED` (both 404),
`PORTFOLIO_CONSENT_REQUIRED`, `PORTFOLIO_CONSENT_EXPIRED` (403), `PORTFOLIO_EMPTY` (409),
`PORTFOLIO_CONTEXT_TOO_LARGE` (413), `PORTFOLIO_CURRENCY_REQUIRED` (422), `PORTFOLIO_CONTEXT_INVALID` (422).

## Tests

`tests/test_portfolio_research_api.py` covers end-to-end research, deterministic weights, no trading tools,
currency requirement, identity-argument rejection, duplicate skipping, cross-user/tenant 404, consent
revoke/expiry, book-value withholding, CSV limits and strictness, auth/scope, and stock-report shape.
Android: `PortfolioResearchViewModelTest`, `PortfolioResearchMappingTest`. Node: `portfolioResearchRoutes.test.js`.

## Not in this milestone

Real Wealthsimple, SnapTrade production, transaction history, tax advice, personalized buy/sell advice and
any brokerage write capability.
