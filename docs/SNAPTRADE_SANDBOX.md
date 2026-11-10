# SnapTrade Sandbox (development-only, read-only)

> **EasyMoney requests a read-only connection.**
> EasyMoney has no order, transfer, deposit or withdrawal capability.
> **SnapTrade Sandbox uses simulated data and does not connect a real brokerage account.**

## Scope

- Development-only. Environment accepts only `sandbox`; connection type accepts only `read`; broker is `SANDBOX`.
- Disabled by default (`SNAPTRADE_ENABLED=false`). Disabled responses: HTTP 503, code `SNAPTRADE_DISABLED`,
  message "Automatic portfolio connection is currently unavailable."
- No real Wealthsimple connection. Production access is deferred until SnapTrade confirms read-only Wealthsimple
  access for the EasyMoney production application. A live connector needs a separate review and separate consent text.
- Provider costs are not assumed.
- CSV import (see `PORTFOLIO_CSV_IMPORT.md`) remains the default import method.

## Server owner

The **agent-service (FastAPI)** owns the integration, because it already owns JWT verification, tenant/user
principals, scopes, SQLite persistence and audit events. The Node.js server has no auth layer, so it only provides
an **allow-listed pass-through** (`server/routes/portfolioConnectionRoutes.js`) that forwards the `Authorization`
header. No SnapTrade logic or credentials exist in Node or Android.

## Configuration (`agent-service/.env`, see `.env.example`)

`SNAPTRADE_ENABLED`, `SNAPTRADE_CLIENT_ID`, `SNAPTRADE_CONSUMER_KEY`, `SNAPTRADE_ENVIRONMENT=sandbox`,
`SNAPTRADE_REDIRECT_URI=easymoney://portfolio/snaptrade`, `SNAPTRADE_CONNECTION_TYPE=read`,
`SNAPTRADE_BROKER=SANDBOX`, `SNAPTRADE_TIMEOUT_SECONDS=20`, `SNAPTRADE_ENCRYPTION_KEY` (Fernet key).

Startup fails if the connection type is not `read` (e.g. `trade`, `trade-if-available`) or the environment is not
`sandbox`. Secrets are `SecretStr`, never logged or returned.

## Flow

1. Android `POST /portfolio/connections/snaptrade/session` with only `{"redirectUri"}`.
2. Backend registers the provider user (opaque ID `easymoney_sandbox_<hmac>`; secret encrypted at rest) and calls
   SnapTrade login with `connectionType=read`, `broker=SANDBOX`, `immediateRedirect=true`, `customRedirect=<configured>`.
3. Portal URL (HTTPS, host validated as `*.snaptrade.com`, never stored) is returned; Android opens it in
   **Custom Tabs** (no WebView, nothing inspected).
4. SnapTrade redirects to `easymoney://portfolio/snaptrade`. `MainActivity` (singleTop) validates scheme, host,
   path, status and that a session is pending; the callback is consumed once.
5. Android calls `POST /portfolio/connections/snaptrade/{sessionId}/complete`. The callback is **not** proof: the
   backend asks SnapTrade for the user's connections, detects the new one, stores it and runs one bounded sync.

## Endpoints (all JWT; scope `portfolio:connect` or `portfolio:read`; cross-user/tenant = 404)

`POST /portfolio/connections/snaptrade/session`, `POST .../snaptrade/{sessionId}/complete`,
`GET /portfolio/connections`, `GET /portfolio/connections/{id}/portfolio`, `POST .../{id}/sync`,
`POST .../{id}/reconnect-session`, `DELETE .../{id}` (disconnect), `DELETE .../{id}/data?confirm=true`.

## Read-only enforcement

- `PortfolioProviderCapabilityPolicy` allows only read/connect/refresh/disconnect/delete-data capabilities.
- `SnapTradePortfolioConnectionProvider` can reach only a fixed allow-list of SnapTrade endpoints (registerUser,
  login, authorizations, accounts, balances, positions, refresh, delete authorization). Any other path is refused
  before a network call. Direct signed HTTP is used (HMAC-SHA256 per SnapTrade docs) instead of the SDK so no trading
  API is ever importable.
- No order/trade/transfer models, services, tools or UI exist. Sandbox orders and transactions are ignored.

## Normalization

Accounts map to `TFSA, RRSP, FHSA, RESP, LIRA, PERSONAL, CRYPTO, CASH, UNKNOWN` by exact match only; numbers are
masked server-side (`••••1234`). Security identity: provider security ID → MIC+ticker+currency →
exchange+ticker+currency → name+currency; tickers alone never merge. Decimals are serialized as strings; a missing
price yields a null market value (never 0). CAD and USD stay separate.

## Sync

Per-connection lock (no concurrent sync), overall timeout = 3× provider timeout, idempotent snapshot ID from a
content fingerprint, atomic snapshot swap + `READY`. A failed or partially failed refresh keeps the previous
snapshot. Provider 401/403 → `RECONNECT_REQUIRED`; 429/5xx → temporary-unavailable (no retry loop).

## Android

`PortfolioConnectionRepository` (in-memory synced portfolio, no Room, no secrets), `PortfolioConnectionViewModel`,
`PortfolioDeepLinkHandler`, consent dialog, connection card, details sheet, disconnect/delete confirmations.
Snapshots carry `PortfolioSource` (`SNAPTRADE_SANDBOX` only; `SNAPTRADE_LIVE` is never exposed).

## CSV coexistence

Sources are displayed separately and never merged; when both exist the user picks one. Disconnecting the sandbox
does not touch the CSV snapshot.

## Privacy and audit

