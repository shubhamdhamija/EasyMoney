const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');

const {
  stockExplanationRouter,
  createStockExplanationRouter
} = require(
  '../routes/stockExplanationRoutes'
);

const {
  InvalidAiOutputError
} = require(
  '../errors/InvalidAiOutputError'
);

const {
  requestIdMiddleware
} = require(
  '../middleware/requestId'
);

const {
  errorHandler
} = require(
  '../middleware/errorHandler'
);

function createTestApp() {
  const app = express();

  app.use(express.json());

  app.use(
    '/ai/explain',
    stockExplanationRouter
  );

  return app;
}

function createValidRequest(
  overrides = {}
) {
  return {
    symbol: 'AAPL',
    companyName: 'Apple Inc.',
    priceChangePercent: 2.4,
    marketChangePercent: 1.1,
    dayLow: 220.5,
    dayHigh: 228.3,
    evidence: [
      {
        type: 'news',
        headline:
          'Apple reported quarterly revenue above expectations.',
        source:
          'Controlled test source'
      }
    ],
    ...overrides
  };
}

test(
  'POST /ai/explain normalizes and accepts valid input',
  async () => {
    const app = createTestApp();

    const response =
      await request(app)
        .post('/ai/explain')
        .send(
          createValidRequest({
            symbol: ' aapl '
          })
        )
        .expect(200);

    assert.equal(
      response.body.ok,
      true
    );

    assert.equal(
      response.body.explanation.symbol,
      'AAPL'
    );

    assert.equal(
      response.body.explanation.direction,
      'increase'
    );

    assert.equal(
      response.body.explanation
        .evidenceSentiment,
      'neutral'
    );

    assert.equal(
      response.body.explanation
        .evidenceSufficient,
      true
    );

    assert.deepEqual(
      response.body.explanation.factors,
      []
    );

    assert.equal(
      response.body.explanation.disclaimer,
      'For informational purposes only.'
    );
  }
);

test(
  'POST /ai/explain returns all validation errors',
  async () => {
    const app = createTestApp();

    const response =
      await request(app)
        .post('/ai/explain')
        .send({
          symbol: '',
          priceChangePercent: 'up',
          dayLow: 300,
          dayHigh: 200,
          evidence: 'news'
        })
        .expect(400);

    assert.equal(
      response.body.ok,
      false
    );

    assert.deepEqual(
      response.body.error,
      {
        code: 'INVALID_REQUEST',
        message:
          'Invalid stock explanation request.'
      }
    );

    assert.ok(
      response.body.validationErrors.includes(
        'symbol is required'
      )
    );

    assert.ok(
      response.body.validationErrors.includes(
        'priceChangePercent must be a finite number'
      )
    );

    assert.ok(
      response.body.validationErrors.includes(
        'dayLow must not be greater than dayHigh'
      )
    );

    assert.ok(
      response.body.validationErrors.includes(
        'evidence must be an array'
      )
    );
  }
);

test(
  'POST /ai/explain returns insufficient state for empty evidence',
  async () => {
    const app = createTestApp();

    const response =
      await request(app)
        .post('/ai/explain')
        .send(
          createValidRequest({
            symbol: 'TSLA',
            priceChangePercent: -3.1,
            marketChangePercent: null,
            dayLow: null,
            dayHigh: null,
            evidence: []
          })
        )
        .expect(200);

    const explanation =
      response.body.explanation;

    assert.equal(
      explanation.symbol,
      'TSLA'
    );

    assert.equal(
      explanation.direction,
      'decrease'
    );

    assert.equal(
      explanation.evidenceSentiment,
      'insufficient'
    );

    assert.equal(
      explanation.evidenceSufficient,
      false
    );

    assert.deepEqual(
      explanation.factors,
      []
    );

    assert.match(
      explanation.summary,
      /insufficient/i
    );
  }
);

test(
  'POST /ai/explain returns unchanged for zero movement',
  async () => {
    const app = createTestApp();

    const response =
      await request(app)
        .post('/ai/explain')
        .send(
          createValidRequest({
            priceChangePercent: 0
          })
        )
        .expect(200);

    assert.equal(
      response.body.explanation.direction,
      'unchanged'
    );
  }
);

test(
  'POST /ai/explain rejects incomplete input',
  async () => {
    const app = createTestApp();

    const response =
      await request(app)
        .post('/ai/explain')
        .send({
          symbol: 'AAPL'
        })
        .expect(400);

    assert.ok(
      response.body.validationErrors.includes(
        'priceChangePercent must be a finite number'
      )
    );

    assert.ok(
      response.body.validationErrors.includes(
        'evidence must be an array'
      )
    );
  }
);

test(
  'route forwards invalid AI output to central error middleware',
  async () => {
    const app = express();

    app.use(express.json());
    app.use(requestIdMiddleware);

    const failingService = {
      async generateStockExplanation() {
        throw new InvalidAiOutputError([
          'summary is required'
        ]);
      }
    };

    app.use(
      '/ai/explain',
      createStockExplanationRouter({
        stockExplanationService:
          failingService
      })
    );

    app.use(errorHandler);

    const response =
      await request(app)
        .post('/ai/explain')
        .send(
          createValidRequest()
        )
        .expect(502);

    assert.equal(
      response.body.ok,
      false
    );

    assert.equal(
      response.body.error.code,
      'INVALID_AI_OUTPUT'
    );

    assert.equal(
      response.body.error.message,
      'The AI provider returned an invalid response.'
    );

    assert.ok(
      response.body.requestId
    );
  }
);

test(
  'POST /ai/explain logs exactly one safe entry per request',
  async () => {
    const entries = [];
    const app = express();

    app.use(express.json());
    app.use(requestIdMiddleware);
    app.use(
      '/ai/explain',
      createStockExplanationRouter({
        logger: (entry) => entries.push(entry),
        stockExplanationService: {
          async generateStockExplanation() {
            return {
              explanation: {},
              metadata: {
                promptVersion: 'stock-explanation-test',
                providerCalled: true
              }
            };
          }
        }
      })
    );

    await request(app)
      .post('/ai/explain')
      .send(createValidRequest())
      .expect(200);

    assert.equal(entries.length, 1);
    assert.deepEqual(
      Object.keys(entries[0]).sort(),
      [
        'promptVersion',
        'providerCalled',
        'requestId',
        'route',
        'status',
        'symbol'
      ]
    );
    assert.equal(entries[0].symbol, 'AAPL');
    assert.equal(entries[0].providerCalled, true);
    assert.ok(entries[0].requestId);
    assert.ok(
      !JSON.stringify(entries[0]).includes(
        'Apple reported quarterly revenue'
      )
    );
  }
);

