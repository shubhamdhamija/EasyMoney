const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');

function computeMomentum(prices) {
  if (!prices || prices.length < 2) return 0;
  const recent = prices.slice(-10);
  const first = recent[0];
  const last = recent[recent.length - 1];
  return ((last - first) / (first || 1)) * 100;
}

async function computeRagSentiment(symbol) {
  try {
    const queryEmbedding = await embeddingService.createEmbedding(`${symbol} stock news analyst sentiment`);
    const matches = await vectorStore.querySimilar(queryEmbedding, 5, { symbol: { '$eq': symbol } });
    let boost = 0;
    const reasons = [];
    for (const m of matches) {
      const text = (m.metadata?.text || '').toLowerCase();
      const sent = (m.metadata?.sentiment || '').toLowerCase();
      if (sent === 'positive' || /upgrade|beat|surge|strong|bullish/.test(text)) {
        boost += 1;
        reasons.push('positive news');
      } else if (sent === 'negative' || /downgrade|miss|drop|weak|bearish/.test(text)) {
        boost -= 1;
        reasons.push('negative news');
      }
    }
    const unique = [...new Set(reasons)];
    const label = boost > 0 ? 'positive' : boost < 0 ? 'negative' : 'neutral';
    const summary = matches.length > 0
      ? ` News sentiment: ${label} (${unique.join(', ') || 'mixed signals'}, ${matches.length} docs).`
      : '';
    return { boost, summary };
  } catch {
    return { boost: 0, summary: '' };
  }
}

async function analyzeSymbol(symbol, marketData = {}) {
  const momentum = computeMomentum(marketData.prices || []);
  const { boost, summary } = await computeRagSentiment(symbol);

  const adjusted = momentum + boost * 1.5;
  const confidence = Math.min(0.95, Math.abs(adjusted) / 10);
  const signal = adjusted > 2 ? 'bullish' : (adjusted < -2 ? 'bearish' : 'neutral');
  const reason = `Momentum ${momentum.toFixed(2)}% over window.${summary}`;

  return { symbol, signal, confidence: +confidence.toFixed(2), reason };
}

module.exports = { analyzeSymbol };

