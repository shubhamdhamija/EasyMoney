const { retrieveOptimized } = require('./optimizedRetriever');

/**
 * For each symbol, retrieve up to `perSymbol` top docs from vector store.
 * Returns array of { symbol, text, score, id }
 */
async function getWatchlistContext(symbols, perSymbol = 3) {
  const allDocs = [];
  for (const symbol of symbols) {
    try {
      const docs = await retrieveOptimized(symbol);
      allDocs.push(...docs.map(text => ({ symbol, text })));
    } catch (e) {
      console.warn('Retriever error for', symbol, e.message);
    }
  }
  return allDocs;
}

module.exports = { getWatchlistContext };
