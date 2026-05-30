// Small abstraction that supports multiple vector backends: Pinecone, Weaviate, FAISS (local)

const fs = require('fs');
const path = require('path');
const { PineconeClient } = require('@pinecone-database/pinecone');

const INDEX_NAME = process.env.VECTOR_INDEX || 'stock-index';
const STORAGE_DIR = path.join(__dirname, 'local_vectors');
const NEWS_INDEX = process.env.PINECONE_NEWS_INDEX || 'stock-news-index';
const SIGNAL_INDEX = process.env.PINECONE_SIGNAL_INDEX || 'stock-signal-index';

// Ensure local dir exists
if (!fs.existsSync(STORAGE_DIR)) fs.mkdirSync(STORAGE_DIR, { recursive: true });

// Pinecone client (lazy)
let pinecone = null;
async function getPineconeClient() {
  if (!pinecone) {
    if (!process.env.PINECONE_API_KEY || !process.env.PINECONE_ENVIRONMENT) return null;
    pinecone = new PineconeClient();
    await pinecone.init({ apiKey: process.env.PINECONE_API_KEY, environment: process.env.PINECONE_ENVIRONMENT });
  }
  return pinecone;
}

function cosine(a, b) {
  let dot = 0, na = 0, nb = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    na += a[i] * a[i];
    nb += b[i] * b[i];
  }
  return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-12);
}

// Local storage format: one JSON file per document in local_vectors/{id}.json
async function upsertLocal(id, embedding, metadata) {
  const file = path.join(STORAGE_DIR, `${id}.json`);
  const payload = { id, embedding, metadata, ts: Date.now() };
  fs.writeFileSync(file, JSON.stringify(payload));
  return { ok: true };
}

async function queryLocal(queryEmbedding, topK = 5, filter = {}) {
  const files = fs.readdirSync(STORAGE_DIR).filter(f => f.endsWith('.json'));
  const results = [];
  for (const f of files) {
    try {
      const data = JSON.parse(fs.readFileSync(path.join(STORAGE_DIR, f), 'utf8'));
      // filter by metadata if provided
      if (filter && filter.symbol && filter.symbol['$eq']) {
        if (data.metadata?.symbol !== filter.symbol['$eq']) continue;
      }
      const score = cosine(queryEmbedding, data.embedding);
      results.push({ id: data.id, score, metadata: data.metadata, text: data.metadata?.text });
    } catch (e) { }
  }
  results.sort((a, b) => b.score - a.score);
  return results.slice(0, topK);
}

function getIndexName(type) {
  if (type === 'analyst' || type === 'earnings' || type === 'signal') return SIGNAL_INDEX;
  return NEWS_INDEX;
}

async function upsertDocument(id, embedding, metadata) {
  // metadata: { symbol, type, sentiment, timestamp, source, text, summary }
  const usePinecone = !!process.env.PINECONE_API_KEY;
  const type = metadata.type || 'news';
  const indexName = getIndexName(type);
  if (usePinecone) {
    const client = await getPineconeClient();
    if (!client) return upsertLocal(id, embedding, metadata);
    const index = client.Index(indexName);
    await index.upsert({ upsertRequest: { vectors: [{ id, values: embedding, metadata }] } });
    return { ok: true };
  }
  return upsertLocal(id, embedding, metadata);
}

async function querySimilar(embedding, topK = 5, filter = {}, type = 'news') {
  const usePinecone = !!process.env.PINECONE_API_KEY;
  const indexName = getIndexName(type);
  if (usePinecone) {
    const client = await getPineconeClient();
    if (!client) return queryLocal(embedding, topK, filter);
    const index = client.Index(indexName);
    const q = {
      vector: embedding,
      topK,
      includeMetadata: true,
      filter
    };
    const resp = await index.query({ queryRequest: q });
    return resp.matches.map(m => ({ id: m.id, score: m.score, metadata: m.metadata }));
  }
  return queryLocal(embedding, topK, filter);
}

module.exports = { upsertDocument, querySimilar, getIndexName };
