package com.invest.easymoney.data.portfolio.connection

import com.google.gson.JsonParser
import com.invest.easymoney.data.api.dto.ConnectedPortfolioResponseDto
import com.invest.easymoney.data.api.dto.ConnectionDto
import com.invest.easymoney.data.api.dto.SyncedPortfolioDto
import com.invest.easymoney.domain.portfolio.Broker
import com.invest.easymoney.domain.portfolio.CurrencyCode
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.PortfolioAccount
import com.invest.easymoney.domain.portfolio.PortfolioAccountType
import com.invest.easymoney.domain.portfolio.PortfolioCashBalance
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import com.invest.easymoney.domain.portfolio.PortfolioSnapshot
import com.invest.easymoney.domain.portfolio.PortfolioSource
import com.invest.easymoney.domain.portfolio.PositionDirection
import com.invest.easymoney.domain.portfolio.connection.ConnectedPortfolio
import com.invest.easymoney.domain.portfolio.connection.ConnectionStatus
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnection
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionException
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

/** Maps normalized server DTOs into the same domain models the CSV portfolio uses. */
object ConnectedPortfolioMapper {

    fun toConnection(dto: ConnectionDto): PortfolioConnection = PortfolioConnection(
        connectionId = dto.connectionId.orEmpty(),
        institutionName = dto.institutionName?.takeIf { it.isNotBlank() } ?: "SnapTrade Sandbox",
        status = runCatching { ConnectionStatus.valueOf(dto.status.orEmpty()) }.getOrDefault(ConnectionStatus.UNKNOWN),
        createdAt = instant(dto.createdAt),
        lastSuccessfulSyncAt = instant(dto.lastSuccessfulSyncAt),
        accountCount = dto.accountCount ?: 0,
        positionCount = dto.positionCount ?: 0
    )

    fun toConnectedPortfolio(response: ConnectedPortfolioResponseDto): ConnectedPortfolio {
        val connectionDto = response.connection
            ?: throw PortfolioConnectionException("PROVIDER_UNAVAILABLE", Messages.PROVIDER_UNAVAILABLE, response.requestId, true)
        return ConnectedPortfolio(
            connection = toConnection(connectionDto),
            portfolio = response.portfolio?.let(::toPortfolio),
            warnings = response.portfolio?.warnings.orEmpty()
        )
    }

    fun toPortfolio(dto: SyncedPortfolioDto): Portfolio {
        val snapshotId = dto.snapshotId.orEmpty()
        val effectiveAt = instant(dto.effectiveAt) ?: Instant.now()
        val accounts = dto.accounts.orEmpty().mapNotNull { a ->
            val id = a.providerAccountId ?: return@mapNotNull null
            PortfolioAccount(
                accountId = id,
                snapshotId = snapshotId,
                broker = Broker.SNAPTRADE,
                accountName = a.displayName ?: "Account",
                accountType = runCatching { PortfolioAccountType.valueOf(a.accountType.orEmpty()) }
                    .getOrDefault(PortfolioAccountType.UNKNOWN),
                accountClassification = a.providerCategory,
                maskedAccountNumber = a.maskedAccountNumber.orEmpty(),
                accountNumberHash = "", // the server never sends the account number or a hash
                baseCurrency = a.currency?.let(::CurrencyCode),
                effectiveAt = effectiveAt
            )
        }
        val positions = dto.positions.orEmpty().mapNotNull { p ->
            val accountId = p.accountId ?: return@mapNotNull null
            val quantity = decimal(p.quantity) ?: return@mapNotNull null
            val price = decimal(p.marketPrice)
            val value = decimal(p.marketValue)
            val avg = decimal(p.averageCost)
            val currency = p.currency?.let(::CurrencyCode)
            val book = if (avg != null) avg.multiply(quantity) else null
            val unrealized = if (value != null && book != null) value.subtract(book) else null
            PortfolioPosition(
                positionId = p.providerPositionId ?: "$accountId|${p.symbol}",
                snapshotId = snapshotId,
                accountId = accountId,
                symbol = p.symbol,
                exchange = p.exchange,
                mic = p.mic,
                securityName = p.securityName ?: p.symbol ?: "Unknown security",
                securityType = runCatching { PortfolioSecurityType.valueOf(p.securityType.orEmpty()) }
                    .getOrDefault(PortfolioSecurityType.OTHER),
                quantity = quantity,
                direction = if (quantity.signum() < 0) PositionDirection.SHORT else PositionDirection.LONG,
                marketPrice = price,
                marketPriceCurrency = if (price != null) currency else null,
                bookValueCad = null,
                bookValueCadCurrency = null,
                bookValueMarket = book,
                bookValueMarketCurrency = if (book != null) currency else null,
                marketValue = value,
                marketValueCurrency = if (value != null) currency else null,
                unrealizedReturn = unrealized,
                unrealizedReturnCurrency = if (unrealized != null) currency else null,
                effectiveAt = effectiveAt,
                warnings = emptyList()
            )
        }
        val cash = dto.cashBalances.orEmpty().mapNotNull { c ->
            val accountId = c.accountId ?: return@mapNotNull null
            val currency = c.currency ?: return@mapNotNull null
            val amount = decimal(c.amount) ?: return@mapNotNull null
            PortfolioCashBalance(
                cashBalanceId = "$accountId|$currency",
                snapshotId = snapshotId,
                accountId = accountId,
                currency = CurrencyCode(currency),
                amount = amount,
                effectiveAt = effectiveAt
            )
        }
        val snapshot = PortfolioSnapshot(
            snapshotId = snapshotId,
            broker = Broker.SNAPTRADE,
            importedAt = Instant.now(),
            effectiveAt = effectiveAt,
            sourceFileName = null,
            accountCount = accounts.size,
            positionCount = positions.size,
            cashBalanceCount = cash.size,
            fingerprint = snapshotId,
            source = PortfolioSource.SNAPTRADE_SANDBOX
        )
        return Portfolio(listOf(snapshot), accounts, positions, cash)
    }

