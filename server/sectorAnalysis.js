const vectorStore = require('./vectorStore');
const embeddingService = require('./embeddingService');
const { getStockPrice } = require('./systemOrchestrator');

// Stock-to-sector mapping — use 3 representative stocks per sector to stay within
// Finnhub free-tier rate limit (60 req/min) when analyzing all sectors at once
const SECTOR_STOCKS = {
  'AI & Tech':     ['NVDA', 'MSFT', 'GOOGL'],
  'Consumer Tech': ['AAPL', 'AMZN', 'TSLA'],
  'Finance':       ['JPM', 'BAC', 'V'],
  'Energy':        ['XOM', 'CVX', 'COP'],
  'Healthcare':    ['JNJ', 'PFE', 'LLY'],
  'Industrials':   ['BA', 'CAT', 'RTX'],
  'Consumer':      ['WMT', 'COST', 'NKE']
};

// Keywords to boost/penalise sector sentiment score from RAG docs
const POSITIVE_KEYWORDS = ['upgrade', 'beat', 'surge', 'strong', 'bullish', 'outperform', 'record', 'gain'];
const NEGATIVE_KEYWORDS = ['downgrade', 'miss', 'drop', 'weak', 'bearish', 'underperform', 'loss', 'concern'];

/**
 * Score a sector using:
 *   1. RAG sentiment from vector store docs for its stocks (weight: 0.5)
 *   2. Average % price change for its stocks today (weight: 0.5, capped)
 */
async function scoreSector(sector, stocks) {
  let ragScore = 0;
  let ragDocCount = 0;

  // RAG sentiment per stock (parallel)
  const ragResults = await Promise.allSettled(
    stocks.map(async symbol => {
      try {
        const q = `${symbol} stock news sentiment`;
        const embedding = await embeddingService.createEmbedding(q);
        const matches = await vectorStore.querySimilar(embedding, 3, { symbol: { '$eq': symbol } });
        let s = 0;
        for (const m of matches) {
          const text = (m.metadata?.text || '').toLowerCase();
          const sentMeta = (m.metadata?.sentiment || '').toLowerCase();
          if (sentMeta === 'positive' || POSITIVE_KEYWORDS.some(k => text.includes(k))) s += 1;
          if (sentMeta === 'negative' || NEGATIVE_KEYWORDS.some(k => text.includes(k))) s -= 1;
        }
        return { count: matches.length, score: s };
      } catch { return { count: 0, score: 0 }; }
    })
  );

  for (const r of ragResults) {
    if (r.status === 'fulfilled') {
      ragScore += r.value.score;
      ragDocCount += r.value.count;
    }
  }

  // Live price movement per stock (parallel, only if Finnhub key available)
  let priceScore = 0;
  if (process.env.FINNHUB_API_KEY) {
    const priceResults = await Promise.allSettled(
      stocks.map(sym => getStockPrice(sym))
    );
    let priceCount = 0;
    for (const r of priceResults) {
      if (r.status === 'fulfilled' && r.value) {
        priceScore += r.value.changePct || 0;
        priceCount++;
      }
    }
    if (priceCount > 0) priceScore = priceScore / priceCount; // average %
  }

  // Normalize: ragScore per doc, then combine
  const normalizedRag = ragDocCount > 0 ? ragScore / ragDocCount : 0;
  const combined = normalizedRag * 0.5 + Math.max(-5, Math.min(5, priceScore)) * 0.1;

  return {
    sector,
    score: +combined.toFixed(3),
    ragDocs: ragDocCount,
    avgPriceChange: priceScore !== 0 ? +priceScore.toFixed(2) : null,
    stocks: stocks.slice(0, 5)
  };
}

/**
 * Analyze all sectors and return ranked results.
 * Runs sequentially with a small delay to respect Finnhub free-tier rate limits.
 * @param {string[]} [sectorList] - optional override of sectors to analyze
 */
async function analyzeSectors(sectorList) {
  const sectors = sectorList
    ? sectorList.reduce((acc, s) => { acc[s] = SECTOR_STOCKS[s] || [s]; return acc; }, {})
    : SECTOR_STOCKS;

  const results = [];
  for (const [sector, stocks] of Object.entries(sectors)) {
    try {
      const scored = await scoreSector(sector, stocks);
      results.push(scored);
    } catch (e) {
      console.warn(`sectorAnalysis: scoreSector failed for ${sector}:`, e.message);
    }
    // 300ms gap between sectors keeps us under 60 req/min on Finnhub free tier
    await new Promise(r => setTimeout(r, 300));
  }
  return results.sort((a, b) => b.score - a.score);
}

module.exports = { analyzeSectors, SECTOR_STOCKS };
