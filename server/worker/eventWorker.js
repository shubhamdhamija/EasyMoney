const cron = require('node-cron');
const { detectPriceEvent } = require('../ai/eventDetector');
const { handleEvent } = require('../ai/eventProcessor');
const { getUsersWatching } = require('../usersService');

// Replace with real price API in production
global.priceHistory = {
  AAPL: [180, 182, 185, 190, 195, 200],
  TSLA: [700, 690, 680, 670, 660, 650],
  NVDA: [400, 410, 420, 430, 440, 450]
};

async function getRecentPrices(symbol) {
  // Replace with real API call
  return global.priceHistory[symbol] || [];
}

async function checkMarket() {
  const symbols = Object.keys(global.priceHistory); // Replace with dynamic list
  for (const symbol of symbols) {
    const prices = await getRecentPrices(symbol);
    const event = detectPriceEvent(symbol, prices);
    if (event) {
      const users = await getUsersWatching(symbol);
      await handleEvent(event, users);
    }
  }
}

// Run every 5 minutes
cron.schedule('*/5 * * * *', checkMarket);

// Allow manual run
if (require.main === module) {
  checkMarket().then(() => console.log('Manual event check complete')).catch(e => console.error(e));
}

