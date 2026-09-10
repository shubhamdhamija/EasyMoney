package com.invest.easymoney.domain.model

data class EarningsQuarter(
    val period: String,
    val epsActual: Double?,
    val epsEstimate: Double?
)

