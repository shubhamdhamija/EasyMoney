#!/bin/bash

# Kill any existing node processes on port 8080
echo "🔄 Stopping any existing server..."
pkill -f "node index.js" 2>/dev/null
sleep 1

# Navigate to server directory
cd /Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/server

# Start the server
echo "🚀 Starting EasyMoney Backend Server..."
npm start &
SERVER_PID=$!

# Give it time to start
sleep 3

# Check if server is running
if ps -p $SERVER_PID > /dev/null; then
    echo "✅ Server is running (PID: $SERVER_PID)"

    # Test the endpoint
    echo "📡 Testing backend endpoint..."
    RESPONSE=$(curl -s http://localhost:8080/ 2>&1)
    if [ $? -eq 0 ]; then
        echo "✅ Backend is responding: $RESPONSE"
        echo ""
        echo "🎯 Backend URL for emulator: http://10.0.2.2:8080/"
        echo "🎯 Backend URL for physical device: http://YOUR_MAC_IP:8080/"
        echo ""
        echo "To find your Mac IP:"
        echo "  ifconfig | grep \"inet \" | grep -v 127.0.0.1"
    else
        echo "⚠️  Backend test failed"
    fi
else
    echo "❌ Failed to start server"
    exit 1
fi

# Keep the script running
wait $SERVER_PID

