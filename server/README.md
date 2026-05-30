# EasyMoney AI Server (PHASE 3 Skeleton)

This lightweight Node backend gives you the skeleton for Smart Event-Based RAG and AI-driven features.

Files created:
- `index.js` - Express server + endpoints
- `eventProcessor.js` - event routing + cron scaffold
- `insightService.js` - RAG/insight generation stub
- `embeddingService.js` - embedding abstraction
- `vectorStore.js` - vector DB abstraction
- `db/schema.sql` - minimal schema

Getting started (macOS):

1. Node 18+ & npm installed
2. From project root:

```bash
cd server
npm install
cp .env.example .env  # create appropriate API keys and settings
npm run dev
```

.env variables you may set:
- `OPENAI_API_KEY` - for OpenAI embeddings (optional)
- `OPENAI_URL` - custom OpenAI-compatible endpoint (e.g., Pollinations)
- `EMBEDDING_PROVIDER` - `openai` | `pinecone` | `dummy`
- `VECTOR_INDEX` - index name for your vector DB

PHASE 3 implementation checklist (priority):
1. Implement event sources (news feed, price monitor, analyst API)
2. Wire embeddings and a vector DB (Pinecone / Weaviate / FAISS)
3. Implement RAG prompt templates and LLM provider selection
4. Add notification wiring (FCM / APNs)
5. Build Android UI screens and notification handlers

This scaffold is intentionally minimal. I can wire Pinecone or Weaviate and provide working integration next — tell me which vector DB you prefer.

