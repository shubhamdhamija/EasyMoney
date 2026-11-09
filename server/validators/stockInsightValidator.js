const {
  validateStockExplanationRequest
} = require('./stockExplanationValidator');

function isFiniteNumber(value) {
  return typeof value === 'number' && Number.isFinite(value);
}

/**
 * Request validator for POST /ai/stock-insight.
 *
 * Shares symbol, price-change, day-range and evidence rules with the stock
 * explanation request, and adds an optional non-negative `currentPrice`.
 */
function validateStockInsightRequest(body) {
  const base = validateStockExplanationRequest(body);

  const errors = [...base.errors];

  const hasCurrentPrice =
    body !== null &&
    typeof body === 'object' &&
    body.currentPrice !== null &&
    body.currentPrice !== undefined;

  if (
    hasCurrentPrice &&
    (!isFiniteNumber(body.currentPrice) || body.currentPrice < 0)
  ) {
    errors.push(
      'currentPrice must be a non-negative finite number when provided'
    );
  }

  if (errors.length > 0) {
    return { valid: false, errors, value: null };
  }

  return {
    valid: true,
    errors: [],
    value: {
      ...base.value,
      currentPrice: hasCurrentPrice ? body.currentPrice : null
    }
  };
}

module.exports = { validateStockInsightRequest };

