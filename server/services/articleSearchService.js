const ARTICLE_LIMIT = 30;

class ArticleSearchError extends Error {
  constructor(code, message, statusCode = 502) {
    super(message);
    this.name = 'ArticleSearchError';
    this.code = code;
    this.statusCode = statusCode;
  }
}

function normalizeQuery(query) {
  if (typeof query !== 'string') return null;
  const normalized = query.trim().replace(/\s+/gu, ' ');
  return normalized.length >= 2 && normalized.length <= 200
    ? normalized
    : null;
}

function isWebUrl(value) {
  if (typeof value !== 'string') return false;
  try {
    const url = new URL(value);
    return ['https:', 'http:'].includes(url.protocol) &&
      !!url.hostname && !url.username && !url.password;
  } catch {
    return false;
  }
}

function normalizeArticles(data) {
  if (!data || !Array.isArray(data.news)) {
    throw new ArticleSearchError(
      'INVALID_ARTICLE_RESPONSE',
      'The news provider returned an invalid response.'
    );
  }

  const articles = data.news.slice(0, ARTICLE_LIMIT).map(item => {
    if (!item || typeof item.uuid !== 'string' || !item.uuid.trim() ||
        typeof item.title !== 'string' || !item.title.trim() ||
        typeof item.publisher !== 'string' || !item.publisher.trim() ||
        !isWebUrl(item.link) ||
        !Number.isSafeInteger(item.providerPublishTime) ||
        item.providerPublishTime < 0 || item.providerPublishTime > 253402300799) {
      throw new ArticleSearchError(
        'INVALID_ARTICLE_RESPONSE',
        'The news provider returned an invalid article.'
      );
    }
    return {
      id: item.uuid.trim(),
      headline: item.title.trim(),
      source: item.publisher.trim(),
      url: item.link,
      datetime: item.providerPublishTime
    };
  });

  const ids = new Set();
  const urls = new Set();
  return articles.filter(article => {
    if (ids.has(article.id) || urls.has(article.url)) return false;
    ids.add(article.id);
    urls.add(article.url);
    return true;
  });
}

function createArticleSearchService({ fetchImpl = globalThis.fetch } = {}) {
  return {
    async searchArticles(query) {
      const normalized = normalizeQuery(query);
      if (!normalized) {
        throw new ArticleSearchError(
          'INVALID_ARTICLE_QUERY',
          'Enter a search query between 2 and 200 characters.',
          400
        );
      }

      const url = new URL('https://query1.finance.yahoo.com/v1/finance/search');
      url.search = new URLSearchParams({
        q: normalized,
        quotesCount: '0',
        newsCount: String(ARTICLE_LIMIT),
        enableFuzzyQuery: 'false'
      }).toString();

      let data;
      try {
        const response = await fetchImpl(url, {
          headers: { 'User-Agent': 'Mozilla/5.0', Accept: 'application/json' },
          signal: AbortSignal.timeout(10000)
        });
        if (!response.ok) {
          throw new ArticleSearchError(
            'ARTICLE_PROVIDER_UNAVAILABLE',
            'Article search is temporarily unavailable. Please try again.'
          );
        }
        data = await response.json();
      } catch (error) {
        if (error instanceof ArticleSearchError) throw error;
        throw new ArticleSearchError(
          'ARTICLE_PROVIDER_UNAVAILABLE',
          'Article search is temporarily unavailable. Please try again.'
        );
      }

      return { articles: normalizeArticles(data), limit: ARTICLE_LIMIT };
    }
  };
}

module.exports = {
  ArticleSearchError,
  createArticleSearchService
};
