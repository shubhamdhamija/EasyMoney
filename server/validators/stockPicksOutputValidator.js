const MAX_REASON_LENGTH = 400;

/**
 * Structural validation only. Symbol membership, duplicates, counts and
 * wording are policy rules (see stockPicksOutputPolicy.js) and trigger the
 * deterministic fallback instead of an error.
 */
function validateStockPicksOutput(generated) {
  if (generated === null || typeof generated !== 'object' || Array.isArray(generated)) {
    return {
      valid: false,
      errors: ['generated output must be a JSON object'],
      value: null
    };
  }

  if (!Array.isArray(generated.picks)) {
    return { valid: false, errors: ['picks must be an array'], value: null };
  }

  const errors = [];
  const picks = [];

  generated.picks.forEach((item, index) => {
    if (item === null || typeof item !== 'object' || Array.isArray(item)) {
      errors.push(`picks[${index}] must be an object`);
      return;
    }

    const symbol =
      typeof item.symbol === 'string' ? item.symbol.trim().toUpperCase() : '';
    const reason = typeof item.reason === 'string' ? item.reason.trim() : '';

    if (!symbol) {
      errors.push(`picks[${index}].symbol is required`);
    }

    if (!reason) {
      errors.push(`picks[${index}].reason is required`);
    } else if (reason.length > MAX_REASON_LENGTH) {
      errors.push(`picks[${index}].reason is too long`);
    }

    if (symbol && reason && reason.length <= MAX_REASON_LENGTH) {
      picks.push({ symbol, reason });
    }
  });

  if (errors.length > 0) {
    return { valid: false, errors, value: null };
  }

  return { valid: true, errors: [], value: { picks } };
}

module.exports = { MAX_REASON_LENGTH, validateStockPicksOutput };
