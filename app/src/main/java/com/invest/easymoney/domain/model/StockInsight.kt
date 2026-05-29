package com.invest.easymoney.domain.model

data class StockInsight(
    val symbol: String,
    /** "bullish", "bearish", or "neutral" */
    val sentiment: String,
    val insight: String,
    val shortTermOutlook: String,
    val longTermOutlook: String
)
