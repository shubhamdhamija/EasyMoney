/**
 * Community service - tracks trending stocks and community activity
 */
const communityData = {};

module.exports = {
  getTrending(limit = 10) {
    const trending = [
      { symbol: 'AAPL', count: 150, trend: 'up' },
      { symbol: 'NVDA', count: 140, trend: 'up' },
      { symbol: 'TSLA', count: 130, trend: 'down' },
      { symbol: 'MSFT', count: 120, trend: 'up' },
      { symbol: 'AMZN', count: 110, trend: 'stable' }
    ];
    return trending.slice(0, limit);
  },

  recordWatchlistChange(userId, symbol, action) {
    const key = `${userId}:${symbol}`;
    if (!communityData[key]) {
      communityData[key] = { userId, symbol, action, timestamp: Date.now(), count: 0 };
    }
    communityData[key].count++;
  }
};

