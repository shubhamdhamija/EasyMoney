package com.invest.easymoney.data.portfolio.research

import com.google.gson.Gson
import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.dto.LocalPortfolioContextDto
import com.invest.easymoney.data.api.dto.LocalPositionDto
import com.invest.easymoney.data.api.dto.PortfolioContextRequestDto
import com.invest.easymoney.data.api.dto.PortfolioResearchErrorDto
import com.invest.easymoney.data.api.dto.PortfolioResearchReportDto
import com.invest.easymoney.data.api.dto.PortfolioResearchRequestDto
import com.invest.easymoney.data.api.dto.ResearchConsentRequestDto
import com.invest.easymoney.data.api.dto.ResearchWeightedDto
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.PortfolioSource
import com.invest.easymoney.domain.portfolio.research.PORTFOLIO_RESEARCH_NO_TRADES_STATEMENT
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchException
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchReport
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchRepository
import com.invest.easymoney.domain.portfolio.research.ResearchConcentration
import com.invest.easymoney.domain.portfolio.research.ResearchConsentHandle
import com.invest.easymoney.domain.portfolio.research.ResearchCurrencySummary
import com.invest.easymoney.domain.portfolio.research.ResearchDataCategory
import com.invest.easymoney.domain.portfolio.research.ResearchPortfolioTarget
import com.invest.easymoney.domain.portfolio.research.ResearchPositionLine
import com.invest.easymoney.domain.portfolio.research.ResearchWeightedPosition
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_RESEARCH_POSITIONS = 50

/**
 * Sends only a pointer (connection ID in a path) for server-owned portfolios, or a bounded,
 * normalized subset for the on-device CSV portfolio. Never logs IDs, holdings or values.
 */
@Singleton
class PortfolioResearchRepositoryImpl @Inject constructor(
    private val api: BackendApiService,
    private val gson: Gson
) : PortfolioResearchRepository {

    override suspend fun createResearchConsent(
        target: ResearchPortfolioTarget,
        categories: Set<ResearchDataCategory>
    ): NetworkResult<ResearchConsentHandle> = safe {
        val portfolioId = when (target) {
            is ResearchPortfolioTarget.Connected -> target.connectionId
            is ResearchPortfolioTarget.LocalCsv -> {
                val context = LocalContextMapper.map(target.portfolio, categories)
                    ?: throw PortfolioResearchException(
                        "PORTFOLIO_EMPTY", Messages.EMPTY, retryable = false
                    )
                api.createLocalPortfolioContext(context).portfolioId.orEmpty()
            }
        }
        if (portfolioId.isBlank()) throw PortfolioResearchException("PORTFOLIO_NOT_FOUND", Messages.NOT_FOUND, retryable = false)
        val response = api.createPortfolioResearchConsent(
            portfolioId,
            ResearchConsentRequestDto(categories.map { it.name })
        )
        val consentId = response.consentId
        if (consentId.isNullOrBlank()) throw PortfolioResearchException("UNKNOWN", Messages.GENERIC, response.requestId)
        ResearchConsentHandle(
            portfolioId = portfolioId,
            consentId = consentId,
            source = target.source,
            categories = categories,
            expiresAt = response.expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        )
    }

    override suspend fun startPortfolioResearch(
        handle: ResearchConsentHandle,
        categories: Set<ResearchDataCategory>
    ): NetworkResult<PortfolioResearchReport> = safe {
        val dto = api.startPortfolioResearch(
            PortfolioResearchRequestDto(
                portfolioContext = PortfolioContextRequestDto(
                    portfolioId = handle.portfolioId,
                    source = handle.source.name,
                    includePositions = ResearchDataCategory.POSITIONS in categories,
                    includeCash = ResearchDataCategory.CASH_BALANCES in categories,
                    includeBookValue = ResearchDataCategory.BOOK_VALUES in categories,
                    maximumPositions = 20,
                    consentId = handle.consentId
                )
            )
        )
        ResearchReportMapper.map(dto)
    }

    override suspend fun revokeResearchConsent(handle: ResearchConsentHandle): NetworkResult<Unit> = safe {
        api.revokePortfolioResearchConsent(handle.portfolioId, handle.consentId)
        Unit
    }

    private suspend fun <T> safe(block: suspend () -> T): NetworkResult<T> = try {
        NetworkResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: PortfolioResearchException) {
        NetworkResult.Error(e)
    } catch (e: HttpException) {
        NetworkResult.Error(ResearchErrors.fromHttp(e.code(), runCatching { e.response()?.errorBody()?.string() }.getOrNull(), gson))
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error(PortfolioResearchException("TIMEOUT", "The research request timed out. Please try again."))
    } catch (e: IOException) {
        NetworkResult.Error(PortfolioResearchException("NETWORK", "Check your connection and try again."))
    } catch (e: Exception) {
        NetworkResult.Error(PortfolioResearchException("UNKNOWN", Messages.GENERIC))
    }
}

