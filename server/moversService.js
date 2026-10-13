/**
 * Market movers service - gainers/losers and trending stocks.
 *
 * Keeps an in-memory cache so the backend can boot and serve stable responses
 * even when no external movers provider is configured.
 */
let cache = {
  gainers: [],
  losers: [],
  mostActive: [],
  updatedAt: null
};

function normalizeLimit(limit = 10) {
  const parsed = Number.parseInt(limit, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 10;
}

function take(list, limit) {
  return list.slice(0, normalizeLimit(limit));
}

async function updateMovers() {
  cache = {
    ...cache,
    updatedAt: new Date().toISOString()
  };

  return getMovers();
}

function getCacheAge() {
  return cache.updatedAt;
}

function getGainers(limit = 10) {
  return take(cache.gainers, limit);
}

function getLosers(limit = 10) {
  return take(cache.losers, limit);
}

function getMostActive(limit = 10) {
  return take(cache.mostActive, limit);
}

function getMovers(limit = 10) {
  return {
    ok: true,
    gainers: getGainers(limit),
    losers: getLosers(limit),
    mostActive: getMostActive(limit),
    updatedAt: getCacheAge()
  };
}

module.exports = {
  getMovers,
  getGainers,
  getLosers,
  getMostActive,
  getCacheAge,
  updateMovers
};
