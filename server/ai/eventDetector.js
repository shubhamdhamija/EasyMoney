const { EVENT_TYPES } = require('./eventTypes');

function detectPriceEvent(symbol, prices) {
  if (!prices || prices.length < 2) return null;
  const latest = prices[prices.length - 1];
  const previous = prices[0];
  const changePercent = ((latest - previous) / previous) * 100;
  if (changePercent >= 5) {
    return {
      type: EVENT_TYPES.PRICE_SPIKE,
      symbol,
      changePercent
    };
  }
  if (changePercent <= -5) {
    return {
      type: EVENT_TYPES.PRICE_DROP,
      symbol,
      changePercent
    };
  }
  return null;
}

// Add more detectors (analyst, earnings, news) as needed

module.exports = { detectPriceEvent };

