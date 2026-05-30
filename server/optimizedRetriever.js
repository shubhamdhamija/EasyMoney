const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');

// Advanced retriever: multi-query, dedup, re-rank, recency boost, context compression
async function retrieveOptimized(symbol) {
  const queries = [
    `latest news about ${symbol}`,
    `${symbol} analyst rating changes`,
    `${symbol} earnings report impact`
  ];
  const types = ['news', 'analyst', 'earnings'];
  // Run all queries in parallel for each type
  const results = await Promise.all(queries.map((q, i) => (
    embeddingService.createEmbedding(q).then(embedding =>
      vectorStore.querySimilar(embedding, 3, { symbol: { "$eq": symbol }, type: { "$in": [types[i]] } }, types[i])
    )
  )));
  let allResults = results.flat();
  // Deduplicate by id
  const unique = Object.values(
    allResults.reduce((acc, item) => {
      acc[item.id] = item;
      return acc;
    }, {})
  );
  // Re-rank: similarity, recency, importance
  const now = Date.now();
  unique.sort((a, b) => {
    const recencyA = now - (a.metadata?.timestamp || 0);
    const recencyB = now - (b.metadata?.timestamp || 0);
    const importanceA = a.metadata?.type === 'analyst' ? 2 : a.metadata?.type === 'earnings' ? 1.5 : 1;
    const importanceB = b.metadata?.type === 'analyst' ? 2 : b.metadata?.type === 'earnings' ? 1.5 : 1;
    const scoreA = (a.score || 0) * 0.6 - recencyA * 0.000001 + importanceA * 0.1;
    const scoreB = (b.score || 0) * 0.6 - recencyB * 0.000001 + importanceB * 0.1;
    return scoreB - scoreA;
  });
  // Context compression: take top 5, use summary if present, else first 200 chars
  return unique.slice(0, 5).map(m => m.metadata?.summary || (m.metadata?.text || '').substring(0, 200));
}

module.exports = { retrieveOptimized };

