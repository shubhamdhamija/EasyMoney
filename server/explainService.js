/**
 * Explain why a stock moved - uses RAG to provide context
 */
module.exports = {
  async explainStockMove(symbol) {
    // Placeholder: This would normally retrieve relevant news/events from the vector store
    // and use an LLM to generate an explanation
    return {
      explanation: `Recent market data and sentiment analysis suggest ${symbol} moved due to technical factors and sector momentum.`
    };
  }
};

