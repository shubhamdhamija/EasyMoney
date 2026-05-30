const { getWatchlistContext } = require('./watchlistRetriever');
const { buildWatchlistPrompt } = require('./watchlistPromptBuilder');
const watchlistService = require('./watchlistService');
const watchlistRetriever = require('./watchlistRetriever');
const axios = require('axios');

/**
 * Portfolio generator.
 * - risk: 'low' | 'medium' | 'high'
 * - returns an object with named portfolios and rationale
 */
async function generatePortfolio({ risk = 'medium', symbols = [] } = {}) {
  // If symbols not provided, use sample watchlist
  if (!symbols || symbols.length === 0) {
    symbols = await (watchlistService.getUserWatchlist ? watchlistService.getUserWatchlist('default') : ['AAPL','NVDA','TSLA']);
  }

  // Limit symbols to 30 for cost
  symbols = symbols.slice(0, 30);

  // Fetch small RAG context for each symbol (cheap)
  const contexts = await watchlistRetriever.getWatchlistContext(symbols, 2);

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

  const rationale = `Generated using simple RAG signals across ${symbols.length} symbols. Risk=${risk}. Top picks: ${chosen.slice(0,5).join(', ')}.`;

  return { risk, portfolio: chosen, rationale };
}

module.exports = { generatePortfolio };

