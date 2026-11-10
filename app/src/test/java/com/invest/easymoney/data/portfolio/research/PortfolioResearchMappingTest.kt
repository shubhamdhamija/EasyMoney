package com.invest.easymoney.data.portfolio.research

import com.google.gson.Gson
import com.invest.easymoney.data.api.dto.PortfolioResearchReportDto
import com.invest.easymoney.data.api.dto.ResearchConcentrationDto
import com.invest.easymoney.data.api.dto.ResearchCurrencySummaryDto
import com.invest.easymoney.data.api.dto.ResearchWarningDto
import com.invest.easymoney.data.api.dto.ResearchWeightedDto
import com.invest.easymoney.domain.portfolio.Broker
import com.invest.easymoney.domain.portfolio.CurrencyCode
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.PortfolioAccount
import com.invest.easymoney.domain.portfolio.PortfolioAccountType
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import com.invest.easymoney.domain.portfolio.PositionDirection
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchException
import com.invest.easymoney.domain.portfolio.research.ResearchDataCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class PortfolioResearchMappingTest {

    private val now = Instant.parse("2026-11-09T12:00:00Z")

    private fun account() = PortfolioAccount(
        accountId = "a1", snapshotId = "s1", broker = Broker.WEALTHSIMPLE, accountName = "My TFSA 123456789",
        accountType = PortfolioAccountType.TFSA, accountClassification = null,
        maskedAccountNumber = "****6789", accountNumberHash = "HASHVALUE", baseCurrency = CurrencyCode("CAD"), effectiveAt = now
    )

    private fun position(i: Int, value: String?, currency: String? = "CAD", symbol: String? = "S$i", book: String? = null) =
        PortfolioPosition(
            positionId = "p$i", snapshotId = "s1", accountId = "a1", symbol = symbol, exchange = null, mic = null,
            securityName = "Security $i", securityType = PortfolioSecurityType.EQUITY, quantity = BigDecimal("2"),
            direction = PositionDirection.LONG, marketPrice = null, marketPriceCurrency = null,
            bookValueCad = book?.let(::BigDecimal), bookValueCadCurrency = book?.let { CurrencyCode("CAD") },
            bookValueMarket = null, bookValueMarketCurrency = null,
            marketValue = value?.let(::BigDecimal), marketValueCurrency = currency?.let(::CodeOf),
            unrealizedReturn = null, unrealizedReturnCurrency = null, effectiveAt = now, warnings = emptyList()
        )

    private fun CodeOf(c: String) = CurrencyCode(c)

    private fun portfolio(positions: List<PortfolioPosition>) =
        Portfolio(emptyList(), listOf(account()), positions, emptyList())

    @Test fun localContextCapsAtFiftyLargestPositions() {
        val positions = (1..80).map { position(it, it.toString()) }
        val dto = LocalContextMapper.map(portfolio(positions), setOf(ResearchDataCategory.POSITIONS))!!
        assertEquals(50, dto.positions.size)
        assertEquals("80", dto.positions.first().marketValue)
    }

    @Test fun localContextContainsNoAccountIdentifiersOrNames() {
        val json = Gson().toJson(LocalContextMapper.map(portfolio(listOf(position(1, "10"))), emptySet()))
        for (forbidden in listOf("HASHVALUE", "6789", "123456789", "My TFSA", "a1", "s1", "p1", "accountNumber")) {
            assertFalse("leaked $forbidden", json.contains(forbidden))
        }
        assertTrue(json.contains("TFSA")) // account TYPE only
    }

    @Test fun bookValuesAreSentOnlyWithConsent() {
        val p = portfolio(listOf(position(1, "10", book = "8")))
        assertNull(LocalContextMapper.map(p, setOf(ResearchDataCategory.POSITIONS))!!.positions[0].bookValue)
        assertEquals("8", LocalContextMapper.map(p, setOf(ResearchDataCategory.BOOK_VALUES))!!.positions[0].bookValue)
    }

    @Test fun unusablePositionsAreDroppedAndEmptyReturnsNull() {
        assertNull(LocalContextMapper.map(portfolio(listOf(position(1, "10", currency = null))), emptySet()))
        val dto = LocalContextMapper.map(portfolio(listOf(position(1, null), position(2, "5", symbol = "bad sym!"))), emptySet())!!
        assertEquals(2, dto.positions.size)
        assertNull(dto.positions.first { it.securityName == "Security 1" }.marketValue) // missing stays null, never zero
        assertEquals("BADSYM", dto.positions.first { it.securityName == "Security 2" }.symbol)
    }

    @Test fun reportMapsDeterministicFieldsAndKeepsCurrenciesSeparate() {
        val dto = PortfolioResearchReportDto(
            status = "completed", summary = "Summary", errorCode = null, evidenceSummary = listOf("Evidence"),
            portfolioSource = "SNAPTRADE_SANDBOX", snapshotAt = "2026-11-09T12:00:00Z",
            currencySummaries = listOf(
                ResearchCurrencySummaryDto("USD", "1000.00", null, null, null, 3),
                ResearchCurrencySummaryDto("CAD", "50.00", "10.00", null, null, 1)
            ),
            positions = emptyList(),
            concentration = listOf(
                ResearchConcentrationDto("USD", "1000.00", 1, ResearchWeightedDto("AAA", "AAA Corp", "750.00", "75.0"),
                    emptyList(), null, null)
            ),
            warnings = listOf(ResearchWarningDto("MIXED_CURRENCIES", null, null), ResearchWarningDto("UNKNOWN_CODE", null, null)),
            disclaimer = "x", requestId = "r1"
        )
        val report = ResearchReportMapper.map(dto)
        assertEquals(listOf("USD", "CAD"), report.currencySummaries.map { it.currency })
        assertNull(report.currencySummaries[0].cashValue)
        assertEquals("75.0", report.concentration[0].largestPosition!!.weightPercent)
        assertEquals(1, report.warnings.size) // unknown codes are never shown raw
        assertEquals("No trades or account changes were made.", report.statement)
        assertNotNull(report.snapshotAt)
    }

    @Test fun failedReportBecomesSafeError() {
        val dto = PortfolioResearchReportDto("failed", "x", "INVALID_TOOL_ARGUMENTS", null, null, null, null, null, null, null, null, null)
        try {
            ResearchReportMapper.map(dto)
            fail("expected exception")
        } catch (e: PortfolioResearchException) {
            assertFalse(e.message.contains("INVALID_TOOL_ARGUMENTS"))
        }
    }

    @Test fun errorCodesMapToSafeMessagesAndConsentRetry() {
        val gson = Gson()
        val expired = ResearchErrors.fromHttp(403, """{"error":{"code":"PORTFOLIO_CONSENT_EXPIRED","message":"raw"},"request_id":"r"}""", gson)
        assertTrue(expired.consentRequired)
        assertEquals("r", expired.requestId)
        val notFound = ResearchErrors.fromHttp(404, """{"error":{"code":"PORTFOLIO_ACCESS_DENIED"}}""", gson)
        assertEquals("This portfolio is no longer available.", notFound.message)
        assertFalse(notFound.retryable)
        assertEquals("Sign in again to research your portfolio.", ResearchErrors.fromHttp(401, "garbage", gson).message)
        assertTrue(ResearchErrors.fromHttp(503, null, gson).retryable)
    }
}
