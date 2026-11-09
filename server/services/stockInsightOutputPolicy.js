const {
  findUnsupportedPsychology
} = require('./stockExplanationOutputPolicy');

const INSIGHT_OUTPUT_POLICY_VERSION = 'stock-insight-output-policy-v1';

const UNSUPPORTED_INTERPRETATION = 'UNSUPPORTED_INTERPRETATION';
const UNSUPPORTED_FORECAST = 'UNSUPPORTED_FORECAST';
const INTERNAL_TERMINOLOGY_LEAK = 'INTERNAL_TERMINOLOGY_LEAK';
const BENCHMARK_MISREPRESENTATION = 'BENCHMARK_MISREPRESENTATION';

// The benchmark is the broader market, never peers or competitors, unless the
// supplied evidence itself uses those words.
const BENCHMARK_PATTERNS = [
  /\bpeers?\b/i,
  /\bpeer (?:companies|group)\b/i,
  /\bcompetitors?\b/i,
  /\bcomparable companies\b/i
];

// Internal prompt vocabulary must never reach users.
const INTERNAL_TERMINOLOGY_PATTERNS = [
  /\buntrusted evidence\b/i,
  /\btrusted market data\b/i,
  /\bsystem (?:instructions?|prompt)\b/i,
  /\buser message\b/i,
  /\bcontext block\b/i,
  /\bevidence items?\b/i,
  /\b(?:news|evidence|headline|article) (?:item|number|#) ?\d+\b/i,
  /\bthe prompt\b/i
];

const MOVEMENT_VERBS =
  'rise|rising|increase|grow|climb|rally|fall|decline|drop|gain|surge|' +
  'outperform|underperform|reach|continue|recover|rebound';

// Forecast, price-target, guarantee and advice wording. Word boundaries keep
// "shareholder" or "household" from matching "hold". A pattern only counts if
// the generated text matches it and the supplied evidence does not.
const FORECAST_PATTERNS = [
  new RegExp(
    `\\bwill (?:likely |probably |soon |certainly |definitely )?(?:${MOVEMENT_VERBS})\\b`,
    'i'
  ),
  new RegExp(
    `\\b(?:expected|likely|set|poised|on track|bound) to (?:${MOVEMENT_VERBS})\\b`,
    'i'
  ),
  /\bprice target\b/i,
  /\bguaranteed (?:to|return|returns|profit|profits|gain|gains|growth)\b/i,
  /\bwill (?:certainly|definitely)\b/i,
  /\b(?:buy|sell|hold)\b/i,
  /\bshould (?:invest|consider buying|consider selling)\b/i
];

function collectGeneratedText(generated) {
  return [
    generated.summary,
    generated.shortTermContext,
    generated.longTermContext,
    ...(generated.factors || [])
  ]
    .filter(Boolean)
    .join(' ');
}

function collectEvidenceText(evidence) {
  return (evidence || [])
    .flatMap(item => [item?.headline, item?.source])
    .filter(Boolean)
    .join(' ');
}

/**
 * Returns policy rule names that the generated insight violates. The generated
 * wording itself is never returned.
 */
function findInsightPolicyViolations({ generated, evidence }) {
  const violations = [];

  const psychology = findUnsupportedPsychology({
    generated: {
      summary: [
        generated.summary,
        generated.shortTermContext,
        generated.longTermContext
      ]
        .filter(Boolean)
        .join(' '),
      factors: generated.factors
    },
    evidence
  });

  if (psychology.length > 0) {
    violations.push(UNSUPPORTED_INTERPRETATION);
  }

  const generatedText = collectGeneratedText(generated);
  const evidenceText = collectEvidenceText(evidence);

  const forecast = FORECAST_PATTERNS.some(
    pattern => pattern.test(generatedText) && !pattern.test(evidenceText)
  );

  if (forecast) {
    violations.push(UNSUPPORTED_FORECAST);
  }

  if (INTERNAL_TERMINOLOGY_PATTERNS.some(pattern => pattern.test(generatedText))) {
    violations.push(INTERNAL_TERMINOLOGY_LEAK);
  }

  if (
    BENCHMARK_PATTERNS.some(
      pattern => pattern.test(generatedText) && !pattern.test(evidenceText)
    )
  ) {
    violations.push(BENCHMARK_MISREPRESENTATION);
  }

  return violations;
}

module.exports = {
  INSIGHT_OUTPUT_POLICY_VERSION,
  UNSUPPORTED_INTERPRETATION,
  UNSUPPORTED_FORECAST,
  INTERNAL_TERMINOLOGY_LEAK,
  FORECAST_PATTERNS,
  findInsightPolicyViolations
};







