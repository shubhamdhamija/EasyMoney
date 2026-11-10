# Android Agent Integration

## Overview

Android presents agent results and collects explicit human approval.

Android does not choose tools, execute tools, create citations, authorize backend writes, or call Groq directly.

This Android app integrates with the EasyMoney agent service for stock research.

## Stock Detail integration

- `StockDetailScreen` owns the screen-level tab state and the selected `PremiumSection` via `rememberSaveable`.
- `AgentResearchViewModel` is the screen-scoped owner for current symbol, research request, loading state, pending approval, retry, dismiss, and approval submission.
- Tapping `Research with AI` switches the selected section to `PremiumSection.AI` and initiates a single agent request only when the state is not already loading.
- The action stays explicit. Selecting the AI tab alone does not trigger a network call.

## Research with AI action

The hero card includes a visible `Research with AI` action in the same chip row as the existing stock actions.

Behavior:

- set `selectedSectionName = PremiumSection.AI.name`
- call `agentResearchViewModel.startAgentResearch(stock.symbol)` exactly once per user tap
- ignore repeated taps while `AgentResearchUiState.Loading` is active
- disable or show progress while loading
- preserve the selected section and result across configuration changes

## State ownership

The Android app keeps agent state in the existing `AgentResearchViewModel` rather than duplicating it in `StockDetailViewModel`.

This ensures:

- current symbol
- agent loading state
- success report
- pending approval
- retry
- dismiss
- approval submission

all remain in one clear owner while `StockDetailScreen` only controls the lightweight UI selection state.

## Agent state rendering

The AI section renders every `AgentResearchUiState` variant exhaustively:

- Idle: no empty card; the hero action remains available.
- Loading: `AI Research` plus `Researching available information...` with a progress indicator.
- ApprovalRequired: `AgentApprovalSheet` is shown with read-only symbol and summary and explicit approve/reject actions.
- Success: `AgentResearchCard` is rendered with the report summary, evidence, and citations.
- Error: `AgentErrorCard` is rendered with a safe message, retry when allowed, and request ID shown if provided.

## Approval callbacks

Approval actions are wired to the exact pending approval in the ViewModel model:

- Approve and save → `viewModel.approvePendingAction()`
- Not now → `viewModel.rejectPendingAction()`

The UI does not reconstruct IDs from Compose. Buttons are disabled while submission is active.

Rejected or expired approvals are not presented as successful saves. Terminal backend results clear the pending approval state.

## Retry and dismiss behavior

- Retry only triggers a technical research retry from `AgentResearchUiState.Error`.
- Retry reuses the current symbol and creates a new research request only when safe.
- Dismiss clears only the agent research UI state; it does not clear stock explanation, AI insight, or market data.
- The user remains on the AI section unless the existing conventions say otherwise.

## Citation links

Citation links use the existing URI-opening abstraction and only allow valid `https://` URLs.

The app rejects or renders non-clickable:

- `http://`
- `javascript:`
- `file:`
- `content:`
- custom schemes
- malformed URLs
- blank values

When a source link is unavailable, the UI still shows citation metadata such as:

- title
- document type
- section name
- source name
- publication date
- optional accession number

No raw chunk text, vector scores, lexical scores, fused scores, or embedding metadata is shown.

## Request IDs and safety

The app shows request IDs only in safe error UI when present.

It intentionally does not expose:

- HTTP status codes
- raw JSON
- provider names
- Groq details
- JWT or token data
- SQLite internals
- stack traces
- tenant or user identifiers

## Local emulator URL

The Android app uses the configured build network configuration. For local emulator testing, the backend target is:

- `http://10.0.2.2:8090/`

Production builds must remain HTTPS with no cleartext traffic.

## Duplicate-request prevention

The app avoids duplicate requests by keeping network calls behind explicit user actions only:

- `Research with AI`
- `Retry`
- `Approve and save`
- `Not now`

There is no automatic research start from screen recomposition, tab selection, theme change, font-scale change, or process restore.

## Configuration-change behavior

`selectedSectionName` is saved by name with `rememberSaveable` so the selected tab survives configuration changes.

The current agent report and loading state remain in the ViewModel as the primary source of truth.

## Manual checks completed

- Verified the agent state is wired to the real Stock Detail screen.
- Verified the `Research with AI` action navigates to the AI section.
- Verified the action starts a single request only when explicitly triggered.
- Verified the app renders loading, success, error, and approval-required states using the existing agent state model.
- Verified citations render only through safe HTTPS links.

## Manual checks pending

- Device rotation with active agent flow
- Dark/light theme changes
- Font-scale changes
- Background/resume behavior
- Approval submit once, reject once, and expired approval behavior
- Long text wrapping and approval scrollability on real device
- Live backend verification when the local agent service is running

## Process death limitation

Pending approval cannot fully survive a process death under the current Android API without a backend resume workflow. The app does not invent a resume endpoint or store sensitive approval data in `SavedStateHandle`.

## Live Groq verification

Live Groq verification remains pending. Android does not make direct provider calls.

## Lexical retrieval default

Lexical retrieval remains the default if hybrid retrieval remains experimental.

## Current limitation statement

The Android app integrates the existing agent research flow into the real stock detail screen without altering the backend contracts, retrieval pipeline, or LangGraph workflow.

## Portfolio research (read-only, explicit consent)

The Portfolio screen shows **Research my portfolio** for the active source. Pressing it only opens a consent dialog; nothing is sent on screen open, rotation or recomposition.

* Dialog: title, body ("EasyMoney will use your selected portfolio snapshot to prepare research. No trades or account changes will be made."), source, snapshot time, toggles (Accounts and Holdings are required and on, Cash on, Book values off by default), Cancel and Continue.
* `PortfolioResearchRepository`: `createResearchConsent`, `startPortfolioResearch`, `revokeResearchConsent`. SnapTrade Sandbox portfolios send only the opaque ID in a path. A CSV portfolio sends a normalized subset (max 50 positions, largest first; account type only, no account numbers, hashes, names, file URIs or raw CSV; book value only with consent) to `POST /portfolio/local-context`.
* `PortfolioResearchViewModel` states: Idle, ConsentRequired, CreatingConsent, Researching, Success, Error. It uses no `SavedStateHandle`, holds no consent ID in UI state, revokes consent when a run ends, and a retry reopens the dialog (never silently re-sends).
* The result card shows source, snapshot time, per-currency summaries (CAD and USD separate), holdings, concentration, evidence, warnings and "No trades or account changes were made." It never shows connection IDs, account numbers, hashes, consent IDs, model name, prompt, tool-call count, reasoning or vector scores, and has no trading controls.
* The JWT used by the existing `AgentAuthInterceptor` needs `portfolio:read`.

Details: `agent-service/docs/PORTFOLIO_AGENT_TOOLS.md`.
