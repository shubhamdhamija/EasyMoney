# EasyMoney - Stock Market Intelligence App

A modern Android app for real-time stock tracking, AI-powered insights, and portfolio management.

## 🎯 Quick Start

**New to the project?** Start here:
👉 [**QUICK_START.md**](QUICK_START.md) - Get the app running in 5 minutes

## 📋 Project Structure

```
EasyMoney/
├── app/                          # Android app (Kotlin/Compose)
│   ├── src/main/java/
│   │   ├── data/                # API services, repositories, Room DB
│   │   ├── domain/              # Business models, interfaces
│   │   ├── ui/                  # Jetpack Compose screens
│   │   ├── di/                  # Hilt dependency injection
│   │   └── util/                # Constants, utilities
│   ├── build.gradle.kts         # App dependencies
│   └── src/main/AndroidManifest.xml
│
└── server/                       # Node.js backend
    ├── index.js                 # Express API server
    ├── package.json             # Node dependencies
    ├── .env                     # Configuration (API keys)
    └── services/                # Business logic modules
```

## ✨ Features

### Home Screen
- **Gainers/Losers**: Real-time market movers
- **Trending**: Popular stocks
- **AI Picks**: AI-powered stock recommendations
- **Search**: Quick stock symbol search

### Stock Details
- Full company information
- Intraday price chart
- Latest news
- AI insights
- Price alerts

### Watchlist
- Track favorite stocks
- Earnings calendar
- Historical EPS data
- Quick actions

### Alerts
- Price threshold monitoring
- Real-time notifications
- Custom alert rules

## 🛠 Technology Stack

### Android App
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: Clean Architecture + MVVM
- **Database**: Room
- **Network**: Retrofit + OkHttp
- **DI**: Hilt
- **State Management**: MutableStateFlow + StateFlow

### Backend Server
- **Runtime**: Node.js 18+
- **Framework**: Express.js
- **APIs**: Yahoo Finance, IEX, Finnhub
- **Features**: AI insights, earnings tracking, event processing

## 🚀 Getting Started

### Prerequisites
- Android Studio (Latest)
- Android SDK 30+
- Node.js 18+ (for backend)

### 1. Build Android App
```bash
./gradlew assembleDebug
```

### 2. Start Backend Server
```bash
cd server
npm install
npm start
```

### 3. Run App
```bash
./gradlew installDebug  # Install to emulator/device
```

**Detailed instructions**: See [QUICK_START.md](QUICK_START.md)

## 📱 App Architecture

### Clean Architecture Layers

```
UI Layer (Compose Screens)
    ↓
ViewModel Layer (State Management)
    ↓
Domain Layer (Business Models & Interfaces)
    ↓
Data Layer (Repositories & API Services)
    ↓
External APIs (Yahoo Finance, IEX, Finnhub)
```

### Key Classes

| Component | Purpose |
|-----------|---------|
| `HomeScreen` | Main app entry point with tabs |
| `StockDetailScreen` | Full stock information view |
| `WatchlistScreen` | User's tracked stocks |
| `StockRepositoryImpl` | Stock data from APIs |
| `BackendRepositoryImpl` | Backend service integration |
| `HomeViewModel` | Home screen state |
| `StockDetailViewModel` | Detail screen state |

## 🔧 Configuration

### Backend Configuration
**File:** `server/.env`
```env
FINNHUB_API_KEY=your_key_here
PORT=8080
LLM_PROVIDER=pollinations
```

### App Configuration
**File:** `app/src/main/java/com/invest/easymoney/util/Constants.kt`
```kotlin
const val IEX_API_TOKEN = "your_key_here"
```

## 🐛 Troubleshooting

### No data showing in app?
1. ✅ Backend server running? → `curl http://localhost:8080/`
2. ✅ Internet access? → Check emulator settings
3. ✅ API keys configured? → Check `.env` and `Constants.kt`

See [DATA_LOADING_TROUBLESHOOTING.md](DATA_LOADING_TROUBLESHOOTING.md) for detailed help.

## 📚 Documentation

- [QUICK_START.md](QUICK_START.md) - Get running quickly
- [DATA_LOADING_TROUBLESHOOTING.md](DATA_LOADING_TROUBLESHOOTING.md) - Debug data issues
- [server/SETUP.md](server/SETUP.md) - Backend server setup
- [FULL_APP_DOCUMENTATION.md](FULL_APP_DOCUMENTATION.md) - Complete code reference

## 📊 API Endpoints

### Stock Data (Direct from APIs)
- Yahoo Finance: `https://query1.finance.yahoo.com/v8/finance/...`
- IEX: `https://cloud.iexapis.com/stable/...`
- Finnhub: `https://finnhub.io/api/v1/...`

### Backend Services (http://localhost:8080)
- `GET /` - Health check
- `GET /ai/insight` - Stock insight
- `GET /ai/explain/:symbol` - Explain movement
- `GET /ai/earnings/:symbol` - Earnings data
- `GET /community/trending` - Trending stocks

## 🎓 Architecture Decisions

### Why Compose over XML?
- Modern declarative UI
- Better state management
- Faster development
- Type-safe

### Why Clean Architecture?
- Separation of concerns
- Testability
- Maintainability
- Scalability

### Why Room over SharedPreferences?
- Relational data (watchlist, alerts)
- Query flexibility
- Built-in migrations
- Type safety

## 🚢 Deployment

### Android App
1. Update version in `app/build.gradle.kts`
2. Build release:
   ```bash
   ./gradlew assembleRelease
   ```
3. Sign and upload to Play Store

### Backend Server
1. Set environment variables on server
2. Run: `NODE_ENV=production npm start`
3. Use process manager (PM2):
   ```bash
   pm2 start index.js --name easymoney-server
   ```

## 📞 Support

For issues or questions:
1. Check the troubleshooting guides
2. Review Logcat output in Android Studio
3. Check server console logs

## 📄 License

[Add your license here]

---

**Built with ❤️ using Kotlin, Compose, and Node.js**

