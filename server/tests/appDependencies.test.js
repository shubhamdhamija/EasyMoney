const test = require('node:test');
const assert = require('node:assert/strict');

const {
  createAppDependencies
} = require(
  '../appDependencies'
);

test(
  'app dependencies use disabled client when live calls are off',
  async () => {
    const dependencies =
      createAppDependencies({
        env: {
          AI_LIVE_CALLS_ENABLED:
            'false'
        }
      });

    assert.equal(
      dependencies
        .llmClient
        .providerName,
      'disabled'
    );

    await assert.rejects(
      () =>
        dependencies
          .llmClient
          .generateJson({
            systemInstruction:
              'System',

            userMessage:
              'User'
          }),

      /Live AI calls are disabled/
    );
  }
);

test(
  'app dependencies create Groq-backed service when enabled',
  async () => {
    const fetchImpl =
      async () => ({
        ok: true,
        status: 200,

        async json() {
          return {
            choices: [
              {
                message: {
                  content:
                    JSON.stringify({
                      evidenceSentiment:
                        'positive',

                      summary:
                        'Revenue evidence may have contributed.',

                      factors: [
                        'Revenue exceeded expectations.'
                      ],

                      evidenceSufficient:
                        true
                    })
                }
              }
            ]
          };
        }
      });

    const dependencies =
      createAppDependencies({
        env: {
          AI_LIVE_CALLS_ENABLED:
            'true',

          AI_PROVIDER:
            'groq',

          GROQ_API_KEY:
            'gsk_test_key',

          GROQ_BASE_URL:
            'https://api.groq.com/openai/v1',

          GROQ_MODEL:
            'openai/gpt-oss-20b',

          AI_REQUEST_TIMEOUT_MS:
            '30000'
        },

        fetchImpl
      });

    assert.equal(
      dependencies
        .llmClient
        .providerName,
      'groq'
    );

    const result =
      await dependencies
        .stockExplanationService
        .generateStockExplanation({
          symbol: 'AAPL',
          companyName: 'Apple Inc.',
          priceChangePercent: 2.4,
          marketChangePercent: 1.1,
          dayLow: 220.5,
          dayHigh: 228.3,
          evidence: [
            {
              type: 'news',
              headline:
                'Revenue exceeded expectations.',
              source:
                'Controlled test source'
            }
          ]
        });

    assert.equal(
      result.explanation
        .evidenceSentiment,
      'positive'
    );
  }
);

