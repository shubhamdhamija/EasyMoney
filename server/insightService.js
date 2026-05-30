const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');
const { v4: uuidv4 } = require('uuid');

// Simple RAG flow: embed query, get top docs, call LLM (user can wire provider)

async function fetchContextForSymbol(symbol) {
  // Retrieve top 5 relevant docs from vector store
  const queryEmbedding = await embeddingService.createEmbedding(symbol);
  const matches = await vectorStore.querySimilar(queryEmbedding, 5, { symbol: { "$eq": symbol } });
  return matches.map(m => ({ id: m.id, text: m.metadata?.text || '', score: m.score }));
}

async function generateInsight(symbol, opts = {}) {
  const contextDocs = opts.context || (await fetchContextForSymbol(symbol));
  const contextText = contextDocs.map(d => d.text).join('\n').slice(0, 16_000);
  const shortSummary = `Auto-insight for ${symbol}: ${contextText ? contextText.substring(0, 240) + (contextText.length > 240 ? '...' : '') : 'No context available.'}`;
  return { shortSummary, context: contextDocs };
}

function normalizeText(symbol, headline, summary, source) {
  return `Stock: ${symbol}\nHeadline: ${headline}\nSummary: ${summary}\nSource: ${source}`;
}

async function processNews(newsList, symbol) {
  for (const article of newsList) {
    // Avoid duplicates by id if provided
    const id = article.id ? article.id.toString() : uuidv4();
    const type = article.type || 'news';
    const sentiment = article.sentiment || 'neutral';
    const timestamp = article.timestamp || article.datetime || Date.now();
    const source = article.source || 'unknown';
    const headline = article.headline || '';
    const summary = article.summary || article.content || '';
    const text = normalizeText(symbol, headline, summary, source);
    const embedding = await embeddingService.createEmbedding(text);
    const metadata = { symbol, type, sentiment, timestamp, source, text, summary };
    await vectorStore.upsertDocument(id, embedding, metadata);
  }
  return { ok: true, count: newsList.length };
}

async function watchlistSummary(userId) {
  // Placeholder: in production, fetch user watchlist and create aggregated RAG per symbol
  return { userId, summary: 'Your watchlist summary is not configured yet.' };
}

module.exports = { fetchContextForSymbol, generateInsight, processNews, watchlistSummary };
