const express = require('express');

const {
  validateStockInsightRequest
} = require('../validators/stockInsightValidator');

function defaultLogger(entry) {
  console.log(`POST /ai/stock-insight ${JSON.stringify(entry)}`);
}

function createStockInsightRouter({
  stockInsightService,
  logger = defaultLogger
}) {
  if (
    !stockInsightService ||
    typeof stockInsightService.generateStockInsight !== 'function'
  ) {
    throw new TypeError(
      'stockInsightService must provide generateStockInsight'
    );
  }

  const router = express.Router();

  router.post('/', async (req, res, next) => {
    // Safe fields only: never prompts, article text, headers or keys.
    const logEntry = {
      requestId: req.requestId || null,
      route: '/ai/stock-insight',
      symbol:
        typeof req.body?.symbol === 'string'
          ? req.body.symbol.slice(0, 12)
          : null,
      promptVersion: null,
      providerCalled: null,
      fallbackUsed: null,
      status: null
    };

    try {
      const validation = validateStockInsightRequest(req.body);

      if (!validation.valid) {
        logEntry.status = 400;
        logger(logEntry);

        return res.status(400).json({
          ok: false,
          error: {
            code: 'INVALID_REQUEST',
            message: 'Invalid stock insight request.'
          },
          validationErrors: validation.errors,
          requestId: req.requestId || null
        });
      }

      const result = await stockInsightService.generateStockInsight(
        validation.value
      );

      logEntry.status = 200;
      logEntry.promptVersion = result?.metadata?.promptVersion ?? null;
      logEntry.providerCalled = result?.metadata?.providerCalled ?? null;
      logEntry.fallbackUsed = result?.metadata?.fallbackUsed ?? null;
      logger(logEntry);

      // Internal metadata stays server-side (logged above), not in the client
      // contract.
      return res.status(200).json({
        ok: true,
        insight: result.insight,
        requestId: req.requestId || null
      });
    } catch (error) {
      logEntry.status = 'error';
      logger(logEntry);
      return next(error);
    }
  });

  return router;
}

module.exports = { createStockInsightRouter };

