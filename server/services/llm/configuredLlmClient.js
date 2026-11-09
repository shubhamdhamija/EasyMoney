const {
  createGroqLlmClient
} = require(
  './groqLlmClient'
);

const {
  createDisabledLlmClient
} = require(
  './disabledLlmClient'
);

function isLiveAiEnabled(
  env
) {
  return (
    env.AI_LIVE_CALLS_ENABLED ===
    'true'
  );
}

function parsePositiveNumber(
  value,
  fallback
) {
  const parsed =
    Number(value);

  return (
    Number.isFinite(parsed) &&
    parsed > 0
  )
    ? parsed
    : fallback;
}

function createConfiguredLlmClient({
  env = process.env,
  fetchImpl = globalThis.fetch
}) {
  if (!isLiveAiEnabled(env)) {
    return createDisabledLlmClient();
  }

  const provider =
    env.AI_PROVIDER
      ?.trim()
      .toLowerCase();

  if (provider !== 'groq') {
    throw new Error(
      `Unsupported AI provider: ${
        provider || 'missing'
      }`
    );
  }

  return createGroqLlmClient({
    apiKey:
      env.GROQ_API_KEY,

    baseUrl:
      env.GROQ_BASE_URL,

    model:
      env.GROQ_MODEL,

    timeoutMs:
      parsePositiveNumber(
        env.AI_REQUEST_TIMEOUT_MS,
        30_000
      ),

    fetchImpl
  });
}

module.exports = {
  isLiveAiEnabled,
  parsePositiveNumber,
  createConfiguredLlmClient
};


