const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');

const { validateStockInsightRequest } = require('../validators/stockInsightValidator');
const { validateStockInsightOutput } = require('../validators/stockInsightOutputValidator');
const { createStockInsightService } = require('../services/stockInsightService');
const { createStockInsightRouter } = require('../routes/stockInsightRoutes');
const { findInsightPolicyViolations } = require('../services/stockInsightOutputPolicy');
const { PROMPT_VERSION, buildStockInsightPrompt } = require('../services/stockInsightPrompt');
const { createFakeLlmClient } = require('../services/llm/fakeLlmClient');
const { LlmProviderError } = require('../errors/LlmProviderError');
const { requestIdMiddleware } = require('../middleware/requestId');
const { errorHandler } = require('../middleware/errorHandler');

const evidence = [
  {
    type: 'news',
    headline: 'Quarterly revenue exceeded the supplied estimate.',
    source: 'News provider'
  }
];

function createRequest(overrides = {}) {
  return {
    symbol: 'aapl',
    companyName: 'Apple Inc.',
    currentPrice: 228.3,
    priceChangePercent: 2.4,
    dayLow: 220.5,
    dayHigh: 228.3,
    evidence,
    ...overrides
  };
}

function createGenerated(overrides = {}) {
  return {
    evidenceSentiment: 'positive',
    summary: 'The supplied revenue evidence may have contributed to the increase.',
    shortTermContext: 'The available data describes current movement only.',
    longTermContext: 'The supplied evidence does not establish a long-term outlook.',
    factors: ['Quarterly revenue exceeded the supplied estimate.'],
    evidenceSufficient: true,
    ...overrides
  };
}

function createService(response, extra = {}) {
  const client = createFakeLlmClient({ response, ...extra });
  return { client, service: createStockInsightService({ llmClient: client }) };
}

// ---- request validator ----

test('insight request: valid input is normalised', () => {
  const result = validateStockInsightRequest(createRequest());
  assert.equal(result.valid, true);
  assert.equal(result.value.symbol, 'AAPL');
  assert.equal(result.value.currentPrice, 228.3);
});

test('insight request: currentPrice is optional but must be non-negative', () => {
  assert.equal(
    validateStockInsightRequest(createRequest({ currentPrice: undefined })).valid,
    true
  );
  const bad = validateStockInsightRequest(createRequest({ currentPrice: -1 }));
  assert.equal(bad.valid, false);
  assert.match(bad.errors.join(' '), /currentPrice/);
});

test('insight request: shared rules still apply', () => {
  const result = validateStockInsightRequest(
    createRequest({ symbol: '', priceChangePercent: 'x' })
  );
  assert.equal(result.valid, false);
  assert.ok(result.errors.length >= 2);
});

// ---- output validator ----

test('insight output: requires both contexts', () => {
  const result = validateStockInsightOutput(
    createGenerated({ shortTermContext: '', longTermContext: undefined })
  );
  assert.equal(result.valid, false);
  assert.ok(result.errors.includes('shortTermContext is required'));
  assert.ok(result.errors.includes('longTermContext is required'));
});

test('insight output: accepts a valid response', () => {
  assert.equal(validateStockInsightOutput(createGenerated()).valid, true);
});

// ---- policy ----

test('policy flags forecast, advice and psychology wording', () => {
  const forecast = findInsightPolicyViolations({
    generated: createGenerated({ shortTermContext: 'The stock will rise next week.' }),
    evidence
  });
  assert.deepEqual(forecast, ['UNSUPPORTED_FORECAST']);

  const advice = findInsightPolicyViolations({
    generated: createGenerated({ summary: 'Investors should buy now.' }),
    evidence
  });
  assert.ok(advice.includes('UNSUPPORTED_FORECAST'));

  const psychology = findInsightPolicyViolations({
    generated: createGenerated({ longTermContext: 'Investor enthusiasm may continue.' }),
    evidence
  });
  assert.ok(psychology.includes('UNSUPPORTED_INTERPRETATION'));
});

