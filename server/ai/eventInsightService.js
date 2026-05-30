const axios = require('axios');
const { getEventContext } = require('./eventRetriever');

async function generateEventInsight(event) {
  const { symbol, changePercent, type } = event;
  const context = await getEventContext(symbol);
  const prompt = `Stock: ${symbol}\nEvent: ${type}\nPrice Change: ${changePercent ? changePercent.toFixed(2) : 'N/A'}%\nContext:${context}\nTasks:\n1. Explain why this event happened\n2. Identify sentiment (bullish/bearish)\n3. Provide short-term outlook\nKeep it concise and factual.\n\nDisclaimer: This is AI-generated analysis, not financial advice.`;

  // Use OpenAI or Pollinations
  const provider = process.env.LLM_PROVIDER || (process.env.OPENAI_API_KEY ? 'openai' : 'pollinations');
  if (provider === 'openai') {
    const urlBase = process.env.OPENAI_URL || 'https://api.openai.com';
    const resp = await axios.post(`${urlBase}/v1/chat/completions`, {
      model: process.env.LLM_MODEL || 'gpt-4o-mini',
      messages: [
        { role: 'system', content: 'You are a financial AI assistant.' },
        { role: 'user', content: prompt }
      ],
      max_tokens: 300
    }, {
      headers: { Authorization: `Bearer ${process.env.OPENAI_API_KEY}` },
      timeout: 30000
    });
    return resp.data.choices?.[0]?.message?.content;
  }
  // Pollinations fallback
  const url = (process.env.OPENAI_URL || 'https://text.pollinations.ai') + '/openai';
  const resp = await axios.post(url, {
    model: process.env.LLM_MODEL || 'openai',
    messages: [
      { role: 'system', content: 'You are a financial AI assistant.' },
      { role: 'user', content: prompt }
    ]
  }, { timeout: 30000 });
  return resp.data?.choices?.[0]?.message?.content || resp.data;
}

module.exports = { generateEventInsight };

