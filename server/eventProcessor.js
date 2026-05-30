const cron = require('node-cron');
const insightService = require('./insightService');

// Example events: "earnings", "analyst_update", "price_spike", "breaking_news"

async function handleEvent(type, payload) {
  console.log('Event received', type, payload);
  // Basic routing — fetch context, run RAG, generate insight, notify
  switch (type) {
    case 'earnings':
    case 'analyst_update':
    case 'breaking_news':
    case 'price_spike':
      return await processMarketEvent(type, payload);
    default:
      throw new Error('Unknown event type: ' + type);
  }
}

async function processMarketEvent(type, payload) {
  // payload should include { symbol }
  const symbol = payload.symbol;
  // 1. gather context (news, docs, past insights)
  const context = await insightService.fetchContextForSymbol(symbol);
  // 2. run RAG / generate insight
  const insight = await insightService.generateInsight(symbol, { eventType: type, payload, context });
  // 3. TODO: send notification (push / email)
  console.log('Generated insight for', symbol, insight.shortSummary || insight);
  return insight;
}

// Example cron job to run every 5 minutes and check custom conditions
cron.schedule('*/5 * * * *', async () => {
  console.log('Cron: checking market events...');
  try {
    // TODO: implement polling logic (price movers, news feed, analyst API)
  } catch (e) {
    console.error('Cron error', e);
  }
});

module.exports = { handleEvent };

