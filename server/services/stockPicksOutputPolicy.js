const { FORECAST_PATTERNS } = require('./stockInsightOutputPolicy');
const { findUnsupportedPsychology } = require('./stockExplanationOutputPolicy');

const PICKS_OUTPUT_POLICY_VERSION = 'stock-picks-output-policy-v1';

const UNSUPPORTED_SYMBOL = 'UNSUPPORTED_SYMBOL';
const DUPLICATE_PICK = 'DUPLICATE_PICK';
const TOO_MANY_PICKS = 'TOO_MANY_PICKS';
const EMPTY_PICKS = 'EMPTY_PICKS';
const RECOMMENDATION_LANGUAGE = 'RECOMMENDATION_LANGUAGE';
const UNSUPPORTED_FORECAST = 'UNSUPPORTED_FORECAST';
const UNSUPPORTED_INTERPRETATION = 'UNSUPPORTED_INTERPRETATION';
const INTERNAL_TERMINOLOGY_LEAK = 'INTERNAL_TERMINOLOGY_LEAK';

// Investment-recommendation framing. Word boundaries keep "shareholder" or
// "household" from matching "hold".
const RECOMMENDATION_PATTERNS = [
  /\b(?:buy|sell|hold)\b/i,
  /\brecommend(?:ed|s|ation|ations)?\b/i,
  /\bbest investments?\b/i,
  /\b(?:attractive|promising|undervalued|overvalued)\b/i,
  /\btop picks?\b/i,
  /\bmust[- ]have\b/i,
  /\b(?:guaranteed|risk[- ]free)\b/i,
  /\b(?:opportunity|opportunities)\b/i,
  /\bworth (?:buying|owning|watching closely)\b/i
];

const INTERNAL_TERMINOLOGY_PATTERNS = [
  /\bsupplied_stock_set\b/i,
  /\bsystem (?:instructions?|prompt)\b/i,
  /\buser message\b/i,
  /\bthe prompt\b/i,
  /\buntrusted\b/i
];

function findStockPicksPolicyViolations({ picks, allowedSymbols, maximumPicks }) {
  const violations = new Set();
  const allowed = new Set(allowedSymbols);

  if (picks.length === 0) {
    violations.add(EMPTY_PICKS);
  }

  if (picks.length > maximumPicks) {
    violations.add(TOO_MANY_PICKS);
  }

  const seen = new Set();

  picks.forEach(pick => {
    if (!allowed.has(pick.symbol)) {
      violations.add(UNSUPPORTED_SYMBOL);
    }

    if (seen.has(pick.symbol)) {
      violations.add(DUPLICATE_PICK);
    }

    seen.add(pick.symbol);
  });

  const text = picks.map(pick => pick.reason).join(' ');

  if (RECOMMENDATION_PATTERNS.some(pattern => pattern.test(text))) {
    violations.add(RECOMMENDATION_LANGUAGE);
  }

  if (FORECAST_PATTERNS.some(pattern => pattern.test(text))) {
    violations.add(UNSUPPORTED_FORECAST);
  }

  if (INTERNAL_TERMINOLOGY_PATTERNS.some(pattern => pattern.test(text))) {
    violations.add(INTERNAL_TERMINOLOGY_LEAK);
  }

  if (
    findUnsupportedPsychology({
      generated: { summary: text, factors: [] },
      evidence: []
    }).length > 0
  ) {
    violations.add(UNSUPPORTED_INTERPRETATION);
  }

  return Array.from(violations);
}

module.exports = {
  PICKS_OUTPUT_POLICY_VERSION,
  RECOMMENDATION_PATTERNS,
  findStockPicksPolicyViolations
};
