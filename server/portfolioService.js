const { getWatchlistContext } = require('./watchlistRetriever');
const { buildWatchlistPrompt } = require('./watchlistPromptBuilder');
const watchlistService = require('./watchlistService');
const watchlistRetriever = require('./watchlistRetriever');
const userProfileService = require('./userProfileService');
const { getStockPrice } = require('./systemOrchestrator');
const axios = require('axios');

// Resolves after `ms` milliseconds with an empty array — used to race against slow RAG calls
const withTimeout = (promise, ms) =>
  Promise.race([promise, new Promise(resolve => setTimeout(() => resolve([]), ms))]);

/**
 * Portfolio generator.
 * - userId: for personalized risk profile
 * - risk: 'low' | 'medium' | 'high' (falls back to user profile if not passed)
 * - returns an object with named portfolios and rationale
 */
async function generatePortfolio({ risk = 'medium', symbols = [], userId = 'default' } = {}) {
  // Load user profile — override risk if profile has a preference
  try {
    const profile = userProfileService.getProfile(userId);
    if (profile && profile.risk && risk === 'medium') risk = profile.risk;
  } catch (e) {
    console.warn('portfolioService: could not load user profile:', e.message);
  }

  // If symbols not provided, use sample watchlist
  if (!symbols || symbols.length === 0) {
    symbols = await (watchlistService.getUserWatchlist ? watchlistService.getUserWatchlist(userId) : ['AAPL','NVDA','TSLA']);
  }

  // Limit symbols to 30 for cost
  symbols = symbols.slice(0, 30);

  // Fetch small RAG context for each symbol — capped at 2s so we never hang
  const contexts = await withTimeout(watchlistRetriever.getWatchlistContext(symbols, 2), 2000);

  // Heuristic scoring: prefer strong positive sentiment (rudimentary)
  // Assign base scores from context presence + simple keyword checks
  const scores = {};
  for (const s of symbols) scores[s] = 0;
  for (const doc of contexts) {
    const sym = doc.symbol;
    const text = (doc.text || '').toLowerCase();
    if (text.includes('surge') || text.includes('strong') || text.includes('beat') || text.includes('upgrade')) scores[sym] += 3;
    if (text.includes('gain') || text.includes('rise') || text.includes('positive')) scores[sym] += 2;
    if (text.includes('downgrade') || text.includes('miss') || text.includes('concern') || text.includes('drop')) scores[sym] -= 3;
    // recency weight
    scores[sym] += (doc.score || 0) * 1.0;
  }

  // Layer in live price movement signals (non-blocking)
  if (process.env.FINNHUB_API_KEY) {
    const priceResults = await withTimeout(
      Promise.allSettled(symbols.map(async sym => {
        const q = await getStockPrice(sym);
        return q ? { sym, changePct: q.changePct } : null;
      })),
      4000
    );
    for (const r of (Array.isArray(priceResults) ? priceResults : [])) {
      if (r?.status === 'fulfilled' && r.value) {
        // Positive momentum adds up to +2, negative deducts up to -2
        scores[r.value.sym] += Math.max(-2, Math.min(2, r.value.changePct * 0.2));
      }
    }
  }

  // Build candidate lists
  const sorted = Object.keys(scores).sort((a,b) => (scores[b]||0) - (scores[a]||0));

  const take = (n) => sorted.slice(0,n);

  const portfolios = {
    aggressive: take(10),
    balanced: take(6),
    conservative: take(4)
  };

  // Adjust based on requested risk
  let chosen;
  if (risk === 'low') chosen = portfolios.conservative;
  else if (risk === 'high') chosen = portfolios.aggressive;
  else chosen = portfolios.balanced;

  const rationale = `Generated using RAG signals + live price momentum across ${symbols.length} symbols. Risk=${risk}. Top picks: ${chosen.slice(0,5).join(', ')}.`;

  return { risk, portfolio: chosen, rationale, userId };
}

module.exports = { generatePortfolio };

