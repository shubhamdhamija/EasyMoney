# Wealthsimple Holdings CSV Import

**EasyMoney imports a read-only portfolio snapshot.**

**EasyMoney does not receive brokerage credentials and cannot place trades,
transfer funds, or modify a Wealthsimple account.**

## Optional alternative source: SnapTrade Sandbox

CSV import is the default method. SnapTrade Sandbox (development-only) is an optional alternative source; see
[SNAPTRADE_SANDBOX.md](SNAPTRADE_SANDBOX.md).

- EasyMoney requests a read-only connection.
- EasyMoney has no order, transfer, deposit or withdrawal capability.
- SnapTrade Sandbox uses simulated data and does not connect a real brokerage account.
- CSV and sandbox portfolios are displayed separately and never merged.

## Supported report
Wealthsimple "holdings report" CSV. Required headers (case-insensitive, BOM/whitespace trimmed):
Account Name, Account Type, Account Classification, Account Number, Symbol, Exchange, MIC, Name,
Security Type, Quantity, Position Direction, Market Price, Market Price Currency, Book Value (CAD),
Book Value Currency (CAD), Book Value (Market), Book Value Currency (Market), Market Value,
Market Value Currency, Market Unrealized Returns, Market Unrealized Returns Currency.
Extra columns are ignored with a warning. Unrelated CSVs are rejected.

## Flow
Storage Access Framework (`OpenDocument`, no storage permission) → bounded read (10 MB) →
`WealthsimpleHoldingsCsvParser` → `PortfolioImportPreview` → explicit confirmation →
Room → Portfolio screen. Everything runs on-device. The raw CSV is never uploaded, copied to
app storage, or persisted; the temporary buffer is zeroed after parsing. URI permission is not persisted.

## Parsing rules
- Own RFC-4180 reader (quotes, embedded commas, escaped quotes, CRLF/LF).
- Final `As of 2026-09-09 10:26 GMT-04:00` line is the snapshot time (explicit offset → `Instant`),
  never a holding. Missing/invalid → import time + `MISSING_/INVALID_SNAPSHOT_TIMESTAMP`.
- `CURRENCY` rows become cash balances (symbol = currency, quantity = amount).
- Quantities and money use `BigDecimal` only; fractional quantities are exact. NaN/Infinity/scientific
  notation and out-of-bounds values reject the row. Valid rows continue after a bad row.
- Blank market price/value/book value produce warnings, not failures. `EXCHANGE\_TRADED\_FUND` is normalized.
- Currencies are never combined; CAD and USD totals, weights and sorting stay separate (no FX).

## Privacy
- Raw account number is hashed (SHA-256 over broker + app salt + number) and masked (`••••1234`) immediately
  and never stored, logged, or shown. Nothing financial is logged.
- Security identity key: `MIC|symbol|currency` (e.g. `XNAS|AAPL|USD`, `XTSE|WCN.TO|CAD`, `CASH|CAD`).

## Fingerprint, duplicates, replacement
- Fingerprint = SHA-256 of canonical JSON (sorted keys, compact, plain decimals, account hashes only).
- Identical fingerprint → "This portfolio snapshot has already been imported." (import disabled).
- Status: FIRST_IMPORT / NEWER_SNAPSHOT / OLDER_SNAPSHOT / DIFFERENT_ACCOUNT / IDENTICAL_IMPORT.
- A snapshot replaces stored accounts sharing the same account hash, in one Room transaction (failure keeps
  previous data). Older snapshots need explicit "Import as active snapshot". No row-level merging.
- Exact duplicate rows are collapsed (`DUPLICATE_POSITION`); conflicting duplicates are rejected, not summed.

## Storage
Room v3 (migration 2→3): `portfolio_snapshots`, `portfolio_accounts`, `portfolio_positions`,
`portfolio_cash_balances`. Decimals are strings, instants are epoch millis, rows are scoped by owner id
(the existing device identity). Delete portfolio data removes everything after confirmation.

## Not included
No trading, transfers, credentials, unofficial Wealthsimple APIs, background sync, or SnapTrade (deferred).

## AI research (pending)
Android-to-agent portfolio context and read-only agent tools (`get_portfolio_summary`,
`get_portfolio_positions`, `get_portfolio_position`, `analyze_portfolio_concentration`) are not implemented yet.
They require a backend ownership contract and the consent text: "EasyMoney will use your imported
holdings to prepare portfolio research. No trades or account changes will be made." Weights must be computed
by `PortfolioSummaryCalculator`, never by an LLM.

## Known limitations
Conflicting duplicates are rejected rather than user-resolved; no FX totals; app-level salt is constant;
no history of prior snapshots; snapshots with missing timestamps get a new fingerprint on each import.
