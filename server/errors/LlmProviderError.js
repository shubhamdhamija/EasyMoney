class LlmProviderError extends Error {
  constructor({
    code,
    message,
    statusCode = 502,
    providerStatus = null,
    cause = null
  }) {
    super(message);

    this.name = 'LlmProviderError';
    this.code = code;
    this.statusCode = statusCode;
    this.providerStatus = providerStatus;
    this.cause = cause;
  }
}

module.exports = {
  LlmProviderError
};

