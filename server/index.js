require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const eventProcessor = require('./eventProcessor');
const insightService = require('./insightService');
const watchlistService = require('./watchlistService');
const portfolioService = require('./portfolioService');
const signalEngine = require('./signalEngine');
const investorTracker = require('./investorTracker');
const sectorAnalysis = require('./sectorAnalysis');
const simulationEngine = require('./simulationEngine');

const app = express();
app.use(bodyParser.json());

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
    const { risk = 'medium', symbols } = req.query;
    const symbolList = symbols ? symbols.split(',').map(s => s.trim()) : [];
    const out = await portfolioService.generatePortfolio({ risk, symbols: symbolList });
    res.json({ ok: true, ...out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Signals
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

const port = process.env.PORT || 8080;
app.listen(port, () => console.log('EasyMoney AI Server listening on', port));
