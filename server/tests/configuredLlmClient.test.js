const test = require('node:test');
const assert = require('node:assert/strict');

const {
  isLiveAiEnabled,
  parsePositiveNumber,
  createConfiguredLlmClient
} = require(
  '../services/llm/configuredLlmClient'
);

test(
  'live calls are enabled only by exact true value',
  () => {
    assert.equal(
      isLiveAiEnabled({
        AI_LIVE_CALLS_ENABLED:
          'true'
      }),
      true
    );

    assert.equal(
      isLiveAiEnabled({
        AI_LIVE_CALLS_ENABLED:
          'false'
      }),
      false
    );

    assert.equal(
      isLiveAiEnabled({
        AI_LIVE_CALLS_ENABLED:
          'TRUE'
      }),
      false
    );
  }
);

test(
  'positive number parser uses valid value',
  () => {
    assert.equal(
      parsePositiveNumber(
        '15000',
        30000
      ),
      15000
    );
  }
);

test(
  'positive number parser falls back for invalid value',
  () => {
    assert.equal(
      parsePositiveNumber(
        'invalid',
        30000
      ),
      30000
    );

    assert.equal(
      parsePositiveNumber(
        '-1',
        30000
      ),
      30000
    );
  }
);

test(
  'disabled live calls return a disabled client',
  async () => {
    const client =
      createConfiguredLlmClient({
        env: {
          AI_LIVE_CALLS_ENABLED:
            'false'
        }
      });

    assert.equal(
      client.providerName,
      'disabled'
    );

    await assert.rejects(
      () =>
        client.generateJson({
          systemInstruction:
            'System',

          userMessage:
            'User'
        }),

      error => {
        assert.equal(
          error.code,
          'AI_LIVE_CALLS_DISABLED'
        );

        assert.equal(
          error.statusCode,
          503
        );

        return true;
      }
    );
  }
);

test(
  'configured client rejects unsupported provider',
  () => {
    assert.throws(
      () =>
        createConfiguredLlmClient({
          env: {
            AI_LIVE_CALLS_ENABLED:
              'true',

            AI_PROVIDER:
              'unknown'
          }
        }),

      /Unsupported AI provider/
    );
  }
);

test(
  'configured client builds a Groq client when enabled',
  async () => {
    let capturedUrl = null;
    let capturedOptions = null;

    const client =
      createConfiguredLlmClient({
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
            '15000'
        },

        fetchImpl:
          async (url, options) => {
            capturedUrl = url;
            capturedOptions = options;

            return {
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
                              'neutral',

                            summary:
                              'Neutral evidence.',

                            factors: [],

                            evidenceSufficient:
                              true
                          })
                      }
                    }
                  ]
                };
              }
            };
          }
      });

    const result =
      await client.generateJson({
        systemInstruction:
          'System',

        userMessage:
          'User'
      });

    assert.equal(
      capturedUrl,
      'https://api.groq.com/openai/v1/chat/completions'
    );

    const body =
      JSON.parse(
        capturedOptions.body
      );

    assert.equal(
      body.model,
      'openai/gpt-oss-20b'
    );

    assert.equal(
      result.evidenceSentiment,
      'neutral'
    );
  }
);

test(
  'configured client falls back to default timeout for invalid env value',
  async () => {
    let aborted = false;

    const client =
      createConfiguredLlmClient({
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
            'invalid'
        },

        fetchImpl:
          async (url, options) => {
            void url;
            aborted =
              options.signal.aborted;

            return {
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
                              'neutral',

                            summary:
                              'Neutral evidence.',

                            factors: [],

                            evidenceSufficient:
                              true
                          })
                      }
                    }
                  ]
                };
              }
            };
          }
      });

    const result =
      await client.generateJson({
        systemInstruction:
          'System',

        userMessage:
          'User'
      });

    assert.equal(
      aborted,
      false
    );

    assert.equal(
      result.evidenceSufficient,
      true
    );
  }
);

