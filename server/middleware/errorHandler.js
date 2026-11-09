const {
  InvalidAiOutputError
} = require(
  '../errors/InvalidAiOutputError'
);

const {
  LlmProviderError
} = require(
  '../errors/LlmProviderError'
);

function errorHandler(
  error,
  req,
  res,
  next
) {
  void next;

  const requestId =
    req.requestId || null;

  console.error(
    '[error]',
    {
      requestId,
      method:
        req.method,

      path:
        req.originalUrl,

      name:
        error?.name,

      code:
        error?.code,

      message:
        error?.message,

      // Server-side diagnostics only; never included in the response body.
      validationErrors:
        error?.validationErrors
    }
  );

  if (
    error instanceof
      InvalidAiOutputError ||
    error?.code ===
      'INVALID_AI_OUTPUT'
  ) {
    return res.status(502).json({
      ok: false,

      error: {
        code:
          'INVALID_AI_OUTPUT',

        message:
          'The AI provider returned an invalid response.'
      },

      requestId
    });
  }

  if (
    error instanceof
    LlmProviderError
  ) {
    const publicMessage =
      error.code ===
      'AI_LIVE_CALLS_DISABLED'
        ? 'AI explanation generation is currently disabled.'
        : 'The AI explanation service is temporarily unavailable.';

    return res
      .status(
        error.statusCode
      )
      .json({
        ok: false,

        error: {
          code:
            error.code,

          message:
            publicMessage
        },

        requestId
      });
  }

  return res.status(500).json({
    ok: false,

    error: {
      code:
        'INTERNAL_SERVER_ERROR',

      message:
        'An unexpected server error occurred.'
    },

    requestId
  });
}

module.exports = {
  errorHandler
};



