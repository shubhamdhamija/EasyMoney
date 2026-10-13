# EasyMoney AI System

## First Supported Task

Explain a stock movement using supplied market evidence.

## User Experience

A user opens a stock-detail screen and requests an explanation for the stock's
recent movement.

EasyMoney collects known market evidence, sends that evidence to the backend,
and returns a concise structured explanation.

## Architecture

```
Android Compose UI
→ StockDetailViewModel
→ AI Repository
→ Retrofit
→ EasyMoney Node.js Backend
→ Evidence Service
→ Prompt Builder
→ LLM Provider
→ Response Validator
→ Android Application
```

## Inputs

The initial feature may receive:

- Stock symbol
- Price-change percentage
- Market-change percentage
- Relevant news evidence
- Relevant earnings evidence

## Output

The backend should return:

- Stock symbol
- Concise summary
- Contributing factors
- Evidence-sufficiency flag
- Informational disclaimer

## Example Response

```json
{
  "symbol": "AAPL",
  "summary": "The supplied evidence suggests that stronger reported revenue and a positive broader market contributed to the gain.",
  "factors": [
    "Reported revenue exceeded expectations.",
    "The broader market increased by 1.1%."
  ],
  "evidenceSufficient": true,
  "disclaimer": "For informational purposes only."
}
```

## Stock Explanation API Contract

### Endpoint

`POST /ai/explain`

### Request

```json
{
  "symbol": "AAPL",
  "companyName": "Apple Inc.",
  "priceChangePercent": 2.4,
  "marketChangePercent": 1.1,
  "dayLow": 220.5,
  "dayHigh": 228.3,
  "evidence": [
    {
      "type": "news",
      "headline": "Apple reported quarterly revenue above expectations.",
      "source": "Example financial source"
    }
  ]
}
```

### Response

```json
{
  "symbol": "AAPL",
  "direction": "increase",
  "evidenceSentiment": "positive",
  "summary": "AAPL increased 2.4%. The supplied revenue evidence was positive and may have contributed to the movement.",
  "factors": [
    "Quarterly revenue exceeded expectations."
  ],
  "evidenceSufficient": true,
  "disclaimer": "For informational purposes only."
}
```

### Field Notes

- `direction` describes the observed price movement and is calculated by
  deterministic backend code.
- `evidenceSentiment` describes the tone of the supplied evidence.
- `direction` and `evidenceSentiment` may differ.
- `evidenceSentiment` must be one of `positive`, `negative`, `mixed`,
  `neutral`, or `insufficient`.
- `evidenceSentiment` must not be presented as a guaranteed price prediction or
  investment recommendation.
