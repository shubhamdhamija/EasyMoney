function buildWatchlistPrompt(symbols, contextDocs) {
  const grouped = symbols.map(symbol => {
    const docs = contextDocs
      .filter(d => d.symbol === symbol)
      .map(d => `- ${d.text}`)
      .join('\n');
    return `Stock: ${symbol}\n${docs}`;
  }).join('\n\n');

  return `You are a financial AI assistant. Analyze the following watchlist stocks:\n\n${grouped}\n\nTasks:\n1. Overall watchlist sentiment (bullish, bearish, neutral)\n2. Highlight best performing stocks\n3. Highlight risky or volatile stocks\n4. Give a short overall outlook\n\nResponse format:\n- Summary (2-3 lines)\n- Bullet points per key stock\n- Final outlook\n\nKeep it concise and clear. Do NOT give financial advice.`;
}

module.exports = { buildWatchlistPrompt };

