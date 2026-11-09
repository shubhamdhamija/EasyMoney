function assertValidLlmClient(
  llmClient
) {
  if (
    !llmClient ||
    typeof llmClient.generateJson !==
      'function'
  ) {
    throw new TypeError(
      'llmClient must provide a generateJson function'
    );
  }

  return llmClient;
}

module.exports = {
  assertValidLlmClient
};