    private fun decimal(value: String?): BigDecimal? = value?.let { runCatching { BigDecimal(it) }.getOrNull() }
    private fun instant(value: String?): Instant? = value?.let {
        runCatching { Instant.parse(it) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(it).toInstant() }.getOrNull()
            // Server timestamps without an offset are UTC.
            ?: runCatching { java.time.LocalDateTime.parse(it).toInstant(java.time.ZoneOffset.UTC) }.getOrNull()
    }
}

/** Fixed, safe user-facing messages. Nothing from the server body is ever shown verbatim. */
object Messages {
    const val DISABLED = "Automatic portfolio connection is currently unavailable."
    const val SESSION_FAILED = "EasyMoney could not start the connection."
    const val CANCELED = "The portfolio connection was canceled."
    const val INCOMPLETE = "The demo brokerage was not connected."
    const val RECONNECT = "Reconnect the portfolio to continue updating it."
    const val SYNC_TIMEOUT = "The portfolio update timed out."
    const val PROVIDER_UNAVAILABLE = "Portfolio synchronization is temporarily unavailable."
    const val UNAUTHORIZED = "Sign in again to connect your portfolio."
    const val NOT_AVAILABLE = "This portfolio connection is no longer available."
}

object PortfolioConnectionErrors {
    enum class Context { SESSION, GENERAL }

    fun map(error: Throwable, context: Context = Context.GENERAL): PortfolioConnectionException {
        if (error is PortfolioConnectionException) return error
        if (error is HttpException) {
            val parsed = parseBody(error.response()?.errorBody()?.string())
            val code = parsed.first ?: when (error.code()) {
                401 -> "AUTHENTICATION_REQUIRED"
                403 -> "INSUFFICIENT_SCOPE"
                404 -> "CONNECTION_NOT_FOUND"
                else -> "PROVIDER_UNAVAILABLE"
            }
            return fromCode(code, parsed.second, context)
        }
        if (error is IOException) {
            return PortfolioConnectionException("PROVIDER_UNAVAILABLE", Messages.PROVIDER_UNAVAILABLE, null, true)
        }
        return PortfolioConnectionException(
            "INTERNAL_ERROR",
            if (context == Context.SESSION) Messages.SESSION_FAILED else Messages.PROVIDER_UNAVAILABLE,
            null,
            false
        )
    }

    fun fromCode(code: String, requestId: String?, context: Context): PortfolioConnectionException {
        val (message, retryable) = when (code) {
            "SNAPTRADE_DISABLED" -> Messages.DISABLED to false
            "CONNECTION_INCOMPLETE" -> Messages.INCOMPLETE to true
            "RECONNECT_REQUIRED" -> Messages.RECONNECT to false
            "SYNC_TIMEOUT" -> Messages.SYNC_TIMEOUT to true
            "PROVIDER_UNAVAILABLE", "PROVIDER_RATE_LIMITED", "SYNC_IN_PROGRESS" -> Messages.PROVIDER_UNAVAILABLE to true
            "AUTHENTICATION_REQUIRED", "INVALID_ACCESS_TOKEN", "INSUFFICIENT_SCOPE" -> Messages.UNAUTHORIZED to false
            "CONNECTION_NOT_FOUND", "SESSION_NOT_FOUND", "SESSION_CONSUMED", "SESSION_EXPIRED", "NO_SNAPSHOT" ->
                Messages.NOT_AVAILABLE to false
            else -> (if (context == Context.SESSION) Messages.SESSION_FAILED else Messages.PROVIDER_UNAVAILABLE) to false
        }
        return PortfolioConnectionException(code, message, requestId, retryable)
    }

    /** Returns (code, requestId) from either {"error":{"code"},"request_id"} or {"detail":{"code"}}. */
    private fun parseBody(body: String?): Pair<String?, String?> {
        if (body.isNullOrBlank()) return null to null
        return runCatching {
            val root = JsonParser.parseString(body).asJsonObject
            val code = root.getAsJsonObject("error")?.get("code")?.asString
                ?: root.getAsJsonObject("detail")?.get("code")?.asString
            val requestId = root.get("request_id")?.takeIf { !it.isJsonNull }?.asString
            code to requestId
        }.getOrDefault(null to null)
    }
}