test('policy flags peer/competitor wording for the market benchmark', () => {
  const violations = findInsightPolicyViolations({
    generated: createGenerated({ summary: 'The stock beat its peers today.' }),
    evidence
  });
  assert.deepEqual(violations, ['BENCHMARK_MISREPRESENTATION']);
});

test('policy accepts cautious wording and word-boundary safe text', () => {
  assert.deepEqual(
    findInsightPolicyViolations({
      generated: createGenerated({
        summary: 'The shareholder letter noted household demand.',
        shortTermContext:
          'The available data describes current movement but is insufficient for a guaranteed short-term forecast.'
      }),
      evidence
    }),
    []
  );
});

// ---- prompt ----

test('insight prompt is versioned and forbids forecasting', () => {
  const prompt = buildStockInsightPrompt({ ...createRequest(), symbol: 'AAPL', marketChangePercent: null, evidence });
  assert.equal(PROMPT_VERSION, 'stock-insight-v3');
  assert.equal(prompt.promptVersion, 'stock-insight-v3');
  assert.match(prompt.systemInstruction, /Do not predict future prices/);
  assert.match(prompt.systemInstruction, /untrusted evidence/);
  assert.match(prompt.userMessage, /Current price: \$228\.30/);
  assert.match(prompt.userMessage, /<untrusted_evidence>/);
});

// ---- service ----

test('insight service: valid response passes through without fallback', async () => {
  const { client, service } = createService(createGenerated());
  const input = validateStockInsightRequest(createRequest()).value;
  const result = await service.generateStockInsight(input);

  assert.equal(client.calls.length, 1);
  assert.equal(result.metadata.fallbackUsed, false);
  assert.equal(result.metadata.providerCalled, true);
  assert.equal(result.insight.direction, 'increase');
  assert.equal(result.insight.disclaimer, 'For informational purposes only.');
  assert.deepEqual(result.insight.factors, ['Quarterly revenue exceeded the supplied estimate.']);
});

test('insight service: forecast wording triggers one provider call and a deterministic fallback', async () => {
  const { client, service } = createService(
    createGenerated({ shortTermContext: 'The stock will rise tomorrow.' })
  );
  const input = validateStockInsightRequest(createRequest()).value;
  const result = await service.generateStockInsight(input);

  assert.equal(client.calls.length, 1);
  assert.equal(result.metadata.fallbackUsed, true);
  assert.equal(result.metadata.fallbackReason, 'UNSUPPORTED_FORECAST');
  assert.equal(result.insight.evidenceSentiment, 'neutral');
  assert.ok(!JSON.stringify(result).includes('will rise'));
  assert.match(result.insight.shortTermContext, /insufficient for a guaranteed short-term forecast/);
});

test('insight service: psychology wording uses the interpretation fallback reason', async () => {
  const { service } = createService(
    createGenerated({ summary: 'Lower guidance tempered investor enthusiasm.' })
  );
  const result = await service.generateStockInsight(
    validateStockInsightRequest(createRequest()).value
  );
  assert.equal(result.metadata.fallbackReason, 'UNSUPPORTED_INTERPRETATION');
  assert.ok(!JSON.stringify(result).toLowerCase().includes('enthusiasm'));
});

test('insight service: leaked internal terminology triggers a fallback', async () => {
  const { client, service } = createService(
    createGenerated({
      summary: 'Untrusted evidence indicates that revenue exceeded estimates.'
    })
  );
  const result = await service.generateStockInsight(
    validateStockInsightRequest(createRequest()).value
  );

  assert.equal(client.calls.length, 1);
  assert.equal(result.metadata.fallbackUsed, true);
  assert.equal(result.metadata.fallbackReason, 'INTERNAL_TERMINOLOGY_LEAK');
  assert.ok(!JSON.stringify(result).toLowerCase().includes('untrusted'));
});

