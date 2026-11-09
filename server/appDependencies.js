const {
  createConfiguredLlmClient
} = require(
  './services/llm/configuredLlmClient'
);

const {
  createStockExplanationService
} = require(
  './services/stockExplanationService'
);

const {
  createStockInsightService
} = require('./services/stockInsightService');
const {
  createStockPicksService
} = require('./services/stockPicksService');

function createAppDependencies({
  env = process.env,
  fetchImpl = globalThis.fetch
} = {}) {
  const llmClient =
    createConfiguredLlmClient({
      env,
      fetchImpl
    });

  const stockExplanationService =
    createStockExplanationService({
      llmClient
    });

  return {
    llmClient,
    stockExplanationService,
    stockInsightService: createStockInsightService({ llmClient }),
    stockPicksService: createStockPicksService({ llmClient })
  };
}

module.exports = {
  createAppDependencies
};

