'use strict';

/**
 * Thin, allow-listed proxy for read-only portfolio research. The agent-service
 * owns authentication, ownership checks, consent, the portfolio tools and audit.
 *
 * Forwarded (and nothing else):
 *   POST   /portfolio/local-context
 *   POST   /portfolio/:id/research-consent
 *   DELETE /portfolio/:id/research-consent/:consentId
 *   POST   /agent/portfolio-research
 *
 * No provider logic, credentials, or trading routes exist here. Non-matching
 * requests fall through (`next()`) to the rest of the app.
 */

const express = require('express');

const ID = '[A-Za-z0-9_-]{1,128}';

const ROUTES = [
  ['POST', new RegExp('^/portfolio/local-context$')],
  ['POST', new RegExp(`^/portfolio/${ID}/research-consent$`)],
  ['DELETE', new RegExp(`^/portfolio/${ID}/research-consent/${ID}$`)],
  ['POST', new RegExp('^/agent/portfolio-research$')]
];

function isAllowed(method, path) {
  return ROUTES.some(([m, re]) => m === method && re.test(path));
}

function createPortfolioResearchProxyRouter({ agentServiceBaseUrl, httpClient, timeoutMs = 90000 }) {
  const router = express.Router();

  router.use(async (req, res, next) => {
    if (!isAllowed(req.method, req.path)) return next();

    const headers = {};
    if (req.headers.authorization) headers.authorization = req.headers.authorization;
    if (req.headers['content-type']) headers['content-type'] = req.headers['content-type'];
    if (req.requestId) headers['x-request-id'] = req.requestId;

    try {
      const response = await httpClient({
        method: req.method,
        url: `${agentServiceBaseUrl}${req.path}`,
        data: req.method === 'POST' ? req.body : undefined,
        headers,
        timeout: timeoutMs,
        validateStatus: () => true
      });
      return res.status(response.status).json(response.data);
    } catch (e) {
      // Never forward upstream error bodies or details on transport failure.
      return res.status(503).json({
        error: {
          code: 'PROVIDER_UNAVAILABLE',
          message: 'Portfolio research is temporarily unavailable.'
        },
        request_id: req.requestId || null
      });
    }
  });

  return router;
}

module.exports = { createPortfolioResearchProxyRouter, isAllowed };
