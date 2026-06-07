package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

/** Top-level wrapper returned by /v1/finance/search (news + quotes) */
// Renamed to avoid duplicate class name collisions during build
data class YahooSearchResultDto(
    @SerializedName("news") val news: List<YahooNewsItemDto>? = emptyList(),
    @SerializedName("quotes") val quotes: List<YahooQuoteDto>? = emptyList()
)

/** Minimal quote item returned by the search endpoint */
data class YahooQuoteDto(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("shortname") val shortName: String = "",
    @SerializedName("longname") val longName: String = "",
    @SerializedName("exchange") val exchange: String = "",
    @SerializedName("quoteType") val quoteType: String = ""
)
