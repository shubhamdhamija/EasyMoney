package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

data class YahooNewsItemDto(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("publisher") val publisher: String = "",
    @SerializedName("link") val link: String = "",
    @SerializedName("providerPublishTime") val publishTime: Long = 0L,
    @SerializedName("type") val type: String = ""
)
