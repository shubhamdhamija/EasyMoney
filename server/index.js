require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const axios = require('axios');
const cron = require('node-cron');
const eventProcessor = require('./eventProcessor');
const insightService = require('./insightService');
const explainService = require('./explainService');
const watchlistService = require('./watchlistService');
const portfolioService = require('./portfolioService');
const signalEngine = require('./signalEngine');
const investorTracker = require('./investorTracker');
const sectorAnalysis = require('./sectorAnalysis');
const simulationEngine = require('./simulationEngine');
// Phase 5
const copilotService = require('./copilotService');
const userProfileService = require('./userProfileService');
const communityService = require('./communityService');
const predictionEngine = require('./predictionEngine');
// New services
const earningsService = require('./earningsService');
const moversService = require('./moversService');
const { buildFullContext } = require('./systemOrchestrator');
const { createArticleSearchService } = require('./services/articleSearchService');
const { createArticleSearchRouter } = require('./routes/articleSearchRoutes');
const {
  createAppDependencies
} = require('./appDependencies');
const {
  createStockExplanationRouter
} = require('./routes/stockExplanationRoutes');
const {
  createStockInsightRouter
} = require('./routes/stockInsightRoutes');
const {
  createStockPicksRouter
} = require('./routes/stockPicksRoutes');
const {
  requestIdMiddleware
} = require(
  './middleware/requestId'
);

const {
  errorHandler
} = require(
  './middleware/errorHandler'
);

const app = express();
const dependencies =
  createAppDependencies({
    env:
      process.env,

    fetchImpl:
      globalThis.fetch
  });

app.use(bodyParser.json());
app.use(requestIdMiddleware);
app.use('/articles', createArticleSearchRouter({
  articleSearchService: createArticleSearchService()
}));
app.use(
  '/ai/explain',
  createStockExplanationRouter({
    stockExplanationService:
      dependencies.stockExplanationService
  })
);
app.use(
  '/ai/stock-insight',
  createStockInsightRouter({
    stockInsightService:
      dependencies.stockInsightService
  })
);
app.use(
  '/ai/picks',
  createStockPicksRouter({
    stockPicksService:
      dependencies.stockPicksService
  })
);

function hasFunction(target, methodName) {
  return !!target && typeof target[methodName] === 'function';
}

async function invokeIfAvailable(target, methodName, args = [], label = methodName) {
  if (!hasFunction(target, methodName)) {
    console.warn(`[startup] ${label} unavailable; skipping`);
    return null;
  }

  try {
    return await target[methodName](...args);
  } catch (e) {
    console.warn(`[startup] ${label} failed:`, e.message);
    return null;
  }
}

app.get('/', (req, res) => res.send('EasyMoney AI Server'));

