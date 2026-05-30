const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');

/**
 * Simple rule-based signal engine.
 * Accepts market data and computes signals.
 */

function computeMomentum(prices) {
  if (!prices || prices.length < 2) return 0;
  const recent = prices.slice(-10);
  const first = recent[0];
  const last = recent[recent.length-1];
  return ((last - first) / (first || 1)) * 100; // percent over window
}

async function analyzeSymbol(symbol, marketData = {}) {
  // marketData: { prices: [..], volume: [..] }
  const momentum = computeMomentum(marketData.prices || []);
  const volChange = 0; // placeholder
  // rudimentary signal rules
  const confidence = Math.min(0.95, Math.abs(momentum) / 10);
  const signal = momentum > 2 ? 'bullish' : (momentum < -2 ? 'bearish' : 'neutral');
  const reason = `Momentum ${momentum.toFixed(2)}% over window.`;
  return { symbol, signal, confidence: +confidence.toFixed(2), reason };
}

module.exports = { analyzeSymbol };

