const MAX_STOCKS = 30;
const MIN_PICKS = 1;
const MAX_PICKS = 5;
const DEFAULT_MAX_PICKS = 3;
const MAX_SYMBOL_LENGTH = 15;
const MAX_COMPANY_NAME_LENGTH = 200;

function isFiniteNumber(value) {
  return typeof value === 'number' && Number.isFinite(value);
}

function optionalNonNegative(value, name, errors) {
  if (value === null || value === undefined) {
    return null;
  }

  if (!isFiniteNumber(value) || value < 0) {
    errors.push(`${name} must be a non-negative finite number when provided`);
    return null;
  }

  return value;
}

/**
 * Validates POST /ai/picks. `maximumPicks` is an upper bound: when fewer stocks
 * are supplied the service simply returns at most that many picks.
 */
function validateStockPicksRequest(body) {
  if (body === null || typeof body !== 'object' || Array.isArray(body)) {
    return {
      valid: false,
      errors: ['request body must be a JSON object'],
      value: null
    };
  }

  const errors = [];

  let maximumPicks = DEFAULT_MAX_PICKS;

  if (body.maximumPicks !== undefined && body.maximumPicks !== null) {
    if (
      !Number.isInteger(body.maximumPicks) ||
      body.maximumPicks < MIN_PICKS ||
      body.maximumPicks > MAX_PICKS
    ) {
      errors.push(
        `maximumPicks must be an integer between ${MIN_PICKS} and ${MAX_PICKS}`
      );
    } else {
      maximumPicks = body.maximumPicks;
    }
  }

  const stocks = [];

  if (!Array.isArray(body.stocks)) {
    errors.push('stocks must be an array');
  } else if (body.stocks.length < 1) {
    errors.push('stocks must contain at least 1 stock');
  } else if (body.stocks.length > MAX_STOCKS) {
    errors.push(`stocks must contain no more than ${MAX_STOCKS} stocks`);
  } else {
    const seen = new Set();

    body.stocks.forEach((item, index) => {
      if (item === null || typeof item !== 'object' || Array.isArray(item)) {
        errors.push(`stocks[${index}] must be an object`);
        return;
      }

      const symbol =
        typeof item.symbol === 'string' ? item.symbol.trim().toUpperCase() : '';

      if (!symbol) {
        errors.push(`stocks[${index}].symbol is required`);
        return;
      }

      if (symbol.length > MAX_SYMBOL_LENGTH) {
        errors.push(`stocks[${index}].symbol is too long`);
        return;
      }

      if (seen.has(symbol)) {
        errors.push(`stocks[${index}].symbol is duplicated`);
        return;
      }

      seen.add(symbol);

      const rowErrors = [];

      if (!isFiniteNumber(item.priceChangePercent)) {
        rowErrors.push(
          `stocks[${index}].priceChangePercent must be a finite number`
        );
      }

      const currentPrice = optionalNonNegative(
        item.currentPrice,
        `stocks[${index}].currentPrice`,
        rowErrors
      );
      const dayLow = optionalNonNegative(
        item.dayLow,
        `stocks[${index}].dayLow`,
        rowErrors
      );
      const dayHigh = optionalNonNegative(
        item.dayHigh,
        `stocks[${index}].dayHigh`,
        rowErrors
      );

      const companyName =
        typeof item.companyName === 'string' && item.companyName.trim()
          ? item.companyName.trim()
          : null;

      if (companyName && companyName.length > MAX_COMPANY_NAME_LENGTH) {
        rowErrors.push(`stocks[${index}].companyName is too long`);
      }

      if (rowErrors.length > 0) {
        errors.push(...rowErrors);
        return;
      }

      stocks.push({
        symbol,
        companyName,
        currentPrice,
        priceChangePercent: item.priceChangePercent,
        dayLow,
        dayHigh
      });
    });
  }

  if (errors.length > 0) {
    return { valid: false, errors, value: null };
  }

  return { valid: true, errors: [], value: { stocks, maximumPicks } };
}

module.exports = {
  MAX_STOCKS,
  MAX_PICKS,
  DEFAULT_MAX_PICKS,
  validateStockPicksRequest
};