internal object Messages {
    const val GENERIC = "Portfolio research could not be completed. Please try again."
    const val NOT_FOUND = "This portfolio is no longer available."
    const val EMPTY = "There are no portfolio positions to research."
    const val CONSENT = "Confirm portfolio research again to continue."
}

internal object ResearchErrors {
    fun fromHttp(status: Int, body: String?, gson: Gson): PortfolioResearchException {
        val parsed = runCatching { gson.fromJson(body.orEmpty(), PortfolioResearchErrorDto::class.java) }.getOrNull()
        val requestId = parsed?.requestId
        return when (parsed?.error?.code) {
            "PORTFOLIO_CONSENT_REQUIRED", "PORTFOLIO_CONSENT_EXPIRED" ->
                PortfolioResearchException("PORTFOLIO_CONSENT_REQUIRED", Messages.CONSENT, requestId, retryable = true, consentRequired = true)
            "PORTFOLIO_NOT_FOUND", "PORTFOLIO_ACCESS_DENIED" ->
                PortfolioResearchException("PORTFOLIO_NOT_FOUND", Messages.NOT_FOUND, requestId, retryable = false)
            "PORTFOLIO_EMPTY" -> PortfolioResearchException("PORTFOLIO_EMPTY", Messages.EMPTY, requestId, retryable = false)
            "PORTFOLIO_CONTEXT_TOO_LARGE" ->
                PortfolioResearchException("PORTFOLIO_CONTEXT_TOO_LARGE", "Select fewer portfolio positions and try again.", requestId, retryable = false)
            "PORTFOLIO_CURRENCY_REQUIRED" ->
                PortfolioResearchException("PORTFOLIO_CURRENCY_REQUIRED", "Choose one currency for the analysis.", requestId, retryable = false)
            else -> when (status) {
                401 -> PortfolioResearchException("UNAUTHENTICATED", "Sign in again to research your portfolio.", requestId, retryable = false)
                403 -> PortfolioResearchException("FORBIDDEN", "You do not have permission to perform this action.", requestId, retryable = false)
                404 -> PortfolioResearchException("PORTFOLIO_NOT_FOUND", Messages.NOT_FOUND, requestId, retryable = false)
                429 -> PortfolioResearchException("RATE_LIMITED", "Too many requests. Please try again later.", requestId)
                in 500..599 -> PortfolioResearchException("UNAVAILABLE", "Portfolio research is temporarily unavailable.", requestId)
                else -> PortfolioResearchException("UNKNOWN", Messages.GENERIC, requestId)
            }
        }
    }
}

/** Builds the bounded, normalized CSV context. Pure and unit-testable. */
internal object LocalContextMapper {
    private val currencyPattern = Regex("^[A-Z]{3}$")
    private val symbolPattern = Regex("[^A-Z0-9.-]")

    /** Null when there is nothing usable to send. At most 50 positions, largest market value first. */
    fun map(portfolio: Portfolio, categories: Set<ResearchDataCategory>): LocalPortfolioContextDto? {
        val accountTypes = portfolio.accounts.associate { it.accountId to it.accountType.name }
        val includeBook = ResearchDataCategory.BOOK_VALUES in categories
        val rows = portfolio.positions
            .sortedByDescending { it.marketValue ?: java.math.BigDecimal.ZERO }
            .mapNotNull { p ->
                val currency = (p.marketValueCurrency?.value ?: p.marketPriceCurrency?.value)
                    ?.trim()?.uppercase()?.takeIf { currencyPattern.matches(it) } ?: return@mapNotNull null
                val symbol = p.symbol?.trim()?.uppercase()?.replace(symbolPattern, "")?.take(15)?.ifBlank { null }
                val name = p.securityName.trim().replace(Regex("\\s+"), " ").take(120).ifBlank { return@mapNotNull null }
                val book = if (!includeBook) null else when {
                    p.bookValueMarket != null && p.bookValueMarketCurrency?.value?.uppercase() == currency -> p.bookValueMarket
                    p.bookValueCad != null && currency == "CAD" -> p.bookValueCad
                    else -> null
                }
                LocalPositionDto(
                    symbol = symbol,
                    securityName = name,
                    securityType = p.securityType.name,
                    accountType = accountTypes[p.accountId] ?: "UNKNOWN",
                    quantity = p.quantity.toPlainString(),
                    marketValue = p.marketValue?.toPlainString(),
                    currency = currency,
                    bookValue = book?.toPlainString()
                )
            }
            .take(MAX_RESEARCH_POSITIONS)
        if (rows.isEmpty()) return null
        return LocalPortfolioContextDto(
            source = PortfolioSource.WEALTHSIMPLE_CSV.name,
            snapshotAt = (portfolio.latestEffectiveAt ?: Instant.now()).toString(),
            positions = rows
        )
    }
}

