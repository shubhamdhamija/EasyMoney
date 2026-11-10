'use strict';

/**
 * Thin, allow-listed proxy to the agent-service, which OWNS the SnapTrade
 * Sandbox integration (JWT, tenant, audit and provider secrets live there).
 *
 * This file contains no provider logic, no credentials, and no trading routes.
 * Only the read-only connection lifecycle paths below are forwarded; anything
 * else is a 404 and is never proxied.
 */

const express = require('express');

const ID = '[A-Za-z0-9_-]{1,128}';

const ALLOWED = [
  ['POST', new RegExp('^/snaptrade/session$')],
  ['POST', new RegExp(`^/snaptrade/${ID}/complete$`)],
  ['GET', new RegExp('^/?$')],
  ['GET', new RegExp(`^/${ID}/portfolio$`)],
  ['POST', new RegExp(`^/${ID}/sync$`)],
  ['POST', new RegExp(`^/${ID}/reconnect-session$`)],
  ['DELETE', new RegExp(`^/${ID}$`)],
  ['DELETE', new RegExp(`^/${ID}/data$`)]
];

function isAllowed(method, path) {
  return ALLOWED.some(([m, re]) => m === method && re.test(path));
}

function createPortfolioConnectionProxyRouter({ agentServiceBaseUrl, httpClient, timeoutMs = 60000 }) {
  const router = express.Router();

  router.use(async (req, res) => {
    if (!isAllowed(req.method, req.path)) {
      return res.status(404).json({
        error: { code: 'NOT_FOUND', message: 'Not found.' },
        request_id: req.requestId || null
      });
    }

    const headers = {};
    if (req.headers.authorization) headers.authorization = req.headers.authorization;
    if (req.headers['content-type']) headers['content-type'] = req.headers['content-type'];
    if (req.requestId) headers['x-request-id'] = req.requestId;

    const query = req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '';
    try {
      const response = await httpClient({
        method: req.method,
        url: `${agentServiceBaseUrl}/portfolio/connections${req.path === '/' ? '' : req.path}${query}`,
        data: ['POST', 'PUT', 'PATCH'].includes(req.method) ? req.body : undefined,
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
          message: 'Portfolio synchronization is temporarily unavailable.'
        },
        request_id: req.requestId || null
      });
    }
  });

  return router;
}

module.exports = { createPortfolioConnectionProxyRouter, isAllowed };
