package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

/** Top-level wrapper returned by /v1/finance/search (news only) */
data class YahooSearchResponseDto(
    @SerializedName("news") val news: List<YahooNewsItemDto>? = emptyList()
)
