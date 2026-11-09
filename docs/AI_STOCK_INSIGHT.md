# POST /ai/stock-insight

Backend replacement for the legacy Android direct-provider stock insight.
The legacy path (`AiInsightRepositoryImpl` -> `OpenAiApiService`) is kept until
Android is migrated and AI picks are also migrated.

```
Android -> EasyMoney backend -> LlmClient (Groq) -> input validation
        -> evidence policy -> output validation -> output policy
        -> deterministic fallback -> safe response
```

## Request

```json
{
  "symbol": "AAPL",
  "companyName": "Apple Inc.",
  "currentPrice": 228.30,
  "priceChangePercent": 2.40,
  "marketChangePercent": 1.10,
  "dayLow": 220.50,
  "dayHigh": 228.30,
  "evidence": [
    { "type": "news", "headline": "Quarterly revenue exceeded the supplied estimate.", "source": "News provider" }
  ]
}
```

- Required: `symbol`, `priceChangePercent`, `evidence` (array, max 7 items).
- Optional: `companyName`, `currentPrice` (>= 0), `marketChangePercent`, `dayLow`, `dayHigh`.
- Evidence types: `news`, `earnings`, `market`, `company_event`.
- Shared rules with `/ai/explain` (`validators/stockExplanationValidator.js`).

## Response (200)

```json
{
  "ok": true,
  "insight": {
    "symbol": "AAPL",
    "direction": "increase",
    "evidenceSentiment": "positive",
    "summary": "...",
    "shortTermContext": "...",
    "longTermContext": "...",
    "factors": ["Quarterly revenue exceeded the supplied estimate."],
    "evidenceSufficient": true,
    "disclaimer": "For informational purposes only."
  },
  "requestId": "request-id"
}
```

- `shortTermContext` / `longTermContext` summarise what the data and evidence
  do or do not establish. They are never forecasts (no `*Outlook` fields).
- `direction` (`increase` | `decrease` | `unchanged`) is calculated by the
  backend, not by the model. This is the one addition to the originally
  proposed contract.
- `evidenceSentiment`: `positive | negative | mixed | neutral | insufficient`.
- Internal metadata (`promptVersion`, `outputPolicyVersion`, `providerCalled`,
  `fallbackUsed`, `fallbackReason`, evidence counts) is **not** returned. It is
  written to the safe server log: `POST /ai/stock-insight {requestId, route,
  symbol, promptVersion, providerCalled, fallbackUsed, status}`.

## Errors

- `400` `INVALID_REQUEST` with `validationErrors`.
- Provider/validation failures use the shared error handler
  (`AI_PROVIDER_TIMEOUT`, `AI_PROVIDER_RATE_LIMITED`, `INVALID_AI_OUTPUT`, ...)
  and never include provider detail.

## Safety layers

1. Request validation.
2. Evidence policy (`stockEvidencePolicy.js`): instruction-like evidence is
   dropped; with no evidence left the provider is not called and an
   insufficient response is returned.
3. Prompt `stock-insight-v3` (`stockInsightPrompt.js`).
4. Structural output validation (`stockInsightOutputValidator.js`).
5. Output policy `stock-insight-output-policy-v1`
   (`stockInsightOutputPolicy.js`): unsupported investor psychology, forecast /
   price-target / advice wording, internal terminology, and peer/competitor
   wording for the market benchmark.
6. On a policy violation: one provider call, **no retry**, deterministic
   fallback (neutral sentiment, accepted headlines as factors, fixed context
   wording). Rejected model wording is never returned.
7. Provider errors (timeout, rate limit, auth, config) are technical errors and
   never use the fallback.

## Evaluation

`npm run eval:insight:fake` and `npm run eval:insight:live` run
`evals/stockInsightCases.json` (bullish, bearish, neutral, insufficient,
injection) through the shared scorer plus a `NO_FORECAST_CLAIMS` check, writing
`stockInsightReport.<mode>.json` (git-ignored). Reports use the same
native / fallback / final-safe metrics as the explanation evaluation.

### Live results

Two prompt iterations were driven by live findings:

- `v1`/`v2`: the model repeated "untrusted evidence" and omitted evidence
  concepts; the fallback scorer also wrongly flagged the deliberate fallback
  copy "insufficient for a guaranteed short-term forecast". Fixed by tightening
  the prompt (v3), adding the internal-terminology output guard and narrowing
  the guarantee pattern to guarantee *claims*.
- `v3`, three spaced runs before the benchmark guard: final-safe 4/5, 5/5, 5/5;
  provider errors 0. The one failure was the model calling the market "peers"
  (benchmark misrepresentation).
- `v3` plus the runtime benchmark guard, three spaced runs (final state):

  | Run | Native passes | Fallback recoveries | Final safe | Provider errors |
  |-----|---------------|---------------------|------------|-----------------|
  | 1   | 5             | 0                   | 5          | 0               |
  | 2   | 4             | 1                   | 5          | 0               |
  | 3   | 4             | 1                   | 5          | 0               |

  Final user-safe rate is 100%; the model is not 100% native (one case per run
  sometimes needs the fallback). Five cases are too few to enforce a fallback
  rate threshold.

## Migration status

| Step | Status |
|------|--------|
| 1-9 Backend contract, validators, evidence policy, prompt, output validator, injected `LlmClient`, fallback, evals | Done (verified with fake and live evals) |
| 10-12 Android DTOs (`StockInsightDto.kt`), `BackendApiService.getStockInsight`, `AiInsightRepositoryImpl.getStockInsight` -> backend, UI verification | Done. Unit-tested; verified on emulator (live: backend log shows `POST /ai/stock-insight`, no direct provider call; disabled: "AI insight is temporarily unavailable." with Retry) |
| 13 Remove `OpenAiApiService`, provider DTOs/Retrofit config, `OPENAI_API_KEY` | Done after AI Picks migration (see docs/AI_PICKS.md); no provider URL or key remains in app/src/main or BuildConfig |




