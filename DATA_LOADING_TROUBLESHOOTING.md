# EasyMoney Data Loading Troubleshooting Guide

## Problem: App showing no stock data

### Root Causes & Solutions

#### 1. **Server Not Running**
The backend server handles AI insights, earnings, and community features. While stock data comes from Yahoo Finance (direct), the app needs the backend for full features.

**Check:**
```bash
# Is the server running on port 8080?
curl http://localhost:8080/
# Should respond: "EasyMoney AI Server"
```

**Fix:**
```bash
cd server
npm install
node index.js
```

#### 2. **No Internet Access on App**
The app connects to:
- **Yahoo Finance API** (https://query1.finance.yahoo.com) - PRIMARY DATA SOURCE
- **IEX Cloud API** (https://cloud.iexapis.com) - Trending stocks
- **Backend Server** (http://10.0.2.2:8080) - AI features

**Check:**
- Emulator has internet access enabled
- Physical device has WiFi/mobile data
- Firewall isn't blocking connections

#### 3. **API Rate Limiting**
Yahoo Finance might block rapid requests from the same IP.

**Check in logcat:**
```
grep -i "403\|429\|rate" logcat.log
```

**Fix:** Wait a few minutes, or use a VPN to change IP

#### 4. **Backend URL Configuration Wrong**

The app auto-detects:
- **Emulator:** http://10.0.2.2:8080
- **Physical Device:** http://10.53.215.50:8080 (configured in Constants.kt)

For your local development machine, update `Constants.BACKEND_BASE_URL`:

**File:** `app/src/main/java/com/invest/easymoney/util/Constants.kt`

```kotlin
val BACKEND_BASE_URL: String
    get() = "http://YOUR_MACHINE_IP:8080/" // e.g., 192.168.1.100
```

#### 5. **API Keys Not Configured**
While Yahoo Finance is free, IEX API needs a token.

**Check:** `app/src/main/java/com/invest/easymoney/util/Constants.kt`
```kotlin
const val IEX_API_TOKEN = "YOUR_IEX_TOKEN"
```

**Get free token:** https://iexcloud.io (free tier available)

---

## Debugging Checklist

### On Android Device/Emulator:
1. Open **Settings → Apps → EasyMoney → Permissions**
   - Ensure **Internet** permission is granted

2. Open **Logcat** in Android Studio
   - Filter by: `com.invest.easymoney`
   - Look for `Network`, `Api`, `Error` keywords
   - Check for stack traces

3. Common error messages:
   - `Unresolved reference` → Missing API service
   - `Connect timeout` → No internet or server down
   - `HttpException` → API returned error
   - `JSON parse exception` → API response format mismatch

### On Development Machine:
1. **Test Yahoo Finance API directly:**
   ```bash
   curl "https://query1.finance.yahoo.com/v8/finance/chart/AAPL?range=1d&interval=1m"
   ```
   Should return JSON data

2. **Test Backend Server:**
   ```bash
   curl http://localhost:8080/
   ```

3. **Check server logs:**
   Server console shows all requests and errors

### From App Source Code:
- Stock data fetch: `StockRepositoryImpl.getTopStocks()`
- News fetch: `StockRepositoryImpl.getNews()`
- Backend calls: `BackendRepositoryImpl` methods
- All use Retrofit + OkHttp logging (enabled in NetworkModule)

---

## Quick Start Summary

**To get data flowing:**

1. **Start backend server:**
   ```bash
   cd server
   npm install
   node index.js
   ```

2. **Update backend URL in app (if needed):**
   - Edit `Constants.BACKEND_BASE_URL`
   - Rebuild & run app

3. **Ensure emulator/device has internet:**
   - Test: Can emulator open `https://google.com`?

4. **Check Logcat for errors:**
   - Filter for network/API errors
   - Screenshot any errors

5. **Restart the app:**
   ```bash
   adb logcat -c
   adb shell am force-stop com.invest.easymoney
   # Then open app normally
   ```

---

## If Still Not Working

1. Capture full logcat:
   ```bash
   adb logcat > debug.log
   # Wait 10 seconds, then Ctrl+C
   cat debug.log
   ```

2. Check if backend is responding:
   ```bash
   curl -v http://localhost:8080/
   ```

3. Verify app is making requests:
   - Look for `OkHttp` logs in logcat
   - Should see request URLs and responses

4. Check network calls in Charles/Burp proxy
   - Intercept app traffic
   - Verify URLs and response codes

