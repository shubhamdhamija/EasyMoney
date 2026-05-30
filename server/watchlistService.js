const { getWatchlistContext } = require('./watchlistRetriever');
const { buildWatchlistPrompt } = require('./watchlistPromptBuilder');
const axios = require('axios');
const embeddingService = require('./embeddingService');

// Simple in-memory cache. For production, use Redis.
const cache = new Map();
const CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

async function getUserWatchlist(userId) {
  // TODO: replace with DB query. For now return sample list or read from env
  if (process.env.SAMPLE_WATCHLIST) return process.env.SAMPLE_WATCHLIST.split(',').map(s => s.trim()).filter(Boolean);
  return ['AAPL', 'NVDA', 'TSLA'];
}

async function callLLM(prompt) {
  const provider = process.env.LLM_PROVIDER || (process.env.OPENAI_API_KEY ? 'openai' : 'pollinations');

  if (provider === 'openai') {
    const urlBase = process.env.OPENAI_URL || 'https://api.openai.com';
    const resp = await axios.post(`${urlBase}/v1/chat/completions`, {
      model: process.env.LLM_MODEL || 'gpt-4o-mini',
      messages: [
        { role: 'system', content: 'You are a financial AI assistant.' },
        { role: 'user', content: prompt }
      ],
      max_tokens: 400
    }, {
      headers: { Authorization: `Bearer ${process.env.OPENAI_API_KEY}` },
      timeout: 30000
    });
    return resp.data.choices?.[0]?.message?.content;
  }

  // Pollinations or other open endpoint (OpenAI-compatible)
  if (provider === 'pollinations' || provider === 'openai_compat') {
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

  // Fallback: lightweight local summary without LLM (deterministic)
  return localSummary(prompt);
}

function localSummary(prompt) {
  // Very naive extraction: return first 300 chars as "summary" as fallback
  return prompt.substring(0, 300) + (prompt.length > 300 ? '...' : '');
}

async function generateWatchlistSummary(userId) {
  const cacheKey = `watchlist:${userId}`;
  const cached = cache.get(cacheKey);
  if (cached && (Date.now() - cached.ts) < CACHE_TTL_MS) return cached.value;

  const symbols = await getUserWatchlist(userId);
  if (!symbols || symbols.length === 0) return 'No stocks in your watchlist.';

  // Limit to first N symbols to bound cost
  const maxSymbols = parseInt(process.env.MAX_WATCHLIST_SYMBOLS || '10', 10);
  const limited = symbols.slice(0, maxSymbols);

  // Retrieve RAG context
  const contextDocs = await getWatchlistContext(limited, 3);
  if (!contextDocs || contextDocs.length === 0) return 'No significant updates in your watchlist today.';

  // Build prompt and call LLM
  const prompt = buildWatchlistPrompt(limited, contextDocs);
  const llmResp = await callLLM(prompt);

  // Cache and return
  cache.set(cacheKey, { value: llmResp, ts: Date.now() });
  return llmResp;
}

module.exports = { generateWatchlistSummary, getUserWatchlist };
