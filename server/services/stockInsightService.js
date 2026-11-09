const { buildStockInsightPrompt } = require('./stockInsightPrompt');
const { assertValidLlmClient } = require('./llm/llmClientContract');
const {
  validateStockInsightOutput
} = require('../validators/stockInsightOutputValidator');
const { InvalidAiOutputError } = require('../errors/InvalidAiOutputError');
const { applyEvidencePolicy } = require('./stockEvidencePolicy');
const { buildFallbackExplanation } = require('./stockExplanationFallback');
const {
  INSIGHT_OUTPUT_POLICY_VERSION,
  findInsightPolicyViolations
} = require('./stockInsightOutputPolicy');

const DISCLAIMER = 'For informational purposes only.';

// Fixed wording: describes the data without forecasting.
const SHORT_TERM_CONTEXT =
  'The available data describes current movement but is insufficient for a guaranteed short-term forecast.';
const LONG_TERM_CONTEXT =
  'The supplied evidence does not by itself establish a long-term outlook.';

function buildInsufficientInsight(input, direction) {
  return {
    symbol: input.symbol,
    direction,
    evidenceSentiment: 'insufficient',
    summary:
      'The available evidence is insufficient to reliably summarise this stock.',
    shortTermContext: SHORT_TERM_CONTEXT,
    longTermContext: LONG_TERM_CONTEXT,
    factors: [],
    evidenceSufficient: false,
    disclaimer: DISCLAIMER
  };
}

function buildDeterministicInsight({
  input,
  direction,
  relativePerformance,
  evidence
}) {
  const base = buildFallbackExplanation({
    input,
    direction,
    relativePerformance,
    evidence,
    disclaimer: DISCLAIMER
  });

  return {
    symbol: base.symbol,
    direction: base.direction,
    evidenceSentiment: base.evidenceSentiment,
    summary: base.summary,
    shortTermContext: SHORT_TERM_CONTEXT,
    longTermContext: LONG_TERM_CONTEXT,
    factors: base.factors,
    evidenceSufficient: base.evidenceSufficient,
    disclaimer: DISCLAIMER
  };
}

function createStockInsightService({ llmClient }) {
  const validatedClient = assertValidLlmClient(llmClient);

  async function generateStockInsight(input) {
    const evidencePolicy = applyEvidencePolicy(input.evidence);

    const safeInput = { ...input, evidence: evidencePolicy.accepted };
    const prompt = buildStockInsightPrompt(safeInput);

    const metadataBase = {
      promptVersion: prompt.promptVersion,
      outputPolicyVersion: INSIGHT_OUTPUT_POLICY_VERSION,
      evidenceAccepted: evidencePolicy.accepted.length,
      evidenceRejected: evidencePolicy.rejected.length
    };

    if (safeInput.evidence.length === 0) {
      return {
        insight: buildInsufficientInsight(safeInput, prompt.direction),
        metadata: {
          ...metadataBase,
          providerCalled: false,
          fallbackUsed: false
        }
      };
    }

    const generated = await validatedClient.generateJson({
      systemInstruction: prompt.systemInstruction,
      userMessage: prompt.userMessage
    });

    const outputValidation = validateStockInsightOutput(generated);

    if (!outputValidation.valid) {
      throw new InvalidAiOutputError(outputValidation.errors);
    }

    const violations = findInsightPolicyViolations({
      generated: outputValidation.value,
      evidence: safeInput.evidence
    });

    if (violations.length > 0) {
      // Not a transient failure: no retry. Serve a deterministic insight and
      // never expose the rejected wording.
      return {
        insight: buildDeterministicInsight({
          input: safeInput,
          direction: prompt.direction,
          relativePerformance: prompt.relativePerformance,
          evidence: safeInput.evidence
        }),
        metadata: {
          ...metadataBase,
          providerCalled: true,
          fallbackUsed: true,
          fallbackReason: violations[0]
        }
      };
    }

    const value = outputValidation.value;

    return {
      insight: {
        symbol: safeInput.symbol,
        direction: prompt.direction,
        evidenceSentiment: value.evidenceSentiment,
        summary: value.summary,
        shortTermContext: value.shortTermContext,
        longTermContext: value.longTermContext,
        factors: value.factors,
        evidenceSufficient: value.evidenceSufficient,
        disclaimer: DISCLAIMER
      },
      metadata: {
        ...metadataBase,
        providerCalled: true,
        fallbackUsed: false
      }
    };
  }

  return { generateStockInsight };
}

module.exports = {
  DISCLAIMER,
  SHORT_TERM_CONTEXT,
  LONG_TERM_CONTEXT,
  createStockInsightService
};

