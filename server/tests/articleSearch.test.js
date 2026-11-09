const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const request = require('supertest');
const { createArticleSearchService } = require('../services/articleSearchService');
const { createArticleSearchRouter } = require('../routes/articleSearchRoutes');

function article(overrides = {}) {
  return {
    uuid: 'article-apple',
    title: 'Apple earnings',
    publisher: 'Test publisher',
    link: 'https://example.com/apple',
    providerPublishTime: 1700000000,
    relatedTickers: ['AAPL'],
    ...overrides
  };
}

function appWith(fetchImpl) {
  const app = express();
  app.use('/articles', createArticleSearchRouter({
    articleSearchService: createArticleSearchService({ fetchImpl })
  }));
  return app;
}

test('searches globally, normalizes and encodes queries, preserves provider order and removes duplicates', async () => {
  let requestedUrl;
  const apple = article();
  const microsoft = article({
    uuid: 'article-microsoft',
    title: 'Microsoft earnings',
    link: 'https://example.com/microsoft',
    relatedTickers: ['MSFT']
  });
  const app = appWith(async (url, options) => {
    requestedUrl = url;
    assert.ok(options.signal instanceof AbortSignal);
    return { ok: true, json: async () => ({ news: [apple, microsoft, apple] }) };
  });
  const response = await request(app)
    .get('/articles/search')
    .query({ q: '  earnings   & revenue  ' })
    .expect(200);

  assert.equal(requestedUrl.origin, 'https://query1.finance.yahoo.com');
  assert.equal(requestedUrl.searchParams.get('q'), 'earnings & revenue');
  assert.equal(requestedUrl.searchParams.get('quotesCount'), '0');
  assert.equal(requestedUrl.searchParams.get('newsCount'), '30');
  assert.equal(requestedUrl.searchParams.has('symbol'), false);
  assert.deepEqual(response.body.articles.map(item => item.id), ['article-apple', 'article-microsoft']);
  assert.equal(response.body.articles[0].headline, 'Apple earnings');
  assert.equal(response.body.limit, 30);
});

test('rejects missing, short, long, repeated and structured queries before provider access', async () => {
  let calls = 0;
  const app = appWith(async () => { calls++; });
  for (const query of ['', '?q=a', `?q=${'a'.repeat(201)}`, '?q=a&q=b', '?q[term]=earnings']) {
    const response = await request(app).get(`/articles/search${query}`).expect(400);
    assert.equal(response.body.error.code, 'INVALID_ARTICLE_QUERY');
  }
  assert.equal(calls, 0);
});

test('distinguishes genuinely empty results from provider failures', async () => {
  const emptyApp = appWith(async () => ({ ok: true, json: async () => ({ news: [] }) }));
  const empty = await request(emptyApp).get('/articles/search?q=earnings').expect(200);
  assert.deepEqual(empty.body.articles, []);

  for (const fetchImpl of [
    async () => ({ ok: false }),
    async () => { throw new Error('private provider diagnostics'); },
    async () => ({ ok: true, json: async () => { throw new SyntaxError('invalid JSON'); } })
  ]) {
    const response = await request(appWith(fetchImpl))
      .get('/articles/search?q=earnings').expect(502);
    assert.equal(response.body.error.code, 'ARTICLE_PROVIDER_UNAVAILABLE');
    assert.doesNotMatch(JSON.stringify(response.body), /private provider diagnostics/);
  }
});

test('rejects malformed provider data and unsafe links instead of returning success', async () => {
  for (const data of [
    {},
    { news: null },
    { news: [null] },
    { news: [article({ title: '' })] },
    { news: [article({ uuid: null })] },
    { news: [article({ publisher: 5 })] },
    { news: [article({ link: 'javascript:alert(1)' })] },
    { news: [article({ link: 'https://user:password@example.com/article' })] },
    { news: [article({ providerPublishTime: '1700000000' })] },
    { news: [article({ providerPublishTime: -1 })] }
  ]) {
    const response = await request(appWith(async () => ({ ok: true, json: async () => data })))
      .get('/articles/search?q=earnings').expect(502);
    assert.equal(response.body.error.code, 'INVALID_ARTICLE_RESPONSE');
  }
});

test('bounds the provider result set', async () => {
  const news = Array.from({ length: 40 }, (_, index) => article({
    uuid: String(index),
    link: `https://example.com/${index}`
  }));
  const response = await request(appWith(async () => ({ ok: true, json: async () => ({ news }) })))
    .get('/articles/search?q=earnings').expect(200);
  assert.equal(response.body.articles.length, 30);
});
