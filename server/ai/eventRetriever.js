const { retrieveOptimized } = require('../optimizedRetriever');

async function getEventContext(symbol) {
  const docs = await retrieveOptimized(symbol);
  return docs.join('\n');
}

module.exports = { getEventContext };

