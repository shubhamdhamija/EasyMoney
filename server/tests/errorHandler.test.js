const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');

const {
  InvalidAiOutputError
} = require(
  '../errors/InvalidAiOutputError'
);

const {
  LlmProviderError
} = require(
  '../errors/LlmProviderError'
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

function createTestApp(
  thrownError
) {
  const app = express();

  app.use(
    requestIdMiddleware
  );

  app.get(
    '/failure',
    (req, res, next) => {
      void res;
      next(thrownError);
    }
  );

  app.use(
    errorHandler
  );

  return app;
}

test(
  'invalid AI output becomes a safe 502 response',
  async () => {
    const app =
      createTestApp(
        new InvalidAiOutputError([
          'summary is required'
        ])
      );

    const response =
      await request(app)
        .get('/failure')
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

    assert.equal(
      response.body.error.validationErrors,
      undefined
    );

    assert.ok(
      response.body.requestId
    );

    assert.equal(
      response.headers[
        'x-request-id'
      ],
      response.body.requestId
    );
  }
);

test(
  'unexpected error becomes a generic 500 response',
  async () => {
    const app =
      createTestApp(
        new Error(
          'Database password was exposed here'
        )
      );

    const response =
      await request(app)
        .get('/failure')
        .expect(500);

    assert.equal(
      response.body.error.code,
      'INTERNAL_SERVER_ERROR'
    );

    assert.doesNotMatch(
      response.body.error.message,
      /password/i
    );

    assert.equal(
      response.body.error.stack,
      undefined
    );
  }
);

test(
  'existing request ID is preserved',
  async () => {
    const app =
      createTestApp(
        new Error(
          'Failure'
        )
      );

    const response =
      await request(app)
        .get('/failure')
        .set(
          'x-request-id',
          'easy-money-test-123'
        )
        .expect(500);

    assert.equal(
      response.body.requestId,
      'easy-money-test-123'
    );

    assert.equal(
      response.headers[
        'x-request-id'
      ],
      'easy-money-test-123'
    );
  }
);

test(
  'provider errors return safe availability responses',
  async () => {
    const app =
      createTestApp(
        new LlmProviderError({
          code:
            'AI_PROVIDER_TIMEOUT',

          message:
            'The AI provider request timed out.',

          statusCode:
            504
        })
      );

    const response =
      await request(app)
        .get('/failure')
        .expect(504);

    assert.equal(
      response.body.ok,
      false
    );

    assert.equal(
      response.body.error.code,
      'AI_PROVIDER_TIMEOUT'
    );

    assert.equal(
      response.body.error.message,
      'The AI explanation service is temporarily unavailable.'
    );

    assert.equal(
      response.body.error.providerStatus,
      undefined
    );
  }
);

test(
  'disabled AI provider errors return the disabled public message',
  async () => {
    const app =
      createTestApp(
        new LlmProviderError({
          code:
            'AI_LIVE_CALLS_DISABLED',

          message:
            'Live AI calls are disabled.',

          statusCode:
            503
        })
      );

    const response =
      await request(app)
        .get('/failure')
        .expect(503);

    assert.equal(
      response.body.error.code,
      'AI_LIVE_CALLS_DISABLED'
    );

    assert.equal(
      response.body.error.message,
      'AI explanation generation is currently disabled.'
    );
  }
);



