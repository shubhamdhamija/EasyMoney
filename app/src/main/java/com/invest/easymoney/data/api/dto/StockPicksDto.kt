package com.invest.easymoney.data.api.dto

/** Request body for `POST /ai/picks` (see docs/AI_PICKS.md). */
data class StockPicksRequest(
    val stocks: List<StockPickInput>,
    val maximumPicks: Int
)

data class StockPickInput(
    val symbol: String,
    val companyName: String?,
    val currentPrice: Double?,
    val priceChangePercent: Double,
    val dayLow: Double?,
    val dayHigh: Double?
)

data class StockPicksApiResponse(
    val ok: Boolean,
    val picks: List<StockPickResponse>? = null,
    val disclaimer: String? = null,
    val requestId: String? = null
)

data class StockPickResponse(
    val symbol: String,
    val reason: String,
    val rank: Int
)