test('insight service: provider timeout is a controlled error with no fallback', async () => {
  const { service } = createService(undefined, {
    error: new LlmProviderError({
      code: 'AI_PROVIDER_TIMEOUT',
      message: 'The AI provider timed out.',
      statusCode: 504
    })
  });
  await assert.rejects(
    () => service.generateStockInsight(validateStockInsightRequest(createRequest()).value),
    error => error.code === 'AI_PROVIDER_TIMEOUT'
  );
});

test('insight service: structurally invalid output is rejected', async () => {
  const { service } = createService(createGenerated({ shortTermContext: undefined }));
  await assert.rejects(
    () => service.generateStockInsight(validateStockInsightRequest(createRequest()).value),
    error => error.name === 'InvalidAiOutputError'
  );
});

// ---- route ----

function createApp(service, logger) {
  const app = express();
  app.use(express.json());
  app.use(requestIdMiddleware);
  app.use(
    '/ai/stock-insight',
    createStockInsightRouter({ stockInsightService: service, logger })
  );
  app.use(errorHandler);
  return app;
}

test('POST /ai/stock-insight returns the contract and hides metadata', async () => {
  const { service } = createService(createGenerated());
  const logs = [];
  const response = await request(createApp(service, e => logs.push(e)))
    .post('/ai/stock-insight')
    .send(createRequest());

  assert.equal(response.status, 200);
  assert.equal(response.body.ok, true);
  assert.equal(response.body.insight.symbol, 'AAPL');
  assert.ok(response.body.requestId);
  assert.equal(response.body.metadata, undefined);
  assert.equal(logs.length, 1);
  assert.equal(logs[0].route, '/ai/stock-insight');
  assert.equal(logs[0].promptVersion, 'stock-insight-v3');
  assert.equal(logs[0].providerCalled, true);
  assert.equal(logs[0].fallbackUsed, false);
  assert.ok(!JSON.stringify(logs[0]).includes('Quarterly'));
});

test('POST /ai/stock-insight rejects invalid requests with 400', async () => {
  const { client, service } = createService(createGenerated());
  const response = await request(createApp(service, () => {}))
    .post('/ai/stock-insight')
    .send({ symbol: '' });

  assert.equal(response.status, 400);
  assert.equal(response.body.error.code, 'INVALID_REQUEST');
  assert.equal(client.calls.length, 0);
});

test('POST /ai/stock-insight maps provider failures to a safe error', async () => {
  const { service } = createService(undefined, {
    error: new LlmProviderError({
      code: 'AI_PROVIDER_RATE_LIMITED',
      message: 'secret-provider-detail',
      statusCode: 429
    })
  });
  const response = await request(createApp(service, () => {}))
    .post('/ai/stock-insight')
    .send(createRequest());

  assert.ok(response.status >= 429);
  assert.equal(response.body.ok, false);
  assert.ok(!JSON.stringify(response.body).includes('secret-provider-detail'));
});

test('eval: insight fallback copy is not flagged as a guarantee claim', () => {
  const { evaluateFallbackSafety } = require('../evals/runStockExplanationEvals');
  const safe = evaluateFallbackSafety(
    { input: { evidence } },
    {
      evidenceSentiment: 'neutral',
      summary:
        'AAPL increased 2.40%. The available data is insufficient for a guaranteed short-term forecast.',
      factors: ['Quarterly revenue exceeded the supplied estimate.']
    },
    { fallbackReason: 'UNSUPPORTED_FORECAST' }
  );
  assert.equal(safe.passed, true, JSON.stringify(safe.problems));

  const unsafe = evaluateFallbackSafety(
    { input: { evidence } },
    {
      evidenceSentiment: 'neutral',
      summary: 'The stock is guaranteed to rise.',
      factors: []
    },
    { fallbackReason: 'UNSUPPORTED_FORECAST' }
  );
  assert.equal(unsafe.passed, false);
});
