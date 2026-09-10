package com.invest.easymoney.util

import com.invest.easymoney.domain.model.Stock

object Constants {
    const val BASE_URL = "https://query1.finance.yahoo.com/"

    // Free, keyless OpenAI-compatible AI endpoint (Pollinations.ai)
    const val OPENAI_BASE_URL = "https://text.pollinations.ai/"

    // EasyMoney backend server (auto-detects emulator vs physical device)
    val BACKEND_BASE_URL: String
        get() {
            val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic")
                    || android.os.Build.FINGERPRINT.startsWith("unknown")
                    || android.os.Build.MODEL.contains("google_sdk")
                    || android.os.Build.MODEL.contains("Emulator")
                    || android.os.Build.MODEL.contains("Android SDK built for x86")
                    || android.os.Build.MANUFACTURER.contains("Genymotion")
                    || android.os.Build.PRODUCT.contains("sdk_google")
                    || android.os.Build.PRODUCT.contains("google_sdk")
                    || android.os.Build.PRODUCT.contains("sdk")
                    || android.os.Build.PRODUCT.contains("sdk_x86")
                    || android.os.Build.PRODUCT.contains("vbox86p")
                    || android.os.Build.PRODUCT.contains("emulator")
                    || android.os.Build.PRODUCT.contains("simulator")
            return if (isEmulator) {
                "http://10.0.2.2:8080/"
            } else {
                "http://10.53.215.50:8080/"
            }
        }

    val POPULAR_STOCKS = listOf(
        "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA",
        "NFLX", "AMD", "INTC", "CRM", "ORCL", "ADBE", "JPM", "BAC",
        "WMT", "DIS", "SPOT", "UBER", "PYPL"
    )

    const val IEX_BASE_URL = "https://cloud.iexapis.com/stable/"
    const val IEX_API_TOKEN = "YOUR_IEX_TOKEN"

    val TRENDING_STOCKS = listOf("NVDA", "TSLA", "AAPL", "AMZN", "META")

    const val NOTIFICATION_CHANNEL_ID = "stock_alerts_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Stock Price Alerts"
    const val ALERT_WORK_TAG = "alert_check_work"

    // Mock stock data for development/testing when APIs are unavailable
    val MOCK_STOCKS = listOf(
        Stock(
            symbol = "AAPL",
            name = "Apple Inc.",
            currentPrice = 227.45,
            change = 5.32,
            changePercent = 2.39,
            highPrice = 228.99,
            lowPrice = 221.45,
            openPrice = 223.12,
            previousClose = 222.13,
            marketCap = 3.6e12,
            exchange = "NASDAQ",
            industry = "Technology"
        ),
        Stock(
            symbol = "MSFT",
            name = "Microsoft Corporation",
            currentPrice = 417.82,
            change = 8.65,
            changePercent = 2.12,
            highPrice = 419.99,
            lowPrice = 410.45,
            openPrice = 413.22,
            previousClose = 409.17,
            marketCap = 3.1e12,
            exchange = "NASDAQ",
            industry = "Technology"
        ),
        Stock(
            symbol = "GOOGL",
            name = "Alphabet Inc.",
            currentPrice = 172.45,
            change = 4.23,
            changePercent = 2.51,
            highPrice = 173.99,
            lowPrice = 168.32,
            openPrice = 170.12,
            previousClose = 168.22,
            marketCap = 1.8e12,
            exchange = "NASDAQ",
            industry = "Technology"
        ),
        Stock(
            symbol = "AMZN",
            name = "Amazon.com Inc.",
            currentPrice = 186.32,
            change = 3.45,
            changePercent = 1.88,
            highPrice = 187.99,
            lowPrice = 183.12,
            openPrice = 184.55,
            previousClose = 182.87,
            marketCap = 1.9e12,
            exchange = "NASDAQ",
            industry = "Consumer Cyclical"
        ),
        Stock(
            symbol = "NVDA",
            name = "NVIDIA Corporation",
            currentPrice = 142.67,
            change = 6.23,
            changePercent = 4.55,
            highPrice = 144.99,
            lowPrice = 138.45,
            openPrice = 139.12,
            previousClose = 136.44,
            marketCap = 3.5e11,
            exchange = "NASDAQ",
            industry = "Technology"
        ),
        Stock(
            symbol = "META",
            name = "Meta Platforms Inc.",
            currentPrice = 543.21,
            change = -8.76,
            changePercent = -1.59,
            highPrice = 551.99,
            lowPrice = 542.11,
            openPrice = 550.45,
            previousClose = 551.97,
            marketCap = 1.4e12,
            exchange = "NASDAQ",
            industry = "Technology"
        ),
        Stock(
            symbol = "TSLA",
            name = "Tesla Inc.",
            currentPrice = 272.45,
            change = -12.33,
            changePercent = -4.33,
            highPrice = 285.99,
            lowPrice = 271.12,
            openPrice = 283.22,
            previousClose = 284.78,
            marketCap = 8.6e11,
            exchange = "NASDAQ",
            industry = "Automotive"
        ),
        Stock(
            symbol = "NFLX",
            name = "Netflix Inc.",
            currentPrice = 321.45,
            change = 5.67,
            changePercent = 1.79,
            highPrice = 323.99,
            lowPrice = 318.12,
            openPrice = 319.33,
            previousClose = 315.78,
            marketCap = 1.4e11,
            exchange = "NASDAQ",
            industry = "Media"
        ),
        Stock(
            symbol = "AMD",
            name = "Advanced Micro Devices Inc.",
            currentPrice = 187.23,
            change = 8.45,
            changePercent = 4.72,
            highPrice = 188.99,
            lowPrice = 181.45,
            openPrice = 183.12,
            previousClose = 178.78,
            marketCap = 1.9e11,
            exchange = "NASDAQ",
            industry = "Semiconductors"
        ),
        Stock(
            symbol = "INTC",
            name = "Intel Corporation",
            currentPrice = 32.45,
            change = -1.23,
            changePercent = -3.66,
            highPrice = 33.99,
            lowPrice = 32.12,
            openPrice = 33.67,
            previousClose = 33.68,
            marketCap = 1.3e11,
            exchange = "NASDAQ",
            industry = "Semiconductors"
        )
    )
}
