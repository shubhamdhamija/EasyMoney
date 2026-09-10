#!/bin/bash

# EasyMoney Backend Server Startup Script

echo "🚀 EasyMoney Backend Server Startup"
echo "===================================="
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed!"
    echo ""
    echo "Install Node.js:"
    echo "1. Using Homebrew (macOS):"
    echo "   brew install node"
    echo ""
    echo "2. Or download from https://nodejs.org/"
    echo ""
    exit 1
fi

echo "✅ Node.js version: $(node --version)"
echo ""

# Check if npm dependencies are installed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing npm dependencies..."
    npm install
    echo ""
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "⚠️  .env file not found!"
    echo "📝 Copy .env.example to .env and configure your API keys:"
    echo "   cp .env.example .env"
    echo ""
    echo "You need to set at least:"
    echo "  - FINNHUB_API_KEY (free tier at https://finnhub.io)"
    echo ""
fi

echo "🔧 Starting server on http://localhost:8080"
echo ""
npm start

