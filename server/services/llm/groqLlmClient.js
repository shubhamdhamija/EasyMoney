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

const DEFAULT_BASE_URL =
  'https://api.groq.com/openai/v1';

const DEFAULT_MODEL =
  'openai/gpt-oss-20b';

const DEFAULT_TIMEOUT_MS =
  30_000;

const MAX_COMPLETION_TOKENS =
  1500;

function removeTrailingSlash(
  value
) {
  return value.replace(
    /\/+$/,
    ''
  );
}

function removeMarkdownFence(
  rawContent
) {
  const trimmed =
    rawContent.trim();

  const match =
    trimmed.match(
      /^```(?:json)?\s*([\s\S]*?)\s*```$/i
    );

  return match
    ? match[1].trim()
    : trimmed;
}

function extractGeneratedContent(
  providerResponse
) {
  const content =
    providerResponse
      ?.choices?.[0]
      ?.message
      ?.content;

  if (
    typeof content !== 'string' ||
    content.trim().length === 0
  ) {
    throw new LlmProviderError({
      code:
        'AI_PROVIDER_EMPTY_RESPONSE',

      message:
        'The AI provider returned empty content.',

      statusCode:
        502
    });
  }

  return content.trim();
}

function parseGeneratedJson(
  rawContent
) {
  const cleaned =
    removeMarkdownFence(
      rawContent
    );

  try {
    return JSON.parse(
      cleaned
    );
  } catch (cause) {
    throw new LlmProviderError({
      code:
        'AI_PROVIDER_INVALID_JSON',

      message:
        'The AI provider returned invalid JSON.',

      statusCode:
        502,

      cause
    });
  }
}

function mapProviderHttpError(
  status
) {
  if (status === 401 || status === 403) {
    return new LlmProviderError({
      code:
        'AI_PROVIDER_AUTHENTICATION_FAILED',

      message:
        'The AI provider rejected the configured credentials.',

      statusCode:
        503,

      providerStatus:
        status
    });
  }

  if (status === 429) {
    return new LlmProviderError({
      code:
        'AI_PROVIDER_RATE_LIMITED',

      message:
        'The AI provider rate limit was reached.',

      statusCode:
        503,

      providerStatus:
        status
    });
  }

  if (status >= 500 && status <= 599) {
    return new LlmProviderError({
      code:
        'AI_PROVIDER_UNAVAILABLE',

      message:
        'The AI provider is temporarily unavailable.',

      statusCode:
        503,

      providerStatus:
        status
    });
  }

  return new LlmProviderError({
    code:
      'AI_PROVIDER_REQUEST_FAILED',

    message:
      'The AI provider rejected the request.',

    statusCode:
      502,

    providerStatus:
      status
  });
}

function createGroqLlmClient({
  apiKey,
  baseUrl =
    DEFAULT_BASE_URL,
  model =
    DEFAULT_MODEL,
  timeoutMs =
    DEFAULT_TIMEOUT_MS,
  fetchImpl =
    globalThis.fetch
}) {
  if (
    typeof apiKey !== 'string' ||
    apiKey.trim().length === 0
  ) {
    throw new TypeError(
      'GROQ_API_KEY is required'
    );
  }

  if (
    typeof fetchImpl !==
    'function'
  ) {
    throw new TypeError(
      'fetchImpl must be a function'
    );
  }

  if (
    typeof baseUrl !== 'string' ||
    baseUrl.trim().length === 0
  ) {
    throw new TypeError(
      'baseUrl must be a non-empty string'
    );
  }

  if (
    typeof model !== 'string' ||
    model.trim().length === 0
  ) {
    throw new TypeError(
      'model must be a non-empty string'
    );
  }

  if (
    !Number.isFinite(timeoutMs) ||
    timeoutMs <= 0
  ) {
    throw new TypeError(
      'timeoutMs must be a positive finite number'
    );
  }

  const endpoint =
    `${removeTrailingSlash(
      baseUrl.trim()
    )}/chat/completions`;

  const client = {
    providerName:
      'groq',

    model:
      model.trim(),

    async generateJson({
      systemInstruction,
      userMessage
    }) {
      const controller =
        new AbortController();

      const timeoutHandle =
        setTimeout(
          () => controller.abort(),
          timeoutMs
        );

      try {
        const response =
          await fetchImpl(
            endpoint,
            {
              method:
                'POST',

              headers: {
                Authorization:
                  `Bearer ${apiKey.trim()}`,

                'Content-Type':
                  'application/json'
              },

              body:
                JSON.stringify({
                  model:
                    model.trim(),

                  messages: [
                    {
                      role:
                        'system',

                      content:
                        systemInstruction
                    },
                    {
                      role:
                        'user',

                      content:
                        userMessage
                    }
                  ],

                  response_format: {
                    type:
                      'json_object'
                  },

                  temperature:
                    0.2,

                  // Reasoning models spend completion tokens on hidden
                  // reasoning. A small budget can leave no room for the
                  // JSON answer (Groq: json_validate_failed).
                  max_completion_tokens:
                    MAX_COMPLETION_TOKENS,

                  ...(model
                    .trim()
                    .startsWith(
                      'openai/gpt-oss'
                    )
                    ? {
                        reasoning_effort:
                          'low'
                      }
                    : {})
                }),

              signal:
                controller.signal
            }
          );

        if (!response.ok) {
          throw mapProviderHttpError(
            response.status
          );
        }

        let providerResponse;

        try {
          providerResponse =
            await response.json();
        } catch (cause) {
          throw new LlmProviderError({
            code:
              'AI_PROVIDER_INVALID_RESPONSE',

            message:
              'The AI provider returned an unreadable response.',

            statusCode:
              502,

            cause
          });
        }

        const content =
          extractGeneratedContent(
            providerResponse
          );

        return parseGeneratedJson(
          content
        );
      } catch (error) {
        if (
          error instanceof
          LlmProviderError
        ) {
          throw error;
        }

        if (
          error?.name ===
          'AbortError'
        ) {
          throw new LlmProviderError({
            code:
              'AI_PROVIDER_TIMEOUT',

            message:
              'The AI provider request timed out.',

            statusCode:
              504,

            cause:
              error
          });
        }

        throw new LlmProviderError({
          code:
            'AI_PROVIDER_NETWORK_ERROR',

          message:
            'The AI provider could not be reached.',

          statusCode:
            503,

          cause:
            error
        });
      } finally {
        clearTimeout(
          timeoutHandle
        );
      }
    }
  };

  return assertValidLlmClient(
    client
  );
}

module.exports = {
  DEFAULT_BASE_URL,
  DEFAULT_MODEL,
  DEFAULT_TIMEOUT_MS,
  createGroqLlmClient,
  extractGeneratedContent,
  removeMarkdownFence,
  parseGeneratedJson,
  mapProviderHttpError
};
