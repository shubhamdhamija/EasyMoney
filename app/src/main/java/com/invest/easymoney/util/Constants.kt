package com.invest.easymoney.util

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
}
