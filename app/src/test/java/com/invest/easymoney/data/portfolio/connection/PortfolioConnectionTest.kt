package com.invest.easymoney.data.portfolio.connection

import com.invest.easymoney.data.api.dto.ConnectedPortfolioResponseDto
import com.invest.easymoney.data.api.dto.ConnectionDto
import com.invest.easymoney.data.api.dto.SyncedAccountDto
import com.invest.easymoney.data.api.dto.SyncedCashBalanceDto
import com.invest.easymoney.data.api.dto.SyncedPortfolioDto
import com.invest.easymoney.data.api.dto.SyncedPositionDto
import com.invest.easymoney.domain.portfolio.PortfolioAccountType
import com.invest.easymoney.domain.portfolio.PortfolioSource
import com.invest.easymoney.domain.portfolio.PortfolioSummaryCalculator
import com.invest.easymoney.domain.portfolio.connection.ConnectionStatus
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionSession
import com.invest.easymoney.ui.portfolio.connection.PortalCallback
import com.invest.easymoney.ui.portfolio.connection.PortfolioDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PortfolioConnectionTest {

    private fun validate(
        scheme: String? = "easymoney",
        host: String? = "portfolio",
        path: String? = "/snaptrade",
        status: String? = "SUCCESS",
        session: String? = null,
        pending: String? = "cs_1"
    ) = PortfolioDeepLink.validate(scheme, host, path, status, session, pending)

    @Test fun validCallbackReturnsPendingSession() {
        assertEquals(PortalCallback.Returned("cs_1"), validate())
    }

    @Test fun wrongSchemeHostOrPathIgnored() {
        assertNull(validate(scheme = "https"))
        assertNull(validate(host = "evil"))
        assertNull(validate(path = "/other"))
    }

    @Test fun callbackWithoutPendingSessionIgnored() {
        assertNull(validate(pending = null))
    }

    @Test fun mismatchedSessionParameterIgnored() {
        assertNull(validate(session = "cs_other"))
        assertEquals(PortalCallback.Returned("cs_1"), validate(session = "cs_1"))
    }

    @Test fun cancelAndErrorMapToNonSuccess() {
        assertEquals(PortalCallback.Canceled("cs_1"), validate(status = "ABANDONED"))
        assertEquals(PortalCallback.Failed("cs_1"), validate(status = "error"))
        assertNull(validate(status = "SOMETHING_ELSE"))
    }

    @Test fun errorCodesMapToSafeMessages() {
        val disabled = PortfolioConnectionErrors.fromCode("SNAPTRADE_DISABLED", "r1", PortfolioConnectionErrors.Context.SESSION)
        assertEquals("Automatic portfolio connection is currently unavailable.", disabled.message)
        assertEquals("r1", disabled.requestId)
        assertEquals("Reconnect the portfolio to continue updating it.",
            PortfolioConnectionErrors.fromCode("RECONNECT_REQUIRED", null, PortfolioConnectionErrors.Context.GENERAL).message)
        assertEquals("The portfolio update timed out.",
            PortfolioConnectionErrors.fromCode("SYNC_TIMEOUT", null, PortfolioConnectionErrors.Context.GENERAL).message)
        assertEquals("Sign in again to connect your portfolio.",
            PortfolioConnectionErrors.fromCode("INVALID_ACCESS_TOKEN", null, PortfolioConnectionErrors.Context.GENERAL).message)
        assertEquals("This portfolio connection is no longer available.",
            PortfolioConnectionErrors.fromCode("CONNECTION_NOT_FOUND", null, PortfolioConnectionErrors.Context.GENERAL).message)
        assertEquals("EasyMoney could not start the connection.",
            PortfolioConnectionErrors.fromCode("WHATEVER", null, PortfolioConnectionErrors.Context.SESSION).message)
    }

    @Test fun sessionToStringRedactsPortalUrl() {
        val s = PortfolioConnectionSession("cs_secret", "https://app.snaptrade.com/portal?token=abc", null)
        assertFalse(s.toString().contains("token"))
        assertFalse(s.toString().contains("cs_secret"))
    }

    private fun response() = ConnectedPortfolioResponseDto(
        connection = ConnectionDto(
            "pc_1", "SNAPTRADE", "SnapTrade Sandbox", "READY", "READ", "2026-09-09T10:00:00+00:00",
            "2026-09-09T10:26:00+00:00", null, 2, 3, 2
        ),
        portfolio = SyncedPortfolioDto(
            snapshotId = "snap_1",
            effectiveAt = "2026-09-09T10:26:00+00:00",
            accounts = listOf(
                SyncedAccountDto("a1", "Margin", "TFSA", "TFSA", null, "••••6789", "CAD", null),
                SyncedAccountDto("a2", "Weird", "NOT_A_TYPE", null, null, "", "USD", null)
            ),
            positions = listOf(
                SyncedPositionDto("p1", "a1", "sec1", "AAPL", "Apple", "NASDAQ", "XNAS", "10", "100.50", "1005.00", "USD", "90.00", "EQUITY"),
                SyncedPositionDto("p2", "a1", null, "XYZ", "No Price", null, null, "5", null, null, "CAD", null, "OTHER")
            ),
            cashBalances = listOf(
                SyncedCashBalanceDto("a1", "CAD", "1240.15"),
                SyncedCashBalanceDto("a2", "USD", "405.18")
            ),
            warnings = emptyList()
        ),
        requestId = "req"
    )

    @Test fun mapsToSameDomainModelAsCsv() {
        val result = ConnectedPortfolioMapper.toConnectedPortfolio(response())
        assertEquals(ConnectionStatus.READY, result.connection.status)
        assertTrue(result.connection.isReadOnly)
        val p = result.portfolio!!
        assertEquals(PortfolioSource.SNAPTRADE_SANDBOX, p.snapshots.single().source)
        assertEquals(PortfolioAccountType.TFSA, p.accounts[0].accountType)
        assertEquals(PortfolioAccountType.UNKNOWN, p.accounts[1].accountType)
        assertEquals("", p.accounts[0].accountNumberHash)
        val apple = p.positions[0]
        assertEquals(BigDecimal("1005.00"), apple.marketValue)
        assertEquals(0, BigDecimal("105.00").compareTo(apple.unrealizedReturn))
    }

    @Test fun missingPriceStaysNullAndCurrenciesStaySeparate() {
        val p = ConnectedPortfolioMapper.toConnectedPortfolio(response()).portfolio!!
        val noPrice = p.positions[1]
        assertNull(noPrice.marketValue)
        assertNull(noPrice.marketPrice)
        val summary = PortfolioSummaryCalculator.calculate(p)
        assertEquals(listOf("CAD", "USD"), summary.byCurrency.map { it.currency })
        val usd = summary.byCurrency.first { it.currency == "USD" }
        assertEquals(0, BigDecimal("1005.00").compareTo(usd.marketValue))
        assertEquals(0, BigDecimal("405.18").compareTo(usd.cash))
    }
}
