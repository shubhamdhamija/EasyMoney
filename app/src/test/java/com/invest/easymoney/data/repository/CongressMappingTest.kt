package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.dto.CongressFilingDetailDto
import com.invest.easymoney.data.api.dto.CongressFilingDto
import com.invest.easymoney.data.api.dto.CongressTradeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CongressMappingTest {
    private val filing = CongressFilingDto("123", "Hon. Nancy Pelosi", "CA11", 2026, "2026-08-21", "https://x/123.pdf").toDomain()!!

    @Test
    fun filingWithoutIdOrYearIsDropped() {
        assertNull(CongressFilingDto(docId = null, year = 2026).toDomain())
        assertNull(CongressFilingDto(docId = "1", year = null).toDomain())
    }

    @Test
    fun tradeMapsPurchaseAndLateFlag() {
        val trade = CongressTradeDto(
            ownerName = "spouse", type = "P", typeName = "purchase", asset = "NVIDIA", ticker = "NVDA",
            amount = "$1,000,001 - $5,000,000", date = "2026-07-01", daysToDisclose = 51, late = true
        ).toDomain()!!
        assertTrue(trade.isPurchase)
        assertTrue(trade.late)
        assertEquals("NVDA", trade.ticker)
    }

    @Test
    fun detailWithoutTradesIsUnavailableAndFallsBackToFilingPdf() {
        val detail = CongressFilingDetailDto(available = true, trades = emptyList()).toDomain(filing)
        assertFalse(detail.available)
        assertEquals("https://x/123.pdf", detail.pdfUrl)
    }
}

