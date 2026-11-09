const express = require('express');

const {
  validateStockExplanationRequest
} = require(
  '../validators/stockExplanationValidator'
);

const {
  generateStockExplanation
} = require(
  '../services/stockExplanationService'
);

function defaultLogger(entry) {
  console.log(
    `POST /ai/explain ${JSON.stringify(entry)}`
  );
}

function createStockExplanationRouter({
  stockExplanationService,
  logger = defaultLogger
}) {
  if (
    !stockExplanationService ||
    typeof stockExplanationService
      .generateStockExplanation !==
    'function'
  ) {
    throw new TypeError(
      'stockExplanationService must provide generateStockExplanation'
    );
  }

  const router = express.Router();

  router.post(
    '/',
    async (req, res, next) => {
      // Safe fields only: never prompts, article text, headers or keys.
      const logEntry = {
        requestId: req.requestId || null,
        route: '/ai/explain',
        symbol:
          typeof req.body?.symbol === 'string'
            ? req.body.symbol.slice(0, 12)
            : null,
        promptVersion: null,
        providerCalled: null,
        status: null
      };

      try {
        const validation =
          validateStockExplanationRequest(
            req.body
          );

        if (!validation.valid) {
          logEntry.status = 400;
          logger(logEntry);
          return res.status(400).json({
            ok: false,

            error: {
              code: 'INVALID_REQUEST',
              message:
                'Invalid stock explanation request.'
            },

            validationErrors:
              validation.errors,

            requestId:
              req.requestId || null
          });
        }

        const result =
          await stockExplanationService
            .generateStockExplanation(
              validation.value
            );

        logEntry.status = 200;
        logEntry.promptVersion =
          result?.metadata?.promptVersion ?? null;
        logEntry.providerCalled =
          result?.metadata?.providerCalled ?? null;
        logger(logEntry);

        return res.status(200).json({
          ok: true,
          ...result,
          requestId:
            req.requestId || null
        });
      } catch (error) {
        logEntry.status = 'error';
        logger(logEntry);
        return next(error);
      }
    }
  );

  return router;
}

const stockExplanationRouter =
  createStockExplanationRouter({
    stockExplanationService: {
      generateStockExplanation
    }
  });

module.exports = {
  createStockExplanationRouter,
  stockExplanationRouter
};
