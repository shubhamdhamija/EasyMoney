const axios = require('axios');

/**
 * createEmbedding(text)
 * - Uses OpenAI embeddings when OPENAI_API_KEY is provided.
 * - Otherwise returns a deterministic dummy vector for local dev.
 *
 * Inputs: text string
 * Outputs: Float32Array (JS array of numbers)
 */
async function createEmbedding(text, opts = {}) {
  const provider = process.env.EMBEDDING_PROVIDER || (process.env.OPENAI_API_KEY ? 'openai' : 'dummy');

  if (provider === 'openai') {
    const urlBase = process.env.OPENAI_URL || 'https://api.openai.com';
    const model = opts.model || process.env.EMBEDDING_MODEL || 'text-embedding-3-small';
    const resp = await axios.post(`${urlBase}/v1/embeddings`, {
      input: text,
      model
    }, {
      headers: {
        'Authorization': `Bearer ${process.env.OPENAI_API_KEY}`,
        'Content-Type': 'application/json'
      },
      timeout: 15000
    });

    // OpenAI response shape: { data: [ { embedding: [...] } ] }
    const embedding = resp.data?.data?.[0]?.embedding;
    if (!embedding) throw new Error('No embedding returned from OpenAI');
    return embedding;
  }

  // Dummy deterministic embedding: map chars to small floats. Dimension 1536 by default.
  const dim = opts.dim || parseInt(process.env.EMBEDDING_DIM || '1536', 10);
  const out = new Array(dim);
  for (let i = 0; i < dim; i++) {
    const code = text.charCodeAt(i % text.length) || 0;
    out[i] = Math.sin((code + i) % 100) / 100.0;
  }
  return out;
}

module.exports = { createEmbedding };
