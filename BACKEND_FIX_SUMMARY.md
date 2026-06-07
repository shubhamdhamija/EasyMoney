# EasyMoney - Backend Server Fix Summary

## ✅ WHAT I FIXED

### 1. **Backend Server is Now Available**
- Located at: `/Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/server/`
- Running on: **Port 8080**
- URL from Android Emulator: `http://10.0.2.2:8080/`
- URL from Physical Device: `http://YOUR_MAC_IP:8080/`

### 2. **Improved Error Logging**
- Added detailed HTTP logging to see request/response bodies
- Added enhanced error messages with debugging info
- Now logs full exception stack traces to help diagnose issues

### 3. **Created Startup Script**
- File: `/Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/start-backend.sh`
- Automatically handles port conflicts
- Tests backend connectivity on startup

## 🚀 HOW TO USE THE BACKEND

### **To Start the Backend Server**
```bash
/Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/start-backend.sh
```

Or manually:
```bash
cd /Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/server
npm start
```

### **Available Endpoints**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/` | Health check |
| GET | `/ai/portfolio?risk=medium` | Generate portfolio recommendations |
| GET | `/ai/sectors` | Get sector analysis |
| POST | `/ai/signal` | Analyze stock signals |
| GET | `/ai/explain/:symbol` | Explain why a stock moved |

### **Testing from Terminal**
```bash
# Health check
curl http://localhost:8080/

# Generate portfolio
curl "http://localhost:8080/ai/portfolio?risk=medium"

# Get sectors
curl http://localhost:8080/ai/sectors
```

## 🔍 DEBUGGING YOUR CONNECTION ISSUE

### **If you still see "backend server not reachable":**

#### **Step 1: Verify Server is Running**
```bash
ps aux | grep "node index.js"
```
You should see: `node index.js` in the output

#### **Step 2: Test Backend**
```bash
curl http://localhost:8080/ai/portfolio?risk=medium
```
Expected response: JSON with portfolio data

#### **Step 3: Check Android Logs**
In Android Studio Logcat, filter by:
- `BackendAPI` - Shows HTTP requests/responses
- `BackendRepository` - Shows error details

#### **Step 4: If Using Physical Device**
Find your Mac's IP:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```
Look for something like `192.168.x.x` or `10.0.x.x`

Then update `Constants.kt`:
```kotlin
const val BACKEND_BASE_URL = "http://192.168.x.x:8080/"  // Replace with your IP
```

## 📋 WHAT HAPPENS WHEN YOU CLICK "Generate Portfolio"

1. **Android App** → Sends request to `http://10.0.2.2:8080/ai/portfolio`
2. **Backend Server** → Processes request, generates portfolio
3. **Backend** → Returns JSON with recommendations
4. **Android App** → Displays portfolio with symbols and rationale

## 🐛 COMMON ISSUES & SOLUTIONS

### **Issue: "Backend server not reachable"**
**Check:**
- Is the server running? (`ps aux | grep node`)
- Is the server on port 8080? (`lsof -i :8080`)
- Can you reach it from terminal? (`curl http://localhost:8080/`)

**Fix:**
```bash
# Kill old process and restart
pkill -f "node index.js"
sleep 2
/Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/start-backend.sh
```

### **Issue: Connection refused**
**Solution:** Make sure port 8080 is not blocked by firewall
```bash
# Force kill on port 8080
lsof -ti:8080 | xargs kill -9
sleep 1
npm start
```

### **Issue: Timeout**
**Solution:** Server is too slow, increase timeout in `BackendModule.kt`
Currently set to 60 seconds - should be enough

## 📱 REBUILDING & RETESTING

After the backend is running:

1. **In Android Studio:**
   - Build → Rebuild Project
   - Run → Run 'app'

2. **In the App:**
   - Navigate to "AI Hub" tab
   - Click "Generate Portfolio"
   - Check for the response (should show 5 stock symbols)

3. **If still failing:**
   - Open Logcat
   - Filter by "BackendAPI"
   - Look for error details
   - Share the error with detailed logs

## 📊 BACKEND HEALTH CHECK

Run this command to get a full status:
```bash
echo "=== Backend Status ===" && \
curl -s http://localhost:8080/ && echo "" && \
echo "=== Portfolio Test ===" && \
curl -s http://localhost:8080/ai/portfolio?risk=medium | head -c 100 && echo ""
```

## 🎯 NEXT STEPS

1. **Start Backend**: Run the startup script
2. **Rebuild App**: Do a clean rebuild in Android Studio
3. **Test**: Click "Generate Portfolio" in the AI Hub
4. **Debug if needed**: Check logcat for "BackendAPI" or "BackendRepository" logs

---

**Created:** June 2, 2026  
**Last Updated:** June 2, 2026

