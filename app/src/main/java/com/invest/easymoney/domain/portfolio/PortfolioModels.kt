package com.invest.easymoney.domain.portfolio

import java.math.BigDecimal
import java.time.Instant

enum class Broker { WEALTHSIMPLE, GENERIC_CSV, MANUAL }

@JvmInline
value class CurrencyCode(val value: String)

enum class PortfolioAccountType { TFSA, RRSP, FHSA, RESP, LIRA, PERSONAL, CRYPTO, CASH, UNKNOWN }

enum class PortfolioSecurityType {
    EQUITY, EXCHANGE_TRADED_FUND, CURRENCY, MUTUAL_FUND, OPTION, FIXED_INCOME, CRYPTO, OTHER
}

enum class PositionDirection { LONG, SHORT, UNKNOWN }

enum class PortfolioImportWarningCode {
    MISSING_SNAPSHOT_TIMESTAMP,
    INVALID_SNAPSHOT_TIMESTAMP,
    UNKNOWN_ACCOUNT_TYPE,
    UNKNOWN_SECURITY_TYPE,
    UNKNOWN_CURRENCY,
    MISSING_MARKET_PRICE,
    MISSING_MARKET_VALUE,
    MISSING_BOOK_VALUE,
    ZERO_MARKET_VALUE,
    DIRECTION_QUANTITY_MISMATCH,
    EXTRA_COLUMNS_IGNORED,
    DUPLICATE_POSITION,
    DUPLICATE_CASH_BALANCE,
    SENSITIVE_ACCOUNT_DATA_REDACTED,
    MIXED_CURRENCIES,
    STALE_SNAPSHOT
}

data class PortfolioSnapshot(
    val snapshotId: String,
    val broker: Broker,
    val importedAt: Instant,
    val effectiveAt: Instant,
    val sourceFileName: String?,
    val accountCount: Int,
    val positionCount: Int,
    val cashBalanceCount: Int,
    val fingerprint: String
)

data class PortfolioAccount(
    val accountId: String,
    val snapshotId: String,
    val broker: Broker,
    val accountName: String,
    val accountType: PortfolioAccountType,
    val accountClassification: String?,
    val maskedAccountNumber: String,
    val accountNumberHash: String,
    val baseCurrency: CurrencyCode?,
    val effectiveAt: Instant
)

data class PortfolioPosition(
    val positionId: String,
    val snapshotId: String,
    val accountId: String,
    val symbol: String?,
    val exchange: String?,
    val mic: String?,
    val securityName: String,
    val securityType: PortfolioSecurityType,
    val quantity: BigDecimal,
    val direction: PositionDirection,
    val marketPrice: BigDecimal?,
    val marketPriceCurrency: CurrencyCode?,
    val bookValueCad: BigDecimal?,
    val bookValueCadCurrency: CurrencyCode?,
    val bookValueMarket: BigDecimal?,
    val bookValueMarketCurrency: CurrencyCode?,
    val marketValue: BigDecimal?,
    val marketValueCurrency: CurrencyCode?,
    val unrealizedReturn: BigDecimal?,
    val unrealizedReturnCurrency: CurrencyCode?,
    val effectiveAt: Instant,
    val warnings: List<PortfolioImportWarningCode>
)

data class PortfolioCashBalance(
    val cashBalanceId: String,
    val snapshotId: String,
    val accountId: String,
    val currency: CurrencyCode,
    val amount: BigDecimal,
    val effectiveAt: Instant
)

/** Safe rejection record. Never contains raw rows or account numbers. */
data class PortfolioRejectedRow(
    val rowNumber: Int,
    val symbol: String?,
    val securityName: String?,
    val reasonCode: String,
    val safeMessage: String
)

enum class DuplicateStatus { FIRST_IMPORT, IDENTICAL_IMPORT, NEWER_SNAPSHOT, OLDER_SNAPSHOT, DIFFERENT_ACCOUNT }

enum class PortfolioReplacementDecision { REPLACE, IMPORT_AS_ACTIVE, CANCEL }

data class PortfolioImportSummary(
    val accountCount: Int,
    val positionCount: Int,
    val cashBalanceCount: Int,
    val rejectedRowCount: Int,
    val currencies: List<String>,
    val securityTypes: List<PortfolioSecurityType>
)

/** Output of a CSV parser, before any persistence decision. */
data class PortfolioImportResult(
    val snapshotId: String,
    val broker: Broker,
    val sourceFileName: String?,
    val importedAt: Instant,
    val effectiveAt: Instant,
    val accounts: List<PortfolioAccount>,
    val positions: List<PortfolioPosition>,
    val cashBalances: List<PortfolioCashBalance>,
    val warnings: List<PortfolioImportWarningCode>,
    val rejectedRows: List<PortfolioRejectedRow>,
    val fingerprint: String
)

data class PortfolioImportPreview(
    val previewToken: String,
    val snapshotId: String,
    val broker: Broker,
    val sourceFileName: String?,
    val importedAt: Instant,
    val effectiveAt: Instant,
    val accounts: List<PortfolioAccount>,
    val positions: List<PortfolioPosition>,
    val cashBalances: List<PortfolioCashBalance>,
    val warnings: List<PortfolioImportWarningCode>,
    val rejectedRows: List<PortfolioRejectedRow>,
    val fingerprint: String,
    val duplicateStatus: DuplicateStatus,
    val summary: PortfolioImportSummary
)

data class Portfolio(
    val snapshots: List<PortfolioSnapshot>,
    val accounts: List<PortfolioAccount>,
    val positions: List<PortfolioPosition>,
    val cashBalances: List<PortfolioCashBalance>
) {
    val latestEffectiveAt: Instant? get() = snapshots.maxOfOrNull { it.effectiveAt }
}

/** Safe, user-presentable import failure. */
class PortfolioImportException(message: String) : Exception(message)