internal object ResearchReportMapper {
    private val warningText = mapOf(
        "STALE_SNAPSHOT" to "This snapshot may be out of date.",
        "MIXED_CURRENCIES" to "CAD and USD are shown separately and never combined.",
        "MISSING_MARKET_VALUES" to "Some positions have no market value.",
        "POSITIONS_EXCLUDED_FROM_ALLOCATION" to "Some positions were excluded from allocation because a value was missing.",
        "MISSING_BOOK_VALUES" to "Book values are not available for some positions.",
        "EMPTY_PORTFOLIO" to "The portfolio has no positions.",
        "NO_POSITIONS_FOR_CURRENCY" to "No positions were found for that currency.",
        "SNAPSHOT_SOURCE_IS_CSV" to "Based on your imported CSV snapshot.",
        "SNAPSHOT_SOURCE_IS_SANDBOX" to "Based on a simulated sandbox portfolio.",
        "PARTIAL_POSITION_LIST" to "Only part of the position list was analyzed.",
        "UNKNOWN_SECURITY_TYPE" to "Some security types are unknown.",
        "AMBIGUOUS_SYMBOL" to "More than one security shares the same ticker."
    )

    fun map(dto: PortfolioResearchReportDto): PortfolioResearchReport {
        if (dto.status.equals("failed", ignoreCase = true) || dto.summary.isNullOrBlank()) {
            throw ResearchErrors.fromHttp(200, """{"error":{"code":"${dto.errorCode.orEmpty().filter { it.isLetter() || it == '_' }}"}}""", Gson())
        }
        val source = runCatching { PortfolioSource.valueOf(dto.portfolioSource.orEmpty()) }.getOrDefault(PortfolioSource.DEMO)
        return PortfolioResearchReport(
            summary = dto.summary.trim(),
            partial = dto.status.equals("partial", ignoreCase = true),
            source = source,
            snapshotAt = dto.snapshotAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            currencySummaries = dto.currencySummaries.orEmpty().mapNotNull {
                val currency = it.currency ?: return@mapNotNull null
                ResearchCurrencySummary(
                    currency, it.investmentMarketValue ?: return@mapNotNull null,
                    it.cashValue, it.bookValue, it.unrealizedReturn, it.positionCount ?: 0
                )
            },
            positions = dto.positions.orEmpty().take(20).mapNotNull {
                ResearchPositionLine(
                    it.symbol, it.securityName ?: return@mapNotNull null, it.securityType.orEmpty(),
                    it.accountType.orEmpty(), it.quantity.orEmpty(), it.marketValue,
                    it.currency ?: return@mapNotNull null, it.bookValue
                )
            },
            concentration = dto.concentration.orEmpty().mapNotNull {
                ResearchConcentration(
                    currency = it.currency ?: return@mapNotNull null,
                    eligibleMarketValue = it.eligibleMarketValue.orEmpty(),
                    excludedPositionCount = it.excludedPositionCount ?: 0,
                    largestPosition = it.largestPosition?.toWeighted(),
                    topPositions = it.topPositions.orEmpty().mapNotNull { w -> w.toWeighted() },
                    topPositionsWeightPercent = it.topPositionsWeightPercent,
                    cashAllocationPercent = it.cashAllocationPercent
                )
            },
            warnings = dto.warnings.orEmpty().mapNotNull { w -> warningText[w.code] }.distinct(),
            evidence = dto.evidenceSummary.orEmpty().filter { it.isNotBlank() }.take(5),
            statement = PORTFOLIO_RESEARCH_NO_TRADES_STATEMENT
        )
    }

    private fun ResearchWeightedDto.toWeighted(): ResearchWeightedPosition? {
        val label = symbol?.takeIf { it.isNotBlank() } ?: securityName ?: return null
        return ResearchWeightedPosition(label, marketValue.orEmpty(), weightPercent.orEmpty())
    }
}
