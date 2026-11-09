package com.invest.easymoney.data.api.dto

/** Request body for `POST /ai/stock-insight` (see docs/AI_STOCK_INSIGHT.md). */
data class StockInsightRequest(
    val symbol: String,
    val companyName: String?,
    val currentPrice: Double?,
    val priceChangePercent: Double,
    val marketChangePercent: Double?,
    val dayLow: Double?,
    val dayHigh: Double?,
    val evidence: List<StockEvidenceRequest>
)

data class StockInsightApiResponse(
    val ok: Boolean,
    val insight: StockInsightResponse? = null,
    val requestId: String? = null
)

data class StockInsightResponse(
    val symbol: String,
    val direction: String,
    val evidenceSentiment: String,
    val summary: String,
    val shortTermContext: String,
    val longTermContext: String,
    val factors: List<String>,
    val evidenceSufficient: Boolean,
    val disclaimer: String
)

