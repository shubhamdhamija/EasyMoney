package com.invest.easymoney.data.api.dto

data class StockExplanationRequest(
    val symbol: String,
    val companyName: String?,
    val priceChangePercent: Double,
    val marketChangePercent: Double?,
    val dayLow: Double?,
    val dayHigh: Double?,
    val evidence: List<StockEvidenceRequest>
)

data class StockEvidenceRequest(
    val type: String,
    val headline: String,
    val source: String?
)

data class StockExplanationApiResponse(
    val ok: Boolean,
    val explanation: StockExplanationResponse? = null,
    val metadata: StockExplanationMetadata? = null,
    val requestId: String? = null,
    val error: StockExplanationError? = null
)

data class StockExplanationResponse(
    val symbol: String,
    val direction: String,
    val evidenceSentiment: String,
    val summary: String,
    val factors: List<String>,
    val evidenceSufficient: Boolean,
    val disclaimer: String
)

data class StockExplanationMetadata(
    val promptVersion: String,
    val providerCalled: Boolean
)

data class StockExplanationErrorResponse(
    val ok: Boolean = false,
    val error: StockExplanationError? = null,
    val requestId: String? = null
)

data class StockExplanationError(
    val code: String? = null,
    val message: String? = null
)

