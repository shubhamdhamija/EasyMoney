const test =
  require('node:test');

const assert =
  require('node:assert/strict');

const {
  validateStockExplanationOutput
} = require(
  '../validators/stockExplanationOutputValidator'
);

function createValidGeneratedOutput(
  overrides = {}
) {
  return {
    evidenceSentiment:
      'positive',

    summary:
      'The supplied revenue evidence may have contributed to the increase.',

    factors: [
      'Revenue exceeded expectations.'
    ],

    evidenceSufficient:
      true,

    ...overrides
  };
}

test(
  'valid generated output passes and is normalized',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          evidenceSentiment:
            ' POSITIVE ',

          summary:
            ' Explanation text. ',

          factors: [
            ' Supported factor. '
          ]
        })
      );

    assert.equal(
      result.valid,
      true
    );

    assert.equal(
      result.value
        .evidenceSentiment,
      'positive'
    );

    assert.equal(
      result.value.summary,
      'Explanation text.'
    );

    assert.deepEqual(
      result.value.factors,
      [
        'Supported factor.'
      ]
    );
  }
);

test(
  'non-object generated output is rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        'invalid'
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'generated output must be a JSON object'
      )
    );
  }
);

test(
  'unsupported evidence sentiment is rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          evidenceSentiment:
            'super_bullish'
        })
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.some(
        error =>
          error.includes(
            'evidenceSentiment'
          )
      )
    );
  }
);

test(
  'blank summary is rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          summary: ' '
        })
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'summary is required'
      )
    );
  }
);

test(
  'non-array factors are rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          factors:
            'Revenue exceeded expectations.'
        })
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'factors must be an array'
      )
    );
  }
);

test(
  'more than three factors are rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          factors: [
            'Factor one',
            'Factor two',
            'Factor three',
            'Factor four'
          ]
        })
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'factors must contain no more than 3 items'
      )
    );
  }
);

test(
  'blank factors are rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          factors: [
            'Valid factor',
            ' '
          ]
        })
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'factors[1] must be a non-blank string'
      )
    );
  }
);

test(
  'non-boolean evidenceSufficient is rejected',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          evidenceSufficient:
            'yes'
        })
      );

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'evidenceSufficient must be a boolean'
      )
    );
  }
);

test(
  'valid insufficient-evidence output passes',
  () => {
    const result =
      validateStockExplanationOutput({
        evidenceSentiment:
          'insufficient',

        summary:
          'The available evidence is insufficient to explain the movement.',

        factors: [],

        evidenceSufficient:
          false
      });

    assert.equal(
      result.valid,
      true
    );

    assert.equal(
      result.value
        .evidenceSentiment,
      'insufficient'
    );

    assert.deepEqual(
      result.value.factors,
      []
    );

    assert.equal(
      result.value
        .evidenceSufficient,
      false
    );
  }
);

test(
  'insufficient evidence cannot contain factors',
  () => {
    const result =
      validateStockExplanationOutput({
        evidenceSentiment:
          'insufficient',

        summary:
          'Evidence is insufficient.',

        factors: [
          'Unsupported factor'
        ],

        evidenceSufficient:
          false
      });

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'factors must be empty when evidenceSufficient is false'
      )
    );
  }
);

test(
  'false evidence sufficiency requires insufficient sentiment',
  () => {
    const result =
      validateStockExplanationOutput({
        evidenceSentiment:
          'positive',

        summary:
          'Evidence is unavailable.',

        factors: [],

        evidenceSufficient:
          false
      });

    assert.equal(
      result.valid,
      false
    );

    assert.ok(
      result.errors.includes(
        'evidenceSentiment must be insufficient when evidenceSufficient is false'
      )
    );
  }
);

test(
  'unexpected provider fields are not included in normalized output',
  () => {
    const result =
      validateStockExplanationOutput(
        createValidGeneratedOutput({
          buyRecommendation:
            'Buy immediately',

          guaranteedPrice:
            500
        })
      );

    assert.equal(
      result.valid,
      true
    );

    assert.equal(
      Object.prototype.hasOwnProperty.call(
        result.value,
        'buyRecommendation'
      ),
      false
    );

    assert.equal(
      Object.prototype.hasOwnProperty.call(
        result.value,
        'guaranteedPrice'
      ),
      false
    );
  }
);

