/**
 * System orchestrator - builds full context for a symbol combining all services
 */
const watchlistService = require('./watchlistService');
const insightService = require('./insightService');
const signalEngine = require('./signalEngine');
const portfolioService = require('./portfolioService');

module.exports = {
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

