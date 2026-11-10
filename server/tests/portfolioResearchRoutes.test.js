const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');
const {
  createPortfolioResearchProxyRouter,
  isAllowed
} = require('../routes/portfolioResearchRoutes');

function appWith(httpClient) {
  const app = express();
  app.use(express.json());
  app.use(createPortfolioResearchProxyRouter({ agentServiceBaseUrl: 'http://agent', httpClient }));
  app.use((req, res) => res.status(404).json({ fellThrough: true }));
  return app;
}

test('allow-list covers only the four read-only research routes', () => {
  assert.equal(isAllowed('POST', '/portfolio/local-context'), true);
  assert.equal(isAllowed('POST', '/portfolio/pc_abc/research-consent'), true);
  assert.equal(isAllowed('DELETE', '/portfolio/pc_abc/research-consent/pcn_1'), true);
  assert.equal(isAllowed('POST', '/agent/portfolio-research'), true);
  for (const [m, p] of [
    ['POST', '/portfolio/pc_abc/orders'],
    ['POST', '/portfolio/pc_abc/trade'],
    ['POST', '/portfolio/pc_abc/rebalance'],
    ['POST', '/portfolio/pc_abc/transfer'],
    ['GET', '/portfolio/pc_abc/research-consent'],
    ['POST', '/agent/portfolio-research/extra']
  ]) {
    assert.equal(isAllowed(m, p), false, `${m} ${p}`);
  }
});

test('unlisted and trading paths are never proxied', async () => {
  const calls = [];
  const app = appWith(async (c) => { calls.push(c); return { status: 200, data: {} }; });
  for (const path of ['/portfolio/pc_abc/orders', '/portfolio/pc_abc/rebalance']) {
    const res = await request(app).post(path).send({});
    assert.equal(res.status, 404);
  }
  assert.equal(calls.length, 0);
});

test('forwards only auth/content headers and returns the upstream status', async () => {
  let seen;
  const app = appWith(async (c) => { seen = c; return { status: 403, data: { error: { code: 'PORTFOLIO_CONSENT_REQUIRED' } } }; });
  const res = await request(app)
    .post('/agent/portfolio-research')
    .set('Authorization', 'Bearer token-value')
    .set('Cookie', 'session=1')
    .send({ portfolio_context: {} });
  assert.equal(res.status, 403);
  assert.equal(res.body.error.code, 'PORTFOLIO_CONSENT_REQUIRED');
  assert.equal(seen.url, 'http://agent/agent/portfolio-research');
  assert.equal(seen.headers.authorization, 'Bearer token-value');
  assert.equal(seen.headers.cookie, undefined);
});

test('transport failures return a safe 503 without upstream details', async () => {
  const app = appWith(async () => { throw new Error('connect ECONNREFUSED secret-detail'); });
  const res = await request(app).delete('/portfolio/pc_abc/research-consent/pcn_1');
  assert.equal(res.status, 503);
  assert.equal(JSON.stringify(res.body).includes('secret-detail'), false);
});
