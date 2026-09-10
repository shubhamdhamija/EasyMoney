package com.invest.easymoney.domain.model

data class EarningsReport(
    val symbol: String,
    val date: String,
    val epsEstimate: Double?,
    val epsActual: Double?,
    val revenueEstimate: Double?,
    val revenueActual: Double?,
    val surprise: Double? = null,
    val surprisePercent: Double? = null,
    val quarters: List<EarningsQuarter> = emptyList()
)

