const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');
const {
  createPortfolioConnectionProxyRouter,
  isAllowed
} = require('../routes/portfolioConnectionRoutes');

function appWith(httpClient) {
  const app = express();
  app.use(express.json());
  app.use(
    '/portfolio/connections',
    createPortfolioConnectionProxyRouter({ agentServiceBaseUrl: 'http://agent', httpClient })
  );
  return app;
}

test('trading, order and transfer paths are never proxied', async () => {
  const calls = [];
  const app = appWith(async (c) => { calls.push(c); return { status: 200, data: {} }; });
  for (const [method, path] of [
    ['post', '/portfolio/connections/abc/orders'],
    ['post', '/portfolio/connections/abc/trade'],
    ['post', '/portfolio/connections/abc/transfer'],
    ['post', '/portfolio/connections/abc/deposit'],
    ['get', '/portfolio/connections/abc/sync']
  ]) {
    const res = await request(app)[method](path);
    assert.equal(res.status, 404, path);
  }
  assert.equal(calls.length, 0);
  assert.equal(isAllowed('POST', '/abc/sync'), true);
});

test('forwards only auth headers and returns upstream status', async () => {
  let seen;
  const app = appWith(async (c) => { seen = c; return { status: 503, data: { error: { code: 'SNAPTRADE_DISABLED' } } }; });
  const res = await request(app)
    .post('/portfolio/connections/snaptrade/session')
    .set('Authorization', 'Bearer t')
    .set('Cookie', 'secret=1')
    .send({ redirectUri: 'easymoney://portfolio/snaptrade' });
  assert.equal(res.status, 503);
  assert.equal(seen.url, 'http://agent/portfolio/connections/snaptrade/session');
  assert.equal(seen.headers.authorization, 'Bearer t');
  assert.equal(seen.headers.cookie, undefined);
});

test('transport failure returns safe error', async () => {
  const app = appWith(async () => { throw new Error('secret stack'); });
  const res = await request(app).get('/portfolio/connections');
  assert.equal(res.status, 503);
  assert.equal(res.body.error.code, 'PROVIDER_UNAVAILABLE');
  assert.ok(!JSON.stringify(res.body).includes('secret stack'));
});
