package com.invest.easymoney.util

object Constants {
    const val BASE_URL = "https://query1.finance.yahoo.com/"

    // Free, keyless OpenAI-compatible AI endpoint (Pollinations.ai)
    const val OPENAI_BASE_URL = "https://text.pollinations.ai/"

    val POPULAR_STOCKS = listOf(
        "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA",
        "NFLX", "AMD", "INTC", "CRM", "ORCL", "ADBE", "JPM", "BAC",
        "WMT", "DIS", "SPOT", "UBER", "PYPL"
    )

    val TRENDING_STOCKS = listOf("NVDA", "TSLA", "AAPL", "AMZN", "META")

    const val NOTIFICATION_CHANNEL_ID = "stock_alerts_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Stock Price Alerts"
    const val ALERT_WORK_TAG = "alert_check_work"
}
