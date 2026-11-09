const express = require('express');

const {
  validateStockPicksRequest
} = require('../validators/stockPicksValidator');

function defaultLogger(entry) {
  console.log(`POST /ai/picks ${JSON.stringify(entry)}`);
}

function createStockPicksRouter({ stockPicksService, logger = defaultLogger }) {
  if (
    !stockPicksService ||
    typeof stockPicksService.generateStockPicks !== 'function'
  ) {
    throw new TypeError('stockPicksService must provide generateStockPicks');
  }

  const router = express.Router();

  router.post('/', async (req, res, next) => {
    // Safe fields only: never prompts, company names, headers or keys.
    const logEntry = {
      requestId: req.requestId || null,
      route: '/ai/picks',
      stockCount: Array.isArray(req.body?.stocks) ? req.body.stocks.length : null,
      promptVersion: null,
      providerCalled: null,
      fallbackUsed: null,
      status: null
    };

    try {
      const validation = validateStockPicksRequest(req.body);

      if (!validation.valid) {
        logEntry.status = 400;
        logger(logEntry);

        return res.status(400).json({
          ok: false,
          error: {
            code: 'INVALID_REQUEST',
            message: 'Invalid stock picks request.'
          },
          validationErrors: validation.errors,
          requestId: req.requestId || null
        });
      }

      const result = await stockPicksService.generateStockPicks(validation.value);

      logEntry.status = 200;
      logEntry.promptVersion = result?.metadata?.promptVersion ?? null;
      logEntry.providerCalled = result?.metadata?.providerCalled ?? null;
      logEntry.fallbackUsed = result?.metadata?.fallbackUsed ?? null;
      logger(logEntry);

      return res.status(200).json({
        ok: true,
        picks: result.picks,
        disclaimer: result.disclaimer,
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

module.exports = { createStockPicksRouter };
