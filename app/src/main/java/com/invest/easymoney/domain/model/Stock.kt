package com.invest.easymoney.domain.model

data class Stock(
    val symbol: String,
    val name: String = "",
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val highPrice: Double = 0.0,
    val lowPrice: Double = 0.0,
    val openPrice: Double = 0.0,
    val previousClose: Double = 0.0,
    val marketCap: Double = 0.0,
    val logoUrl: String = "",
    val exchange: String = "",
    val industry: String = ""
)
