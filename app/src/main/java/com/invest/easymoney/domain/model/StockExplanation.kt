package com.invest.easymoney.domain.model

data class StockExplanation(
    val symbol: String,
    val direction: MovementDirection,
    val evidenceSentiment: EvidenceSentiment,
    val summary: String,
    val factors: List<String>,
    val evidenceSufficient: Boolean,
    val disclaimer: String,
    val requestId: String?
)

enum class MovementDirection {
    INCREASE,
    DECREASE,
    UNCHANGED
}

enum class EvidenceSentiment {
    POSITIVE,
    NEGATIVE,
    MIXED,
    NEUTRAL,
    INSUFFICIENT
}

