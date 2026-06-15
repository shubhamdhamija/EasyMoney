package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName


data class IexStockDto(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("companyName") val companyName: String = "",
    @SerializedName("latestPrice") val latestPrice: Double = 0.0,
    @SerializedName("latestChange") val latestChange: Double = 0.0,
    @SerializedName("latestChangePercent") val latestChangePercent: Double = 0.0
)