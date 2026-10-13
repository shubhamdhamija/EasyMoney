/**
 * System orchestrator - builds full context for a symbol combining all services
 */
const axios = require('axios');
const watchlistService = require('./watchlistService');
const insightService = require('./insightService');
const signalEngine = require('./signalEngine');
const portfolioService = require('./portfolioService');

async function getStockPrice(symbol) {
  const apiKey = process.env.FINNHUB_API_KEY;
  if (!apiKey || !symbol) return null;

  try {
    const normalizedSymbol = String(symbol).trim().toUpperCase();
    const response = await axios.get('https://finnhub.io/api/v1/quote', {
      params: { symbol: normalizedSymbol, token: apiKey },
      timeout: 5000
    });

    const data = response.data || {};
    const current = Number(data.c);
    const previousClose = Number(data.pc);
    const change = Number(data.d);
    const changePct = Number(data.dp);

    if (!Number.isFinite(current)) return null;

    return {
      symbol: normalizedSymbol,
      current,
      previousClose: Number.isFinite(previousClose) ? previousClose : null,
      change: Number.isFinite(change) ? change : null,
      changePct: Number.isFinite(changePct) ? changePct : 0
    };
  } catch (e) {
    console.warn(`systemOrchestrator: quote lookup failed for ${symbol}:`, e.message);
    return null;
  }
}

module.exports = {
  getStockPrice,
  async buildFullContext(symbol, userId = 'default') {
    try {
      const context = {
        symbol,
        userId,
        insight: null,
        signals: null,
        portfolio: null,
        recommendation: null
      };

      // Optionally fetch multiple services in parallel
      return context;
    } catch (e) {
      console.error('Error building context:', e);
      return { symbol, userId, error: e.message };
    }
  }
};

