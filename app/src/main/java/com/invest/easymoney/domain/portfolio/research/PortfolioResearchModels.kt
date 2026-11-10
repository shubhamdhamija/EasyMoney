package com.invest.easymoney.domain.portfolio.research

import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.PortfolioSource
import java.time.Instant

/**
 * Read-only, consent-gated portfolio research. There is intentionally no order, trade,
 * transfer, deposit, withdrawal or rebalance capability anywhere in this package.
 */
enum class ResearchDataCategory(val label: String, val defaultOn: Boolean) {
    ACCOUNTS("Accounts", true),
    POSITIONS("Holdings", true),
    CASH_BALANCES("Cash", true),
    BOOK_VALUES("Book values", false)
}

/** The portfolio the user chose to research. Sources are never merged. */
sealed interface ResearchPortfolioTarget {
    val source: PortfolioSource
    val snapshotAt: Instant?
    val sourceLabel: String

    /** Server-owned (SnapTrade Sandbox) portfolio. The connection ID is sent only as a path. */
    class Connected(val connectionId: String, override val snapshotAt: Instant?) : ResearchPortfolioTarget {
        override val source = PortfolioSource.SNAPTRADE_SANDBOX
        override val sourceLabel = "SnapTrade Sandbox (simulated)"
        override fun toString() = "Connected(<redacted>)"
    }

    /** On-device CSV portfolio. Only a normalized, bounded subset is ever sent. */
    class LocalCsv(val portfolio: Portfolio) : ResearchPortfolioTarget {
        override val source = PortfolioSource.WEALTHSIMPLE_CSV
        override val snapshotAt: Instant? get() = portfolio.latestEffectiveAt
        override val sourceLabel = "Wealthsimple CSV import"
        override fun toString() = "LocalCsv(<redacted>)"
    }
}

/** Opaque, short-lived consent handle. Never shown in UI or logged. */
class ResearchConsentHandle(
    val portfolioId: String,
    val consentId: String,
    val source: PortfolioSource,
    val categories: Set<ResearchDataCategory>,
    val expiresAt: Instant?
) {
    override fun toString() = "ResearchConsentHandle(<redacted>)"
}

data class ResearchCurrencySummary(
    val currency: String,
    val investmentMarketValue: String,
    val cashValue: String?,
    val bookValue: String?,
    val unrealizedReturn: String?,
    val positionCount: Int
)

data class ResearchPositionLine(
    val symbol: String?,
    val securityName: String,
    val securityType: String,
    val accountType: String,
    val quantity: String,
    val marketValue: String?,
    val currency: String,
    val bookValue: String?
)

data class ResearchWeightedPosition(val label: String, val marketValue: String, val weightPercent: String)

data class ResearchConcentration(
    val currency: String,
    val eligibleMarketValue: String,
    val excludedPositionCount: Int,
    val largestPosition: ResearchWeightedPosition?,
    val topPositions: List<ResearchWeightedPosition>,
    val topPositionsWeightPercent: String?,
    val cashAllocationPercent: String?
)

data class PortfolioResearchReport(
    val summary: String,
    val partial: Boolean,
    val source: PortfolioSource,
    val snapshotAt: Instant?,
    val currencySummaries: List<ResearchCurrencySummary>,
    val positions: List<ResearchPositionLine>,
    val concentration: List<ResearchConcentration>,
    val warnings: List<String>,
    val evidence: List<String>,
    val statement: String
)

/** Safe failure: stable code and a user-presentable message only. */
class PortfolioResearchException(
    val code: String,
    override val message: String,
    val requestId: String? = null,
    val retryable: Boolean = true,
    /** The user must confirm again (consent expired, revoked or missing). */
    val consentRequired: Boolean = false
) : Exception(message)

interface PortfolioResearchRepository {
    suspend fun createResearchConsent(
        target: ResearchPortfolioTarget,
        categories: Set<ResearchDataCategory>
    ): NetworkResult<ResearchConsentHandle>

    suspend fun startPortfolioResearch(
        handle: ResearchConsentHandle,
        categories: Set<ResearchDataCategory>
    ): NetworkResult<PortfolioResearchReport>

    suspend fun revokeResearchConsent(handle: ResearchConsentHandle): NetworkResult<Unit>
}

const val PORTFOLIO_RESEARCH_NO_TRADES_STATEMENT = "No trades or account changes were made."
