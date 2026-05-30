-- Documents table for RAG
CREATE TABLE documents (
  id UUID PRIMARY KEY,
  stock_symbol TEXT,
  content TEXT,
  embedding BYTEA,
  type TEXT,
  created_at TIMESTAMP DEFAULT now()
);

-- Events table (optional)
CREATE TABLE events (
  id UUID PRIMARY KEY,
  type TEXT,
  payload JSONB,
  processed_at TIMESTAMP
);

