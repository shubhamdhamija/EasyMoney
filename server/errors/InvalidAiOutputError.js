class InvalidAiOutputError
  extends Error {

  constructor(
    validationErrors
  ) {
    super(
      'The AI provider returned an invalid stock explanation.'
    );

    this.name =
      'InvalidAiOutputError';

    this.code =
      'INVALID_AI_OUTPUT';

    this.validationErrors =
      Array.isArray(validationErrors)
        ? validationErrors
        : [];

    this.statusCode = 502;
  }
}

module.exports = {
  InvalidAiOutputError
};

