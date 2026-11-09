package com.invest.easymoney.ui.detail

import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.StockExplanation
import org.junit.Assert.assertEquals
import org.junit.Test

class ExplainMoveLabelsTest {

    @Test
    fun `every direction has a label`() {
        assertEquals("Stock increased", MovementDirection.INCREASE.toDisplayLabel())
        assertEquals("Stock decreased", MovementDirection.DECREASE.toDisplayLabel())
        assertEquals("Stock unchanged", MovementDirection.UNCHANGED.toDisplayLabel())
    }

    @Test
    fun `every sentiment has a label`() {
        assertEquals("Positive evidence", EvidenceSentiment.POSITIVE.toDisplayLabel())
        assertEquals("Negative evidence", EvidenceSentiment.NEGATIVE.toDisplayLabel())
        assertEquals("Mixed evidence", EvidenceSentiment.MIXED.toDisplayLabel())
        assertEquals("Neutral evidence", EvidenceSentiment.NEUTRAL.toDisplayLabel())
        assertEquals("Insufficient evidence", EvidenceSentiment.INSUFFICIENT.toDisplayLabel())
    }

    @Test
    fun `headline combines sentiment and direction`() {
        val explanation = StockExplanation(
            symbol = "AAPL",
            direction = MovementDirection.INCREASE,
            evidenceSentiment = EvidenceSentiment.POSITIVE,
            summary = "Summary",
            factors = emptyList(),
            evidenceSufficient = true,
            disclaimer = "For informational purposes only.",
            requestId = null
        )

        assertEquals("Positive evidence • Stock increased", explanation.headline())
    }

    @Test
    fun `headline never exposes raw enum names or backend metadata`() {
        val explanation = StockExplanation(
            symbol = "TSLA",
            direction = MovementDirection.DECREASE,
            evidenceSentiment = EvidenceSentiment.INSUFFICIENT,
            summary = "Summary",
            factors = emptyList(),
            evidenceSufficient = false,
            disclaimer = "For informational purposes only.",
            requestId = "req-1"
        )

        val headline = explanation.headline()

        assertEquals("Insufficient evidence • Stock decreased", headline)
        assertEquals(false, headline.contains("INSUFFICIENT"))
        assertEquals(false, headline.contains("req-1"))
    }
}

