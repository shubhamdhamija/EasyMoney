package com.invest.easymoney.ui.detail

import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.StockExplanation

/** User-facing wording for a movement direction. */
fun MovementDirection.toDisplayLabel(): String = when (this) {
    MovementDirection.INCREASE -> "Stock increased"
    MovementDirection.DECREASE -> "Stock decreased"
    MovementDirection.UNCHANGED -> "Stock unchanged"
}

/** User-facing wording for the evidence sentiment. */
fun EvidenceSentiment.toDisplayLabel(): String = when (this) {
    EvidenceSentiment.POSITIVE -> "Positive evidence"
    EvidenceSentiment.NEGATIVE -> "Negative evidence"
    EvidenceSentiment.MIXED -> "Mixed evidence"
    EvidenceSentiment.NEUTRAL -> "Neutral evidence"
    EvidenceSentiment.INSUFFICIENT -> "Insufficient evidence"
}

/** One-line header, for example "Positive evidence • Stock increased". */
fun StockExplanation.headline(): String =
    "${evidenceSentiment.toDisplayLabel()} • ${direction.toDisplayLabel()}"

