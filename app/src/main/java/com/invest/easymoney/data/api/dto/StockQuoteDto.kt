package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

/** Top-level wrapper returned by /v8/finance/chart/{symbol} */
data class YahooChartResponseDto(
    @SerializedName("chart") val chart: ChartBody?
)

data class ChartBody(
    @SerializedName("result") val result: List<ChartResult>? = emptyList(),
    @SerializedName("error") val error: Any? = null
)

data class ChartResult(
    @SerializedName("meta") val meta: ChartMeta = ChartMeta()
)

data class ChartMeta(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("shortName") val shortName: String = "",
    @SerializedName("longName") val longName: String = "",
    @SerializedName("regularMarketPrice") val regularMarketPrice: Double = 0.0,
    @SerializedName("chartPreviousClose") val chartPreviousClose: Double = 0.0,
    @SerializedName("previousClose") val previousClose: Double = 0.0,
    @SerializedName("regularMarketDayHigh") val dayHigh: Double = 0.0,
    @SerializedName("regularMarketDayLow") val dayLow: Double = 0.0,
    @SerializedName("fullExchangeName") val exchange: String = "",
    @SerializedName("currency") val currency: String = ""
)
