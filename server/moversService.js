/**
 * Market movers service - gainers/losers and trending stocks
 */
module.exports = {
  async getMovers() {
    return {
      ok: true,
      gainers: [],
      losers: [],
      mostActive: []
    };
  }
};

