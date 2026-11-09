# POST /ai/picks

Backend source for the "AI Picks" market highlights. These are **notable
stocks showing notable movement**, not investment recommendations. Backend
prompts and UI copy must avoid "best investment", "buy now", "recommended
stock" and "guaranteed opportunity".

```
Android -> EasyMoney backend -> LlmClient (Groq) -> request validation
        -> output validation -> output policy -> deterministic fallback
```

## Request

```json
{
  "stocks": [
    { "symbol": "AAPL", "companyName": "Apple Inc.", "currentPrice": 228.30,
      "priceChangePercent": 2.40, "dayLow": 220.50, "dayHigh": 228.30 }
  ],
  "maximumPicks": 3
}
```

- `stocks`: array of 1-30. Symbols are trimmed, uppercased and must be unique
  (duplicates are rejected with `400`). `priceChangePercent` is required and
  finite; `currentPrice`, `dayLow`, `dayHigh` are optional and non-negative.
- `maximumPicks`: integer 1-5, default 3. It is an **upper bound**: with fewer
  stocks than `maximumPicks` the service returns at most that many picks
  rather than rejecting the request.
- Company names are untrusted. Instruction-like names are dropped before the
  prompt is built.

## Response (200)

```json
{
  "ok": true,
  "picks": [
    { "symbol": "AAPL", "reason": "AAPL recorded notable positive daily movement relative to the supplied stock set.", "rank": 1 }
  ],
  "disclaimer": "For informational purposes only. This is not a recommendation to buy, sell, or hold any security.",
  "requestId": "request-id"
}
```

- `rank` is assigned by the backend from list order (1..n), so ranks are always
  unique and sequential. Internal metadata is logged, not returned.
- `picks` is empty (without a provider call) when no supplied stock moved.
- Errors: `400 INVALID_REQUEST` with `validationErrors`; provider failures use
  the shared error handler (no provider detail).

## Safety layers

1. Request validation (above).
2. Prompt `stock-picks-v1` (`stockPicksPrompt.js`): notable movement only, no
   recommendation framing, symbols only from the supplied set.
3. Structural output validation (`stockPicksOutputValidator.js`).
4. Output policy `stock-picks-output-policy-v1` (`stockPicksOutputPolicy.js`):
   symbol not supplied, duplicate picks, too many / zero picks,
   buy/sell/hold and recommendation wording (attractive, promising,
   opportunity, top pick, guaranteed ...), forecasts, investor psychology,
   internal terminology.
5. On a violation: one provider call, **no retry**, deterministic fallback
   ranked by absolute movement, e.g. "AAPL recorded notable daily movement of
   +2.40% in the supplied stock set." Rejected wording is never returned.
6. Provider errors (timeout, rate limit, auth, config) are technical errors and
   never use the fallback.

## Evaluation

`npm run eval:picks:fake` (11 cases) and `npm run eval:picks:live` (6 cases; the
5 cases that force a specific model misbehaviour are fake-only). Cases: mixed
movers, nearly unchanged, all unchanged, fewer stocks than picks, duplicate
input symbols, injection-like company name, unsupplied symbol, duplicate picks,
buy advice, guaranteed-return wording, provider failure. Reports:
`stockPicksReport.<mode>.json` (git-ignored).

Results: fake 11/11; live, three spaced runs: 6/6 each, 4 native passes + 1
validation rejection + 1 no-provider-call case, 0 fallbacks, 0 provider errors,
average provider latency 379-521 ms. Five-plus-one cases is a small dataset.

## Android

`AiInsightRepositoryImpl.getAiPicks()` -> `BackendApiService.getAiPicks()` ->
`POST ai/picks`. The request is limited to 30 stocks, symbols normalised and
de-duplicated, blank/non-finite rows dropped, `maximumPicks = 3`. The response
maps to the existing `AiPick`. Errors become "AI picks are temporarily
unavailable." style messages; cancellation is rethrown; the 30-minute cache is
kept. Both repository methods use only `BackendApiService`.

## Legacy provider cleanup (done)

Removed: `OpenAiApiService`, `OpenAiDto.kt` (`OpenAiRequest`, `OpenAiResponse`,
`OpenAiMessage`, `ResponseFormat`, `InsightJson`, `AiPicksJson`, `AiPickJson`),
`OpenAiQualifiers.kt`, the OpenAI Retrofit/OkHttp configuration and timeouts in
`AiModule`, `Constants.OPENAI_BASE_URL`, the Pollinations comments, the Gson
JSON-extraction code, and the `OPENAI_API_KEY` BuildConfig field (a credential
that was compiled into the APK).

`grep -R -n -E 'OpenAiApiService|OpenAiRequest|OpenAiResponse|AiPicksJson|response_format|Pollinations' app/src/main`
returns no matches.

## Follow-ups

- Domain rename (`shortTermOutlook`/`longTermOutlook` -> context names) together
  with UI copy, as one cleanup.
- Home copy "Get AI-generated ideas..." still reads like recommendation
  framing; change to "market highlights" in that UI cleanup.

