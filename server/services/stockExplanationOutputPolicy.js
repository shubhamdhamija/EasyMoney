/**
 * Deterministic output-grounding policy.
 *
 * Prompt instructions alone do not reliably stop the model from inventing
 * investor psychology. This policy rejects generated text that uses such
 * wording unless the same wording appears in the supplied evidence.
 */

const OUTPUT_POLICY_VERSION =
  'stock-output-policy-v1';

const UNSUPPORTED_INTERPRETATION =
  'UNSUPPORTED_INTERPRETATION';

const PSYCHOLOGY_PATTERNS = [
  /\binvestor confidence\b/i,
  /\binvestor sentiment\b/i,
  /\binvestor enthusiasm\b/i,
  /\binvestor optimism\b/i,
  /\binvestor fear\b/i,
  /\bmarket optimism\b/i,
  /\btrader reaction\b/i,
  /\btrader sentiment\b/i,
  // Broader psychology wording that the evaluation heuristic also flags.
  /\binvestors?\b/i,
  /\btraders?\b/i,
  /\benthusiasm\b/i,
  /\boptimis(?:m|tic)\b/i,
  /\bpanic\b/i,
  /\bexcitement\b/i
];

function collectGeneratedText(
  generated
) {
  return [
    generated?.summary,
    ...(generated?.factors || [])
  ]
    .filter(Boolean)
    .join(' ');
}

function collectEvidenceText(
  evidence
) {
  return (evidence || [])
    .flatMap(item => [
      item?.headline,
      item?.source
    ])
    .filter(Boolean)
    .join(' ');
}

/**
 * Returns the source of each pattern that matches the generated summary or
 * factors but is absent from the evidence. The generated wording itself is
 * never returned.
 */
function findUnsupportedPsychology({
  generated,
  evidence
}) {
  const generatedText =
    collectGeneratedText(generated);

  const evidenceText =
    collectEvidenceText(evidence);

  return PSYCHOLOGY_PATTERNS
    .filter(
      pattern =>
        pattern.test(generatedText) &&
        !pattern.test(evidenceText)
    )
    .map(
      pattern =>
        pattern.source
    );
}

module.exports = {
  OUTPUT_POLICY_VERSION,
  UNSUPPORTED_INTERPRETATION,
  PSYCHOLOGY_PATTERNS,
  findUnsupportedPsychology
};

