// Small simulation engine: compute historical performance and project simple returns

function simulatePastReturn(prices, investment = 1000) {
  if (!prices || prices.length < 2) return investment;
  const start = prices[0];
  const end = prices[prices.length-1];
  const ret = (end - start) / (start || 1);
  return investment * (1 + ret);
}

function projectSimpleReturn(currentPrice, expectedReturnPct, investment = 1000) {
  const future = investment * (1 + expectedReturnPct/100);
  return { futureValue: future, expectedReturnPct };
}

module.exports = { simulatePastReturn, projectSimpleReturn };

