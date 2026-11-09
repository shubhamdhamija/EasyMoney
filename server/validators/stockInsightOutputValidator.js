const {
  validateStockExplanationOutput
} = require('./stockExplanationOutputValidator');

const MAX_CONTEXT_LENGTH = 600;

function normalizeContext(value) {
  if (typeof value !== 'string') {
    return null;
  }

  const trimmed = value.trim();

  return trimmed.length > 0 ? trimmed : null;
}

/**
 * Structural validation of the model's stock-insight JSON. The shared fields
 * (sentiment, summary, factors, sufficiency) reuse the explanation rules.
 */
function validateStockInsightOutput(generated) {
  const base = validateStockExplanationOutput(generated);
  const errors = [...base.errors];

  const isObject =
    generated !== null &&
    typeof generated === 'object' &&
    !Array.isArray(generated);

  let shortTermContext = null;
  let longTermContext = null;

  if (isObject) {
    shortTermContext = normalizeContext(generated.shortTermContext);
    longTermContext = normalizeContext(generated.longTermContext);

    if (!shortTermContext) {
      errors.push('shortTermContext is required');
    } else if (shortTermContext.length > MAX_CONTEXT_LENGTH) {
      errors.push('shortTermContext is too long');
    }

    if (!longTermContext) {
      errors.push('longTermContext is required');
    } else if (longTermContext.length > MAX_CONTEXT_LENGTH) {
      errors.push('longTermContext is too long');
    }
  }

  if (errors.length > 0) {
    return { valid: false, errors, value: null };
  }

  return {
    valid: true,
    errors: [],
    value: { ...base.value, shortTermContext, longTermContext }
  };
}

module.exports = { validateStockInsightOutput };

