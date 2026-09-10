package com.invest.easymoney.domain.model

data class UpcomingEarning(
    val symbol: String,
    val companyName: String,
    val earningsDate: String,
    val fiscalPeriod: String? = null,
    val fiscalEndDate: String? = null,
    val date: String = earningsDate,
    val time: String? = null,
    val epsEstimate: Double? = null
)

