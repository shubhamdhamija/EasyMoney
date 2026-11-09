package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

data class CongressFilingsResponseDto(
    @SerializedName("filings") val filings: List<CongressFilingDto>? = null,
    @SerializedName("pdf_text_available") val pdfTextAvailable: Boolean? = null,
)

data class CongressFilingDto(
    @SerializedName("doc_id") val docId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("district") val district: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("filed") val filed: String? = null,
    @SerializedName("url") val url: String? = null,
)

data class CongressFilingDetailDto(
    @SerializedName("available") val available: Boolean? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null,
    @SerializedName("filed") val filed: String? = null,
    @SerializedName("trades") val trades: List<CongressTradeDto>? = null,
)

data class CongressTradeDto(
    @SerializedName("owner_name") val ownerName: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("type_name") val typeName: String? = null,
    @SerializedName("asset") val asset: String? = null,
    @SerializedName("ticker") val ticker: String? = null,
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("days_to_disclose") val daysToDisclose: Int? = null,
    @SerializedName("late") val late: Boolean? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("shares") val shares: Int? = null,
    @SerializedName("option") val option: CongressOptionDto? = null,
)

data class CongressOptionDto(
    @SerializedName("contracts") val contracts: Int? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("strike") val strike: Double? = null,
    @SerializedName("expires") val expires: String? = null,
)

