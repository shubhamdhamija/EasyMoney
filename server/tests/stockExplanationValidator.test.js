const test = require('node:test');
const assert = require('node:assert/strict');

const {
  validateStockExplanationRequest
} = require(
  '../validators/stockExplanationValidator'
);

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
          'Example financial source'
      }
    ],
    ...overrides
  };
}

test(
  'valid request passes and normalizes its values',
  () => {
    const request =
      createValidRequest({
        symbol: ' aapl '
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      true
    );

    assert.deepEqual(
      result.errors,
      []
    );

    assert.equal(
      result.value.symbol,
      'AAPL'
    );

    assert.equal(
      result.value.companyName,
      'Apple Inc.'
    );

    assert.equal(
      result.value.priceChangePercent,
      2.4
    );

    assert.equal(
      result.value.evidence.length,
      1
    );
  }
);

test(
  'missing symbol is rejected',
  () => {
    const request =
      createValidRequest({
        symbol: ''
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.equal(
      result.value,
      null
    );

    assert.ok(
      result.errors.includes(
        'symbol is required'
      )
    );
  }
);

test(
  'non-numeric priceChangePercent is rejected',
  () => {
    const request =
      createValidRequest({
        priceChangePercent: 'up'
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'priceChangePercent must be a finite number'
      )
    );
  }
);

test(
  'dayLow greater than dayHigh is rejected',
  () => {
    const request =
      createValidRequest({
        dayLow: 300,
        dayHigh: 200
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'dayLow must not be greater than dayHigh'
      )
    );
  }
);

test(
  'evidence must be an array',
  () => {
    const request =
      createValidRequest({
        evidence:
          'Apple reported stronger revenue'
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'evidence must be an array'
      )
    );
  }
);

test(
  'unsupported evidence type is rejected',
  () => {
    const request =
      createValidRequest({
        evidence: [
          {
            type: 'social_media_rumor',
            headline:
              'Unverified social-media claim',
            source:
              'Unknown'
          }
        ]
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'evidence[0].type is unsupported'
      )
    );
  }
);

test(
  'blank evidence headline is rejected',
  () => {
    const request =
      createValidRequest({
        evidence: [
          {
            type: 'news',
            headline: '   ',
            source: 'Example source'
          }
        ]
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'evidence[0].headline is required'
      )
    );
  }
);

test(
  'empty evidence array is valid',
  () => {
    const request =
      createValidRequest({
        evidence: []
      });

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      true
    );

    assert.deepEqual(
      result.value.evidence,
      []
    );
  }
);

test(
  'optional fields may be omitted',
  () => {
    const request = {
      symbol: 'TSLA',
      priceChangePercent: -3.1,
      evidence: []
    };

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      true
    );

    assert.equal(
      result.value.symbol,
      'TSLA'
    );

    assert.equal(
      result.value.companyName,
      null
    );

    assert.equal(
      result.value.marketChangePercent,
      null
    );

    assert.equal(
      result.value.dayLow,
      null
    );

    assert.equal(
      result.value.dayHigh,
      null
    );
  }
);

test(
  'multiple validation errors are returned together',
  () => {
    const request = {
      symbol: '',
      priceChangePercent: 'up',
      dayLow: 300,
      dayHigh: 200,
      evidence: 'invalid'
    };

    const result =
      validateStockExplanationRequest(
        request
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'symbol is required'
      )
    );

    assert.ok(
      result.errors.includes(
        'priceChangePercent must be a finite number'
      )
    );

    assert.ok(
      result.errors.includes(
        'dayLow must not be greater than dayHigh'
      )
    );

    assert.ok(
      result.errors.includes(
        'evidence must be an array'
      )
    );
  }
);