// Trigger an event for testing
app.post('/event', async (req, res) => {
  try {
    const { type, payload } = req.body;
    const result = await eventProcessor.handleEvent(type, payload);
    res.json({ ok: true, result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Generate insight endpoint
app.post('/ai/insight', async (req, res) => {
  try {
    const { symbol, context } = req.body;
    const insight = await insightService.generateInsight(symbol, context || {});
    res.json({ ok: true, insight });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Ingest news articles for a symbol and store as vectors
app.post('/ai/process-news', async (req, res) => {
  try {
    const { symbol, news } = req.body;
    if (!symbol || !Array.isArray(news)) return res.status(400).json({ ok: false, error: 'Missing symbol or news array' });
    const result = await insightService.processNews(news, symbol);
    res.json({ ok: true, result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Watchlist summary
app.get('/ai/watchlist-summary', async (req, res) => {
  try {
    const userId = req.query.userId;
    const summary = await insightService.watchlistSummary(userId);
    res.json({ ok: true, summary });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Watchlist summary (per-user)
app.get('/ai/watchlist-summary/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const summary = await watchlistService.generateWatchlistSummary(userId);
    res.json({ ok: true, summary });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Portfolio recommendation
app.get('/ai/portfolio', async (req, res) => {
  try {
    const { risk = 'medium', symbols, userId = 'default' } = req.query;
    const symbolList = symbols ? symbols.split(',').map(s => s.trim()) : [];
    const out = await portfolioService.generatePortfolio({ risk, symbols: symbolList, userId });
    res.json({ ok: true, ...out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Signals (RAG-enhanced)
app.post('/ai/signal', async (req, res) => {
  try {
    const { symbol, marketData } = req.body;
    const out = await signalEngine.analyzeSymbol(symbol, marketData || {});
    res.json({ ok: true, ...out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Investor tracker
app.get('/ai/investor/:name', async (req, res) => {
  try {
    const out = await investorTracker.getTopBuysByInstitution(req.params.name);
    res.json({ ok: true, out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Sector analysis
app.get('/ai/sectors', async (req, res) => {
  try {
    const out = await sectorAnalysis.analyzeSectors();
    res.json({ ok: true, out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Stock search (Finnhub-backed)
app.get('/stocks/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q) return res.status(400).json({ ok: false, error: 'Missing query param q' });
    const apiKey = process.env.FINNHUB_API_KEY;
    if (!apiKey) return res.status(503).json({ ok: false, error: 'FINNHUB_API_KEY not configured' });
    const response = await axios.get(
      `https://finnhub.io/api/v1/search?q=${encodeURIComponent(q)}&token=${apiKey}`,
      { timeout: 10000 }
    );
    const results = (response.data.result || [])
      .filter(s => s.type === 'Common Stock' || !s.type)
      .slice(0, 20)
      .map(s => ({ symbol: s.symbol, name: s.description, exchange: s.primaryExchange || '' }));
    res.json({ ok: true, results });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// "Why did this stock move?" — RAG-powered explanation
app.get('/ai/explain/:symbol', async (req, res) => {
  try {
    const { symbol } = req.params;
    const result = await explainService.explainStockMove(symbol.toUpperCase());
    const explanation = typeof result === 'string'
      ? result
      : result?.explanation || 'No explanation available.';
    res.json({ ok: true, explanation });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Simulation endpoints
app.post('/ai/simulate/past', async (req, res) => {
  try {
    const { prices, investment } = req.body;
    const out = simulationEngine.simulatePastReturn(prices, investment || 1000);
    res.json({ ok: true, value: out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/ai/simulate/future', async (req, res) => {
  try {
    const { currentPrice, expectedReturnPct, investment } = req.body;
    const out = simulationEngine.projectSimpleReturn(currentPrice, expectedReturnPct || 5, investment || 1000);
    res.json({ ok: true, out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: AI Copilot ───────────────────────────────────────────────────────
app.post('/ai/copilot', async (req, res) => {
  try {
    const { userId = 'default', question, messages = [], watchlist = [], riskProfile = 'medium' } = req.body;
    if (!question) return res.status(400).json({ ok: false, error: 'Missing question' });
    const result = await copilotService.answerQuestion({ userId, question, messages, watchlist, riskProfile });
    res.json({ ok: true, ...result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: User Profile ──────────────────���──────────────────────────────────
app.get('/user/profile/:userId', (req, res) => {
  try {
    const profile = userProfileService.getProfile(req.params.userId);
    res.json({ ok: true, profile });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/user/profile/:userId', (req, res) => {
  try {
    const profile = userProfileService.updateProfile(req.params.userId, req.body);
    res.json({ ok: true, profile });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/user/search/:userId', (req, res) => {
  try {
    const { symbol } = req.body;
    userProfileService.recordSearch(req.params.userId, symbol);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: Community ────────────────────────────────────────────────────────
app.get('/community/trending', (req, res) => {
  try {
    const limit = parseInt(req.query.limit || '10', 10);
    const trending = communityService.getTrending(limit);
    res.json({ ok: true, trending });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/community/watchlist', (req, res) => {
  try {
    const { userId, symbol, action } = req.body;
    if (!userId || !symbol || !['add', 'remove'].includes(action)) {
      return res.status(400).json({ ok: false, error: 'Missing userId, symbol, or action (add|remove)' });
    }
    communityService.recordWatchlistChange(userId, symbol, action);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: Prediction Engine ────────────────────────────────────────────────
app.get('/ai/predict/:symbol', async (req, res) => {
  try {
    const result = await predictionEngine.predictSymbol(req.params.symbol);
    res.json({ ok: true, ...result });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});


// All routes above this line.
app.use(errorHandler);

const port = process.env.PORT || 8080;
app.listen(port, '0.0.0.0', () => console.log('EasyMoney AI Server listening on', port));
