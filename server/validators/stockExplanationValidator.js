const SUPPORTED_EVIDENCE_TYPES = new Set([
  'news',
  'earnings',
  'market',
  'company_event'
]);

const MAX_EVIDENCE_ITEMS = 7;
const MAX_HEADLINE_LENGTH = 500;
const MAX_SOURCE_LENGTH = 200;
const MAX_SYMBOL_LENGTH = 15;
const MAX_COMPANY_NAME_LENGTH = 200;

function isFiniteNumber(value) {
  return (
    typeof value === 'number' &&
    Number.isFinite(value)
  );
}

function normalizeSymbol(value) {
  if (typeof value !== 'string') {
    return '';
  }

  return value.trim().toUpperCase();
}

function normalizeOptionalText(value) {
  if (typeof value !== 'string') {
    return null;
  }

  const normalized = value.trim();

  return normalized.length > 0
    ? normalized
    : null;
}

function validateEvidence(evidence) {
  if (!Array.isArray(evidence)) {
    return {
      valid: false,
      errors: [
        'evidence must be an array'
      ],
      value: []
    };
  }

  const errors = [];

  if (evidence.length > MAX_EVIDENCE_ITEMS) {
    errors.push(
      `evidence must contain no more than ${MAX_EVIDENCE_ITEMS} items`
    );
  }

  const normalizedEvidence = [];

  evidence
    .slice(0, MAX_EVIDENCE_ITEMS)
    .forEach((item, index) => {
      if (
        item === null ||
        typeof item !== 'object' ||
        Array.isArray(item)
      ) {
        errors.push(
          `evidence[${index}] must be an object`
        );

        return;
      }

      const type =
        normalizeOptionalText(item.type)
          ?.toLowerCase();

      const headline =
        normalizeOptionalText(item.headline);

      const source =
        normalizeOptionalText(item.source);

      if (
        !type ||
        !SUPPORTED_EVIDENCE_TYPES.has(type)
      ) {
        errors.push(
          `evidence[${index}].type is unsupported`
        );
      }

      if (!headline) {
        errors.push(
          `evidence[${index}].headline is required`
        );
      } else if (
        headline.length > MAX_HEADLINE_LENGTH
      ) {
        errors.push(
          `evidence[${index}].headline is too long`
        );
      }

      if (
        source &&
        source.length > MAX_SOURCE_LENGTH
      ) {
        errors.push(
          `evidence[${index}].source is too long`
        );
      }

      if (
        type &&
        SUPPORTED_EVIDENCE_TYPES.has(type) &&
        headline &&
        headline.length <= MAX_HEADLINE_LENGTH &&
        (
          !source ||
          source.length <= MAX_SOURCE_LENGTH
        )
      ) {
        normalizedEvidence.push({
          type,
          headline,
          source
        });
      }
    });

  return {
    valid: errors.length === 0,
    errors,
    value: normalizedEvidence
  };
}

function validateStockExplanationRequest(body) {
  const errors = [];

  if (
    body === null ||
    typeof body !== 'object' ||
    Array.isArray(body)
  ) {
    return {
      valid: false,
      errors: [
        'request body must be a JSON object'
      ],
      value: null
    };
  }

  const symbol =
    normalizeSymbol(body.symbol);

  const companyName =
    normalizeOptionalText(body.companyName);

  if (!symbol) {
    errors.push(
      'symbol is required'
    );
  } else if (
    symbol.length > MAX_SYMBOL_LENGTH
  ) {
    errors.push(
      `symbol must contain no more than ${MAX_SYMBOL_LENGTH} characters`
    );
  }

  if (
    companyName &&
    companyName.length > MAX_COMPANY_NAME_LENGTH
  ) {
    errors.push(
      `companyName must contain no more than ${MAX_COMPANY_NAME_LENGTH} characters`
    );
  }

  if (
    !isFiniteNumber(
      body.priceChangePercent
    )
  ) {
    errors.push(
      'priceChangePercent must be a finite number'
    );
  }

  if (
    body.marketChangePercent !== null &&
    body.marketChangePercent !== undefined &&
    !isFiniteNumber(
      body.marketChangePercent
    )
  ) {
    errors.push(
      'marketChangePercent must be a finite number when provided'
    );
  }

  if (
    body.dayLow !== null &&
    body.dayLow !== undefined &&
    (
      !isFiniteNumber(body.dayLow) ||
      body.dayLow < 0
    )
  ) {
    errors.push(
      'dayLow must be a non-negative finite number when provided'
    );
  }

  if (
    body.dayHigh !== null &&
    body.dayHigh !== undefined &&
    (
      !isFiniteNumber(body.dayHigh) ||
      body.dayHigh < 0
    )
  ) {
    errors.push(
      'dayHigh must be a non-negative finite number when provided'
    );
  }

  if (
    isFiniteNumber(body.dayLow) &&
    isFiniteNumber(body.dayHigh) &&
    body.dayLow > body.dayHigh
  ) {
    errors.push(
      'dayLow must not be greater than dayHigh'
    );
  }

  const evidenceResult =
    validateEvidence(
      body.evidence
    );

  errors.push(
    ...evidenceResult.errors
  );

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
      symbol,
      companyName,
      priceChangePercent:
        body.priceChangePercent,

      marketChangePercent:
        body.marketChangePercent ?? null,

      dayLow:
        body.dayLow ?? null,

      dayHigh:
        body.dayHigh ?? null,

      evidence:
        evidenceResult.value
    }
  };
}

module.exports = {
  validateStockExplanationRequest
};