Audit events: `PORTFOLIO_CONNECTION_SESSION_CREATED`, `_CONNECTION_COMPLETED`, `_CONNECTION_FAILED`,
`_SYNC_STARTED`, `_SYNC_COMPLETED`, `_SYNC_FAILED`, `_RECONNECT_REQUIRED`, `_DISCONNECTED`, `_DATA_DELETED`.
Fields: request ID, user, tenant, connection ID, provider, environment, outcome, error code, duration.
Never logged: holdings, balances, quantities, account numbers, secrets, portal URL, credentials.

## Agent boundary

The agent does not use SnapTrade. A later milestone may expose normalized, consented, read-only portfolio tools.

## Development authentication

- Debug builds only. `DevelopmentAgentAccessTokenProvider` reads `AGENT_DEV_JWT` from the git-ignored
  `local.properties` (exposed as a debug-only `BuildConfig` field; release builds get an empty value and send no token).
- Mint a short-lived token (45 min, scopes `portfolio:connect portfolio:read`, subject `local-sandbox-user`, tenant
  `local-development`) with `cd agent-service && python -m scripts.mint_dev_jwt`, then paste the `AGENT_DEV_JWT=` line
  into `local.properties`. Use a random `AGENT_JWT_SECRET` in the ignored `agent-service/.env`.
- The token is never committed, logged or documented. Provider credentials stay in `agent-service/.env` only.
- Sandbox credentials must be dedicated Sandbox keys. A SnapTrade *Personal* key (client ID starting `PERS-`) must not
  be used with this integration.

## Live validation (SnapTrade Sandbox)

- **Date:** 2026-11-09. **Environment:** local; agent-service (8090) behind the Node proxy (8080); Android emulator
  `emulator-5554` reaching the host at `10.0.2.2`. API level was not recorded.
- **Server owner:** agent-service. Node only forwards an allow-listed path set. Android never calls SnapTrade directly.
- **Connection type:** `read`. **Broker:** `SANDBOX`. **Redirect:** `easymoney://portfolio/snaptrade`.
- **Status:** one live Sandbox connection completed end to end (self-directed scenario).
- **Aggregate result (first successful sync):** status `READY`, 2 accounts, 12 positions, 2 cash balances, currency
  USD only, 0 warnings.
- **Redirect:** the Custom Tab returned to the app; the callback was consumed once and the app then called the backend
  completion endpoint, which verified the connection with SnapTrade. The redirect is a hint, never proof.
- **Duplicate callback:** the handler clears the pending session on first consume and `MainActivity` clears the intent
  data after forwarding it, so a recreation (rotation) does not replay it. Not exercised on a device with rotation.
- **Sync:** one manual sync succeeded and `lastSuccessfulSyncAt` advanced only on success. A failed sync (observed twice
  during validation, see defects) left the previous snapshot intact.
- **Disconnect:** explicit confirmation; provider connection removed (provider connection count 0); status
  `DISCONNECTED`; sync and portfolio reads then return a safe 404 and make no provider call. The CSV flow stayed available.
- **Data deletion:** `DELETE .../data` requires `confirm=true` (400 otherwise), removes only the synchronized snapshot,
  and is idempotent. Disconnect and deletion are separate actions. Normalized data stays on the server after disconnect
  until it is deleted. The delete action was verified through the API, not through the UI.
- **CSV coexistence:** CSV import and the connected source stay separate; nothing was merged. CSV snapshots were not
  modified.
- **Logging review:** Android logcat and both server logs contained no Authorization header, bearer token, JWT, consumer
  key, user secret, portal URL or financial payload from EasyMoney. Android OS components (`ActivityTaskManager`,
  `WindowManager`) log the Custom Tab launch URL, which includes the public client ID; that is outside app control.

### Defects found and fixed during live validation

1. Node imported the proxy router but never mounted it at `/portfolio/connections`.
2. `MainActivity` never forwarded the redirect URI to `PortfolioDeepLinkHandler`.
3. `DevelopmentAgentAccessTokenProvider` still returned no token.
4. SnapTrade now answers `410` on `DELETE /authorizations/{id}` and `GET /accounts/{id}/positions`. Disconnect uses
   `DELETE /connection/{id}` and positions use `GET /accounts/{id}/positions/all` (`results[].instrument` shape).
5. The Sandbox plan rejects manual refresh with `403` (code 1141, data endpoints are already real time). Refresh now
   tolerates that response instead of mapping it to `RECONNECT_REQUIRED`.

### Scenarios

- **Completed live:** self-directed (default), disconnect, synchronized-data deletion (API).
- **Pending (not exercised live):** cash only, no accounts, invalid or failed connection, rate limited, reconnect
  required via the UI, light/dark theme, font scale 1.5, landscape, rotation during portal or sync, long institution
  name. Those are covered by fake-provider and unit tests only.

### Known limitations

- Cost basis is not mapped (`averageCost` is null) because the new positions response returns a total cost basis.
- A connection created at SnapTrade but never completed in EasyMoney is not adopted later; a new session ignores
  connections that already existed. It must be removed at the provider.
- Tests pin `SNAPTRADE_*` settings in `agent-service/tests/conftest.py` (disabled, empty credentials), so a developer's
  ignored `.env` is never loaded by the suite.
- Disconnect is not automatically retried by the app code, but OkHttp's default connection retry still applies.
- Synced data lives on the server; the Android cache is in memory and reloaded from the backend.

### Status

Real Wealthsimple connectivity remains disabled and is not implemented. SnapTrade production access is deferred to a
separate milestone. SnapTrade Sandbox portfolio connection is verified end to end using simulated accounts.
