const test = require('node:test');
const assert = require('node:assert/strict');

const {
  createGroqLlmClient,
  parseGeneratedJson,
  removeMarkdownFence
} = require(
  '../services/llm/groqLlmClient'
);

function createFetchResponse({
  ok = true,
  status = 200,
  body = {},
  jsonError = null
} = {}) {
  return {
    ok,
    status,

    async json() {
      if (jsonError) {
        throw jsonError;
      }

      return body;
    }
  };
}

test(
  'Groq client sends prompt and parses generated JSON',
  async () => {
    const calls = [];

    const fetchImpl =
      async (url, options) => {
        calls.push({
          url,
          options
        });

        return createFetchResponse({
          body: {
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
          }
        });
      };

    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl
      });

    const result =
      await client.generateJson({
        systemInstruction:
          'Use supplied evidence only.',

        userMessage:
          'Symbol: AAPL'
      });

    assert.equal(
      calls.length,
      1
    );

    assert.equal(
      calls[0].url,
      'https://api.groq.com/openai/v1/chat/completions'
    );

    assert.equal(
      calls[0]
        .options
        .headers
        .Authorization,
      'Bearer gsk_test_key'
    );

    const requestBody =
      JSON.parse(
        calls[0]
          .options
          .body
      );

    assert.equal(
      requestBody.messages[0].role,
      'system'
    );

    assert.equal(
      requestBody.messages[1].role,
      'user'
    );

    assert.equal(
      requestBody
        .response_format
        .type,
      'json_object'
    );

    assert.equal(
      result.evidenceSentiment,
      'positive'
    );
  }
);

test(
  'Groq client parses markdown-fenced JSON',
  () => {
    const result =
      parseGeneratedJson(`
\`\`\`json
{
  "evidenceSentiment": "neutral",
  "summary": "Neutral evidence.",
  "factors": [],
  "evidenceSufficient": true
}
\`\`\`
`);

    assert.equal(
      result.evidenceSentiment,
      'neutral'
    );
  }
);

test(
  'markdown fence helper leaves plain JSON untouched',
  () => {
    assert.equal(
      removeMarkdownFence(
        '{"ok":true}'
      ),
      '{"ok":true}'
    );
  }
);

test(
  'Groq 429 becomes rate-limited error',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () =>
            createFetchResponse({
              ok: false,
              status: 429
            })
      });

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
          'AI_PROVIDER_RATE_LIMITED'
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
  'Groq authentication failure is mapped safely',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () => ({
            ok: false,
            status: 401
          })
      });

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
          'AI_PROVIDER_AUTHENTICATION_FAILED'
        );

        assert.equal(
          error.providerStatus,
          401
        );

        return true;
      }
    );
  }
);

test(
  'Groq 5xx becomes provider unavailable error',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () => ({
            ok: false,
            status: 503
          })
      });

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
          'AI_PROVIDER_UNAVAILABLE'
        );

        assert.equal(
          error.providerStatus,
          503
        );

        return true;
      }
    );
  }
);

test(
  'invalid Groq model JSON is rejected',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () =>
            createFetchResponse({
              body: {
                choices: [
                  {
                    message: {
                      content:
                        'not valid json'
                    }
                  }
                ]
              }
            })
      });

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
          'AI_PROVIDER_INVALID_JSON'
        );

        return true;
      }
    );
  }
);

test(
  'empty Groq response is rejected',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () =>
            createFetchResponse({
              body: {
                choices: []
              }
            })
      });

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
          'AI_PROVIDER_EMPTY_RESPONSE'
        );

        return true;
      }
    );
  }
);

test(
  'Groq client converts unreadable provider JSON responses',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () =>
            createFetchResponse({
              jsonError:
                new SyntaxError(
                  'Unexpected token <'
                )
            })
      });

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
          'AI_PROVIDER_INVALID_RESPONSE'
        );

        return true;
      }
    );
  }
);

test(
  'Groq client rejects a missing API key',
  () => {
    assert.throws(
      () =>
        createGroqLlmClient({
          apiKey: ''
        }),

      /GROQ_API_KEY/
    );
  }
);

test(
  'Groq client wraps network failures safely',
  async () => {
    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        fetchImpl:
          async () => {
            throw new Error(
              'connect ECONNREFUSED'
            );
          }
      });

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
          'AI_PROVIDER_NETWORK_ERROR'
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
  'aborted Groq request becomes timeout error',
  async () => {
    const fetchImpl =
      (url, options) =>
        new Promise(
          (resolve, reject) => {
            void url;
            void resolve;

            options.signal.addEventListener(
              'abort',
              () => {
                const error =
                  new Error(
                    'Request aborted'
                  );

                error.name =
                  'AbortError';

                reject(error);
              }
            );
          }
        );

    const client =
      createGroqLlmClient({
        apiKey:
          'gsk_test_key',

        timeoutMs:
          5,

        fetchImpl
      });

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
          'AI_PROVIDER_TIMEOUT'
        );

        assert.equal(
          error.statusCode,
          504
        );

        return true;
      }
    );
  }
);

test(
  'Groq client rejects an invalid timeout',
  () => {
    assert.throws(
      () =>
        createGroqLlmClient({
          apiKey:
            'gsk_test_key',

          timeoutMs:
            0,

          fetchImpl:
            async () => createFetchResponse()
        }),

      /timeoutMs/
    );
  }
);

test(
  'Groq request leaves room for reasoning tokens',
  async () => {
    let sentBody;

    const fetchImpl = async (url, options) => {
      sentBody = JSON.parse(options.body);

      return createFetchResponse({
        body: {
          choices: [
            {
              message: {
                content: JSON.stringify({ ok: true })
              }
            }
          ]
        }
      });
    };

    const gptOss = createGroqLlmClient({
      apiKey: 'gsk_test_key',
      model: 'openai/gpt-oss-20b',
      fetchImpl
    });

    await gptOss.generateJson({
      systemInstruction: 's',
      userMessage: 'u'
    });

    assert.ok(sentBody.max_completion_tokens >= 1500);
    assert.equal(sentBody.reasoning_effort, 'low');

    const other = createGroqLlmClient({
      apiKey: 'gsk_test_key',
      model: 'llama-3.3-70b-versatile',
      fetchImpl
    });

    await other.generateJson({
      systemInstruction: 's',
      userMessage: 'u'
    });

    assert.equal('reasoning_effort' in sentBody, false);
  }
);
