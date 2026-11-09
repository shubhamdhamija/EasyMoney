const {
  buildStockExplanationPrompt
} = require(
  './stockExplanationPrompt'
);

const {
  assertValidLlmClient
} = require(
  './llm/llmClientContract'
);

const {
  validateStockExplanationOutput
} = require(
  '../validators/stockExplanationOutputValidator'
);

const {
  InvalidAiOutputError
} = require(
  '../errors/InvalidAiOutputError'
);

const {
  applyEvidencePolicy
} = require(
  './stockEvidencePolicy'
);

const {
  buildFallbackExplanation
} = require(
  './stockExplanationFallback'
);

const {
  findUnsupportedPsychology,
  OUTPUT_POLICY_VERSION,
  UNSUPPORTED_INTERPRETATION
} = require(
  './stockExplanationOutputPolicy'
);

const DISCLAIMER =
  'For informational purposes only.';

function buildInsufficientEvidenceResponse(
  input,
  direction
) {
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

    disclaimer:
      DISCLAIMER
  };
}

function buildFinalResponse(
  input,
  direction,
  generated
) {
  return {
    symbol: input.symbol,
    direction,

    evidenceSentiment:
      generated.evidenceSentiment,

    summary:
      generated.summary,

    factors:
      generated.factors,

    evidenceSufficient:
      generated.evidenceSufficient,

    disclaimer:
      DISCLAIMER
  };
}

function createStockExplanationService({
  llmClient
}) {
  const validatedClient =
    assertValidLlmClient(
      llmClient
    );

  async function generateStockExplanation(
    input
  ) {
    const evidencePolicy =
      applyEvidencePolicy(
        input.evidence
      );

    const safeInput = {
      ...input,
      evidence:
        evidencePolicy.accepted
    };

    const prompt =
      buildStockExplanationPrompt(
        safeInput
      );

    if (safeInput.evidence.length === 0) {
      return {
        explanation:
          buildInsufficientEvidenceResponse(
            safeInput,
            prompt.direction
          ),

        metadata: {
          promptVersion:
            prompt.promptVersion,

          providerCalled:
            false,

          fallbackUsed:
            false,

          evidenceAccepted:
            0,

          evidenceRejected:
            evidencePolicy.rejected.length
        }
      };
    }

    const generated =
      await validatedClient.generateJson({
        systemInstruction:
          prompt.systemInstruction,

        userMessage:
          prompt.userMessage
      });

    const outputValidation =
      validateStockExplanationOutput(
        generated
      );

    if (!outputValidation.valid) {
      throw new InvalidAiOutputError(
        outputValidation.errors
      );
    }

    // Deterministic grounding check. The generated wording is never exposed;
    // only the rule category is recorded (server-side) on the error.
    const unsupported =
      findUnsupportedPsychology({
        generated:
          outputValidation.value,

        evidence:
          safeInput.evidence
      });

    if (unsupported.length > 0) {
      // A policy rejection is not a transient provider failure: do not retry
      // the model. Return a conservative deterministic explanation instead,
      // and never expose the rejected wording.
      return {
        explanation:
          buildFallbackExplanation({
            input,
            direction:
              prompt.direction,
            relativePerformance:
              prompt.relativePerformance,
            evidence:
              safeInput.evidence,
            disclaimer:
              DISCLAIMER
          }),

        metadata: {
          promptVersion:
            prompt.promptVersion,

          outputPolicyVersion:
            OUTPUT_POLICY_VERSION,

          providerCalled:
            true,

          fallbackUsed:
            true,

          fallbackReason:
            UNSUPPORTED_INTERPRETATION,

          evidenceAccepted:
            evidencePolicy.accepted.length,

          evidenceRejected:
            evidencePolicy.rejected.length
        }
      };
    }

    return {
      explanation:
        buildFinalResponse(
          input,
          prompt.direction,
          outputValidation.value
        ),

      metadata: {
        promptVersion:
          prompt.promptVersion,

        outputPolicyVersion:
          OUTPUT_POLICY_VERSION,

        providerCalled:
          true,

        fallbackUsed:
          false,

        evidenceAccepted:
          evidencePolicy.accepted.length,

        evidenceRejected:
          evidencePolicy.rejected.length
      }
    };
  }

  return {
    generateStockExplanation
  };
}

// TEMPORARY: kept only so the production route keeps working until a real
// LLM client is injected into it. Remove once the route uses
// createStockExplanationService.
function buildTemporaryValidatedResponse(
  input,
  direction
) {
  return {
    symbol: input.symbol,
    direction,
    evidenceSentiment:
      'neutral',

    summary:
      'The request and grounded prompt were created successfully. AI generation is not connected yet.',

    factors: [],

    evidenceSufficient:
      true,

    disclaimer:
      DISCLAIMER
  };
}

// TEMPORARY: see note above. Does not call any LLM.
async function generateStockExplanation(
  input
) {
  const prompt =
    buildStockExplanationPrompt(
      input
    );

  const explanation =
    input.evidence.length === 0
      ? buildInsufficientEvidenceResponse(
          input,
          prompt.direction
        )
      : buildTemporaryValidatedResponse(
          input,
          prompt.direction
        );

  return {
    explanation,

    metadata: {
      promptVersion:
        prompt.promptVersion,

      providerCalled:
        false
    }
  };
}

module.exports = {
  DISCLAIMER,
  createStockExplanationService,
  buildInsufficientEvidenceResponse,
  buildFinalResponse,
  buildTemporaryValidatedResponse,
  generateStockExplanation
};
