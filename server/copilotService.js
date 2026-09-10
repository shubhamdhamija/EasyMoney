/**
 * AI Copilot service - conversational investment assistant
 */
module.exports = {
  async answerQuestion({ userId, question, messages = [], watchlist = [], riskProfile = 'medium' }) {
    try {
      return {
        ok: true,
        reply: `I understand you're asking: "${question}". This is a AI copilot placeholder. Configure LLM provider in .env to enable.`,
        context: {},
        suggestions: []
      };
    } catch (e) {
      return { ok: false, error: e.message };
    }
  }
};

