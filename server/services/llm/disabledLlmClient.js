const {
  assertValidLlmClient
} = require(
  './llmClientContract'
);

const {
  LlmProviderError
} = require(
  '../../errors/LlmProviderError'
);

function createDisabledLlmClient() {
  return assertValidLlmClient({
    providerName:
      'disabled',

    model:
      null,

    async generateJson() {
      throw new LlmProviderError({
        code:
          'AI_LIVE_CALLS_DISABLED',

        message:
          'Live AI calls are disabled.',

        statusCode:
          503
      });
    }
  });
}

module.exports = {
  createDisabledLlmClient
};

