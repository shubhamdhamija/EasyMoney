/**
 * Deterministic, conservative explanation used when the model's output is
 * rejected by the output policy. It is built only from validated market data
 * and accepted evidence, never calls a provider, and makes no claim about
 * causation, investor psychology, advice or future prices.
 */

const FALLBACK_REASON_UNSUPPORTED_INTERPRETATION =
  'UNSUPPORTED_INTERPRETATION';

const MAX_FACTORS = 3;
const MAX_FACTOR_LENGTH = 500;

const ADVICE_PATTERN =
  /\b(buy|sell|hold)\b/i;

const {
  PSYCHOLOGY_PATTERNS
} = require(
  './stockExplanationOutputPolicy'
);

function formatMagnitude(
  value
) {
  return `${Math.abs(value).toFixed(2)}%`;
}

function describeMovement(
  value,
  subject
) {
  if (value > 0) {
    return `${subject} increased ${formatMagnitude(value)}`;
  }

  if (value < 0) {
    return `${subject} decreased ${formatMagnitude(value)}`;
  }

  return `${subject} was unchanged`;
}

function describeRelativePerformance(
  relativePerformance
) {
  switch (relativePerformance) {
    case 'outperforming':
      return 'The stock was outperforming the broader market.';

    case 'underperforming':
      return 'The stock was underperforming the broader market.';

    case 'in_line':
      return 'The stock was in line with the broader market.';

    default:
      return null;
  }
}

function isUsableFactorText(
  text
) {
  if (typeof text !== 'string') {
    return false;
  }

  const trimmed = text.trim();

  if (trimmed.length === 0) {
    return false;
  }

  return (
    !ADVICE_PATTERN.test(trimmed) &&
    !PSYCHOLOGY_PATTERNS.some(
      pattern => pattern.test(trimmed)
    )
  );
}

function toFactor(
  headline
) {
  const trimmed =
    headline.trim();

  return trimmed.length >
    MAX_FACTOR_LENGTH
    ? `${trimmed
        .slice(0, MAX_FACTOR_LENGTH - 1)
        .trimEnd()}…`
    : trimmed;
}

function buildFallbackExplanation({
  input,
  direction,
  relativePerformance,
  evidence,
  disclaimer
}) {
  const factors =
    (evidence || [])
      .map(item => item?.headline)
      .filter(isUsableFactorText)
      .slice(0, MAX_FACTORS)
      .map(toFactor);

  if (factors.length === 0) {
    // Nothing safe to show: fall back to the neutral insufficient state.
    return {
      symbol: input.symbol,
      direction,
      evidenceSentiment:
        'insufficient',
      summary:
        'The available evidence is insufficient to reliably explain this movement.',
      factors: [],
      evidenceSufficient:
        false,
      disclaimer
    };
  }

  const sentences = [];

  let movement =
    describeMovement(
      input.priceChangePercent,
      input.symbol
    );

  if (
    input.marketChangePercent !== null &&
    input.marketChangePercent !== undefined
  ) {
    movement += ` while ${describeMovement(
      input.marketChangePercent,
      'the broader market'
    )}`;
  }

  sentences.push(`${movement}.`);

  const relative =
    describeRelativePerformance(
      relativePerformance
    );

  if (relative) {
    sentences.push(relative);
  }

  sentences.push(
    'The supplied evidence is listed below.'
  );

  sentences.push(
    'The available information does not establish how strongly any of it contributed to the movement.'
  );

  return {
    symbol: input.symbol,
    direction,

    // No interpretation of the evidence is attempted, so no tone is claimed.
    evidenceSentiment:
      'neutral',

    summary:
      sentences.join(' '),

    factors,

    evidenceSufficient:
      true,

    disclaimer
  };
}

module.exports = {
  FALLBACK_REASON_UNSUPPORTED_INTERPRETATION,
  buildFallbackExplanation
};

