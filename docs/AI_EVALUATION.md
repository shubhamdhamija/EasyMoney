# EasyMoney AI Evaluation

## Purpose

This document defines how EasyMoney evaluates AI-generated stock explanations.

The first evaluated feature is:

"Explain why a stock moved using supplied market evidence."

## Success Criteria

An acceptable response must:

1. Return the correct stock symbol.
2. Describe the correct movement direction.
3. Preserve supplied percentages and numbers.
4. Use the supplied evidence.
5. Avoid introducing unsupported claims.
6. Return the required structured JSON.
7. Clearly state when evidence is insufficient.
8. Avoid personalized financial advice.
9. Complete within the accepted response time.
10. Handle provider and validation failures safely.

## Initial Quality Checklist

For each response, check:

- [ ] Correct symbol
- [ ] Correct movement direction
- [ ] Correct supplied numbers
- [ ] Uses supplied evidence
- [ ] Contains no invented claims
- [ ] Returns valid JSON
- [ ] Handles insufficient evidence honestly
- [ ] Uses safe informational wording

## Initial Scoring

Each checked item receives one point.

Maximum score: 8

Initial interpretation:

- 8 points: Pass
- 7 points: Manual review
- 6 points or below: Fail

These thresholds may change after we collect more evaluation results.

## Error Categories

### Input Error

EasyMoney supplied missing or incorrect information to the model.

Example:

The price-change percentage was missing.

### Prompt Error

The model instructions were unclear, incomplete, or contradictory.

Example:

The prompt did not tell the model to use only supplied evidence.

### Hallucination

The model introduced information that was not included in the supplied evidence.

Example:

The model mentioned an analyst upgrade that EasyMoney never provided.

### Direction Error

The response described an increase as a decrease, or a decrease as an increase.

### Number Error

The response changed a supplied number.

Example:

Input: 2.4%

Output: 4.2%

### Format Error

The response did not match the required JSON structure.

### Insufficient-Evidence Error

The model gave a confident explanation even though no supporting evidence was supplied.

### Safety Error

The response presented personalized financial advice or guaranteed predictions.

### Infrastructure Error

Examples:

- Timeout
- Rate limiting
- Provider outage
- Network failure
- Empty model response
- Invalid provider response

## First Evaluation Cases

We will initially create five controlled cases:

1. Positive movement with sufficient evidence
2. Negative movement with sufficient evidence
3. Positive movement without evidence
4. Incorrect or incomplete input
5. Provider or response-format failure

## Evaluation Process

For every case:

1. Send the controlled input.
2. Save the raw model response.
3. Validate the JSON structure.
4. Check the symbol and direction.
5. Check supplied numbers.
6. Check whether supplied evidence was used.
7. Identify unsupported claims.
8. Record latency.
9. Assign error categories.
10. Save the final score.

## Release Rule

Any change to the following must run against the evaluation cases before deployment:

- Prompt
- Model
- LLM provider
- Input evidence format
- Response JSON structure
- Grounding rules

