const {
  assertValidLlmClient
} = require(
  './llmClientContract'
);

function createFakeLlmClient(
  options = {}
) {
  const {
    response = {
      evidenceSentiment:
        'positive',

      summary:
        'The supplied evidence may have contributed to the observed movement.',

      factors: [
        'The supplied evidence was positive.'
      ],

      evidenceSufficient:
        true
    },

    error = null
  } = options;

  const calls = [];

  const client = {
    calls,

    async generateJson({
      systemInstruction,
      userMessage
    }) {
      calls.push({
        systemInstruction,
        userMessage
      });

      if (error) {
        throw error;
      }

      return response;
    }
  };

  return assertValidLlmClient(
    client
  );
}

module.exports = {
  createFakeLlmClient
};

