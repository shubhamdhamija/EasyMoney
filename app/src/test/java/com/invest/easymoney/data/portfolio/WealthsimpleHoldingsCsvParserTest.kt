package com.invest.easymoney.data.portfolio

import com.invest.easymoney.domain.portfolio.Broker
import com.invest.easymoney.domain.portfolio.PortfolioImportException
import com.invest.easymoney.domain.portfolio.PortfolioImportWarningCode
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import com.invest.easymoney.domain.portfolio.PortfolioSummaryCalculator
import com.invest.easymoney.domain.portfolio.hashAccountNumber
import com.invest.easymoney.domain.portfolio.maskAccountNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class WealthsimpleHoldingsCsvParserTest {

    private val header = WealthsimpleHoldingsCsvParser.REQUIRED_HEADERS.joinToString(",")
    private val now = Instant.parse("2026-09-10T00:00:00Z")

    private val sample = "\uFEFF$header\r\n" +
        "My TFSA,TFSA,Non-registered,ACC123456789,AAPL,NASDAQ,XNAS,\"Apple, Inc.\",EQUITY,1.23456789,LONG,200.10,USD,150.00,CAD,110.00,USD,247.04,USD,10.5,USD\r\n" +
        "My TFSA,TFSA,Non-registered,ACC123456789,WCN.TO,TSX,XTSE,Waste Connections,EQUITY,2,LONG,,CAD,100,CAD,100,CAD,0,CAD,,CAD\r\n" +
        "My TFSA,TFSA,Non-registered,ACC123456789,XEQT,TSX,XTSE,iShares ETF,EXCHANGE\\_TRADED\\_FUND,3,LONG,30,CAD,80,CAD,80,CAD,90,CAD,10,CAD\r\n" +
        "My TFSA,TFSA,Non-registered,ACC123456789,CAD,,,Canadian dollar,CURRENCY,50.25,LONG,,,,,,,50.25,CAD,,\r\n" +
        "\r\n" +
        "As of 2026-09-09 10:26 GMT-04:00\r\n"

    private fun parse(text: String) =
        WealthsimpleHoldingsCsvParser().parse(text.byteInputStream(), "file.csv", now)

    @Test
    fun parsesAccountsPositionsCashAndTimestamp() {
        val r = parse(sample)
        assertEquals(1, r.accounts.size)
        assertEquals(3, r.positions.size)
        assertEquals(1, r.cashBalances.size)
        assertEquals(Instant.parse("2026-09-09T14:26:00Z"), r.effectiveAt)
        assertEquals(BigDecimal("1.23456789"), r.positions[0].quantity)
        assertEquals("Apple, Inc.", r.positions[0].securityName)
        assertEquals(PortfolioSecurityType.EXCHANGE_TRADED_FUND, r.positions[2].securityType)
        assertEquals("XTSE", r.positions[1].mic)
        assertTrue(PortfolioImportWarningCode.MISSING_MARKET_PRICE in r.warnings)
        assertTrue(PortfolioImportWarningCode.ZERO_MARKET_VALUE in r.warnings)
        assertTrue(r.rejectedRows.isEmpty())
    }

    @Test
    fun accountNumberIsMaskedAndNeverRetained() {
        val r = parse(sample)
        val account = r.accounts.single()
        assertEquals("••••6789", account.maskedAccountNumber)
        assertEquals(hashAccountNumber(Broker.WEALTHSIMPLE, "ACC123456789"), account.accountNumberHash)
        assertFalse(r.toString().contains("ACC123456789"))
        assertEquals("••••", maskAccountNumber("12"))
    }

    @Test
    fun currenciesStaySeparateInTotals() {
        val summary = PortfolioSummaryCalculator.calculate(
            parse(sample).let {
                com.invest.easymoney.domain.portfolio.Portfolio(emptyList(), it.accounts, it.positions, it.cashBalances)
            }
        )
        assertEquals(listOf("CAD", "USD"), summary.byCurrency.map { it.currency })
        assertEquals(BigDecimal("247.04"), summary.byCurrency.first { it.currency == "USD" }.marketValue)
        assertEquals(BigDecimal("50.25"), summary.byCurrency.first { it.currency == "CAD" }.cash)
    }

    @Test
    fun fingerprintIsDeterministicAndContentSensitive() {
        val a = parse(sample)
        val b = parse(sample)
        assertEquals(a.fingerprint, b.fingerprint)
        assertNotEquals(a.fingerprint, parse(sample.replace("247.04", "248.04")).fingerprint)
    }

    @Test
    fun missingTimestampFallsBackToImportTime() {
        val r = parse(sample.replace("As of 2026-09-09 10:26 GMT-04:00", ""))
        assertEquals(now, r.effectiveAt)
        assertTrue(PortfolioImportWarningCode.MISSING_SNAPSHOT_TIMESTAMP in r.warnings)
    }

    @Test
    fun invalidRowsAreRejectedWithoutStoppingImport() {
        val bad = sample + ""
        val withBad = bad.replace("\r\n\r\nAs of", "\r\nMy TFSA,TFSA,x,ACC123456789,BAD,,,Bad,EQUITY,1e5,LONG,,,,,,,,,,\r\n\r\nAs of")
        val r = parse(withBad)
        assertEquals(1, r.rejectedRows.size)
        assertEquals(3, r.positions.size)
    }

    @Test(expected = PortfolioImportException::class)
    fun unrelatedCsvIsRejected() {
        parse("a,b,c\n1,2,3\n")
    }
}

