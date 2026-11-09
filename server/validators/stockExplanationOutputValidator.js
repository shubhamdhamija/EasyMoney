const ALLOWED_EVIDENCE_SENTIMENTS =
  new Set([
    'positive',
    'negative',
    'mixed',
    'neutral',
    'insufficient'
  ]);

const MAX_FACTORS = 3;
const MAX_SUMMARY_LENGTH = 1200;
const MAX_FACTOR_LENGTH = 500;

function normalizeRequiredText(
  value
) {
  if (typeof value !== 'string') {
    return null;
  }

  const normalized =
    value.trim();

  return normalized.length > 0
    ? normalized
    : null;
}

function validateFactors(
  factors
) {
  const errors = [];

  if (!Array.isArray(factors)) {
    return {
      valid: false,
      errors: [
        'factors must be an array'
      ],
      value: []
    };
  }

  if (factors.length > MAX_FACTORS) {
    errors.push(
      `factors must contain no more than ${MAX_FACTORS} items`
    );
  }

  const normalizedFactors = [];

  factors
    .slice(0, MAX_FACTORS)
    .forEach((factor, index) => {
      const normalized =
        normalizeRequiredText(
          factor
        );

      if (!normalized) {
        errors.push(
          `factors[${index}] must be a non-blank string`
        );

        return;
      }

      if (
        normalized.length >
        MAX_FACTOR_LENGTH
      ) {
        errors.push(
          `factors[${index}] is too long`
        );

        return;
      }

      normalizedFactors.push(
        normalized
      );
    });

  return {
    valid:
      errors.length === 0,

    errors,

    value:
      normalizedFactors
  };
}

function validateStockExplanationOutput(
  generated
) {
  if (
    generated === null ||
    typeof generated !== 'object' ||
    Array.isArray(generated)
  ) {
    return {
      valid: false,
      errors: [
        'generated output must be a JSON object'
      ],
      value: null
    };
  }

  const errors = [];

  const evidenceSentiment =
    normalizeRequiredText(
      generated.evidenceSentiment
    )?.toLowerCase();

  if (
    !evidenceSentiment ||
    !ALLOWED_EVIDENCE_SENTIMENTS.has(
      evidenceSentiment
    )
  ) {
    errors.push(
      'evidenceSentiment must be positive, negative, mixed, neutral, or insufficient'
    );
  }

  const summary =
    normalizeRequiredText(
      generated.summary
    );

  if (!summary) {
    errors.push(
      'summary is required'
    );
  } else if (
    summary.length >
    MAX_SUMMARY_LENGTH
  ) {
    errors.push(
      'summary is too long'
    );
  }

  const factorsResult =
    validateFactors(
      generated.factors
    );

  errors.push(
    ...factorsResult.errors
  );

  if (
    typeof generated.evidenceSufficient !==
    'boolean'
  ) {
    errors.push(
      'evidenceSufficient must be a boolean'
    );
  }

  if (
    evidenceSentiment ===
      'insufficient' &&
    generated.evidenceSufficient ===
      true
  ) {
    errors.push(
      'insufficient sentiment cannot have evidenceSufficient set to true'
    );
  }

  if (
    generated.evidenceSufficient ===
      false &&
    factorsResult.value.length > 0
  ) {
    errors.push(
      'factors must be empty when evidenceSufficient is false'
    );
  }

  if (
    generated.evidenceSufficient ===
      false &&
    evidenceSentiment &&
    evidenceSentiment !==
      'insufficient'
  ) {
    errors.push(
      'evidenceSentiment must be insufficient when evidenceSufficient is false'
    );
  }

  if (
    generated.evidenceSufficient ===
      true &&
    evidenceSentiment ===
      'insufficient'
  ) {
    errors.push(
      'evidenceSentiment cannot be insufficient when evidenceSufficient is true'
    );
  }

  if (errors.length > 0) {
    return {
      valid: false,
      errors,
      value: null
    };
  }

  return {
    valid: true,
    errors: [],
    value: {
      evidenceSentiment,
      summary,
      factors:
        factorsResult.value,

      evidenceSufficient:
        generated.evidenceSufficient
    }
  };
}

module.exports = {
  ALLOWED_EVIDENCE_SENTIMENTS,
  validateStockExplanationOutput
};

