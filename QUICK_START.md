# EasyMoney Quick Start Guide

## ✅ App is Built Successfully

Your Android app compiles without errors! However, to see data, you need to:

1. **Install Node.js** (if not already done)
2. **Start the backend server**
3. **Configure API keys**
4. **Run the app**

---

## Step 1: Install Node.js

### macOS
```bash
# Option A: Using Homebrew
brew install node

# Option B: Download directly
# Visit https://nodejs.org/ and download LTS version
```

### Verify Installation
```bash
node --version  # Should show v18+ or higher
npm --version   # Should show 9+
```

---

## Step 2: Start Backend Server

```bash
cd server
npm install
npm start
```

**Expected output:**
```
EasyMoney AI Server listening on 8080
```

### Troubleshooting Server Startup

**Error: `Cannot find module`**
```bash
npm install
```

**Error: `Port 8080 already in use`**
```bash
# Kill the process using port 8080
lsof -i :8080
kill -9 <PID>
```

**Error: `FINNHUB_API_KEY not configured`**
- The server will still start, but some features won't work
- Later, add your API key to `server/.env`

---

## Step 3: Configure (Optional for now, but recommended)

### Get Free API Keys

**Finnhub API** (for market data):
1. Go to https://finnhub.io
2. Sign up (free tier available)
3. Copy your API key

**IEX Cloud** (for stock trending):
1. Go to https://iexcloud.io
2. Sign up (free tier available)
3. Copy your API key

### Update Configuration

**File:** `server/.env`
```
FINNHUB_API_KEY=pk_YOUR_KEY_HERE
```

**File:** `app/src/main/java/com/invest/easymoney/util/Constants.kt`
```kotlin
const val IEX_API_TOKEN = "YOUR_IEX_TOKEN"
```

Then rebuild the app:
```bash
./gradlew assembleDebug
```

---

## Step 4: Run the App

### On Emulator
1. Open Android Studio
2. Start an Android emulator (API 30+)
3. Click "Run" button or press Shift+F10
4. Wait for app to load
5. You should see stock data loading

### On Physical Device
1. Connect phone via USB
2. Enable "USB Debugging" in Developer Options
3. Click "Run" button in Android Studio
4. Wait for app to install and load
5. You should see stock data loading

---

## ✅ Verification Checklist

### Backend Server is Running
```bash
curl http://localhost:8080/
# Should respond: "EasyMoney AI Server"
```

### App has Internet Access
- Run this on emulator:
  ```bash
  adb shell pm grant com.invest.easymoney android.permission.INTERNET
  ```

### Stock Data is Loading
- Open app
- Go to "Home" tab
- Should see stocks loading with names and prices
- Wait 5-10 seconds for data

### Emulator Connection to Backend (Auto-detected)
- Emulator uses `http://10.0.2.2:8080` automatically
- No configuration needed if server is on same machine

---

## 📊 Expected Features

Once everything is set up:

### Home Tab
- **Gainers**: Top 5 stocks with highest gains
- **Losers**: Top 5 stocks with highest losses
- **Trending**: Trending stocks like AAPL, NVDA, TSLA
- **AI Picks**: AI recommendations (requires backend)

### Watchlist Tab
- Add/remove stocks from watchlist
- View earnings data
- Real-time price updates

### Stock Detail
- Full stock info (price, change, chart)
- Latest news
- AI insight
- Price alerts

### Alerts Tab
- Set price alerts
- Receive notifications

---

## 🆘 Troubleshooting

### "App shows loading, then nothing"
1. Check Logcat for errors
2. Verify backend is running: `curl http://localhost:8080/`
3. Check internet access on emulator/device
4. Restart the app

### "API Key errors"
1. These can be ignored initially
2. Without API keys, some features won't work
3. Add keys to `.env` and `.env` files for full functionality

### "Connection refused / timeout"
1. Backend server not running
2. Check if running: `curl http://localhost:8080/`
3. Start it: `npm start` in `server/` directory

### "Physical device can't connect to backend"
1. Update `Constants.BACKEND_BASE_URL` to your machine's IP
2. Example: `http://192.168.1.100:8080`
3. Find your IP: `ifconfig` (macOS) or `ipconfig` (Windows)

---

## 📝 Project Files Reference

| File | Purpose |
|------|---------|
| `server/index.js` | Backend API server |
| `server/.env` | Backend configuration (API keys) |
| `app/src/main/java/.../StockRepositoryImpl.kt` | Stock data fetching |
| `app/src/main/java/.../HomeViewModel.kt` | Home screen data |
| `app/src/main/java/.../Constants.kt` | App configuration |

---

## 🚀 Next Steps

1. ✅ Build app - DONE
2. 📦 Install Node.js - DO THIS
3. ▶️ Start backend server - DO THIS
4. 📱 Run app - DO THIS
5. 🎉 See data flowing!

---

## 💡 Tips

- **Live development:** Use `npm run dev` instead of `npm start` for auto-reload
- **Check logs:** Server console shows all API calls and errors
- **Refresh in app:** Pull-to-refresh in Home/Watchlist tabs reloads data
- **Clear cache:** Android Studio → Build → Clean Project

---

## Support

For detailed troubleshooting, see:
- `DATA_LOADING_TROUBLESHOOTING.md`
- `server/SETUP.md`
- Logcat output in Android Studio

Good luck! 🎉

