package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

data class EarningsReportDto(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("epsEstimate")
    val epsEstimate: Double? = null,
    @SerializedName("epsActual")
    val epsActual: Double? = null,
    @SerializedName("revenueEstimate")
    val revenueEstimate: Double? = null,
    @SerializedName("revenueActual")
    val revenueActual: Double? = null,
    @SerializedName("surprise")
    val surprise: Double? = null,
    @SerializedName("surprisePercent")
    val surprisePercent: Double? = null
)

data class UpcomingEarningDto(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("companyName")
    val companyName: String,
    @SerializedName("earningsDate")
    val earningsDate: String,
    @SerializedName("fiscalPeriod")
    val fiscalPeriod: String? = null,
    @SerializedName("fiscalEndDate")
    val fiscalEndDate: String? = null
)

