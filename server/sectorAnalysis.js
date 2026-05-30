const vectorStore = require('./vectorStore');
const embeddingService = require('./embeddingService');

// Very basic sector analysis via keyword matching in stored docs.
async function analyzeSectors(sectors = ['AI','Tech','Energy','Healthcare','Finance']) {
  // For now, scan local vectors for keywords to approximate momentum
  const scores = {};
  for (const s of sectors) scores[s] = 0;

  // Use local files (if Pinecone configured, you'd run queries per sector)
  const all = require('fs').readdirSync(require('path').join(__dirname, 'local_vectors')).filter(f => f.endsWith('.json'));
  for (const f of all) {
    try {
      const data = JSON.parse(require('fs').readFileSync(require('path').join(__dirname, 'local_vectors', f), 'utf8'));
      const text = (data.metadata?.text || '').toLowerCase();
      for (const s of sectors) {
        if (text.includes(s.toLowerCase())) scores[s] += 1;
      }
    } catch (e) {}
  }

  return Object.keys(scores).map(k => ({ sector: k, score: scores[k] }));
}

module.exports = { analyzeSectors };

