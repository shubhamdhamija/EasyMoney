# EasyMoney Backend Server Setup Guide

## Prerequisites

- **Node.js 18+** - Download from https://nodejs.org/ or install with Homebrew:
  ```bash
  brew install node
  ```
- **npm** (comes with Node.js)

## Installation

1. **Navigate to server directory:**
   ```bash
   cd server
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Configure environment variables:**
   ```bash
   cp .env.example .env
   ```
   Edit `.env` and add your API keys:
   - **FINNHUB_API_KEY** (free tier: https://finnhub.io) - Essential for stock data
   - **OPENAI_API_KEY** (optional: for AI insights)
   - **POLYGON_API_KEY** (optional: for movers data)

## Running the Server

### Development Mode (with auto-reload):
```bash
npm run dev
```

### Production Mode:
```bash
npm start
```

The server will start on **http://localhost:8080**

## Troubleshooting

### "Module not found" errors
Run: `npm install`

### Port 8080 already in use
Change PORT in `.env` or kill the process using the port:
```bash
# macOS/Linux
lsof -i :8080
kill -9 <PID>
```

### API responses are empty
Ensure **FINNHUB_API_KEY** is configured in `.env` file

### Android app can't connect to server
- **Emulator:** Server URL should be `http://10.0.2.2:8080`
- **Physical device:** Server URL should be your machine's IP (e.g., `http://192.168.1.100:8080`)
- Both are auto-detected in `Constants.BACKEND_BASE_URL`

## Available Endpoints

### Stock Data (Yahoo Finance/IEX APIs - app calls these directly)
- `GET /stocks/search?q=AAPL` - Search stocks
- `GET /` - Health check

### AI & Analytics (Called by app via BackendRepository)
- `GET /ai/insight` - Get AI stock insight
- `GET /ai/explain/:symbol` - Explain stock movement
- `GET /ai/earnings/:symbol` - Get earnings data
- `GET /ai/earnings/upcoming` - Upcoming earnings
- `POST /ai/signal` - Analyze signals
- `GET /ai/predict/:symbol` - Price prediction

### Community & User Data
- `GET /community/trending` - Trending stocks
- `POST /community/watchlist` - Record watchlist changes
- `GET /user/profile/:userId` - User profile

### Server Status
The server logs startup info with all enabled endpoints.

## Monitoring

Check server logs for:
- "listening on port 8080" - Server started successfully
- Network errors - Check API keys and internet connection
- Database errors - Check database configuration if using persistent storage

