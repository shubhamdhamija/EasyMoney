package com.invest.easymoney.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Decimals are stored as canonical plain strings; instants as epoch millis. */
@Entity(
    tableName = "portfolio_snapshots",
    indices = [Index(value = ["ownerId", "fingerprint"], unique = true)]
)
data class PortfolioSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val ownerId: String,
    val broker: String,
    val importedAtMs: Long,
    val effectiveAtMs: Long,
    val sourceFileName: String?,
    val accountCount: Int,
    val positionCount: Int,
    val cashBalanceCount: Int,
    val fingerprint: String
)

@Entity(
    tableName = "portfolio_accounts",
    indices = [Index("snapshotId"), Index(value = ["ownerId", "accountNumberHash"])]
)
data class PortfolioAccountEntity(
    @PrimaryKey val accountId: String,
    val ownerId: String,
    val snapshotId: String,
    val broker: String,
    val accountName: String,
    val accountType: String,
    val accountClassification: String?,
    val maskedAccountNumber: String,
    val accountNumberHash: String,
    val baseCurrency: String?,
    val effectiveAtMs: Long
)

@Entity(
    tableName = "portfolio_positions",
    indices = [Index("accountId"), Index("symbol"), Index("mic"), Index("ownerId")]
)
data class PortfolioPositionEntity(
    @PrimaryKey val positionId: String,
    val ownerId: String,
    val snapshotId: String,
    val accountId: String,
    val symbol: String?,
    val exchange: String?,
    val mic: String?,
    val securityName: String,
    val securityType: String,
    val quantity: String,
    val direction: String,
    val marketPrice: String?,
    val marketPriceCurrency: String?,
    val bookValueCad: String?,
    val bookValueCadCurrency: String?,
    val bookValueMarket: String?,
    val bookValueMarketCurrency: String?,
    val marketValue: String?,
    val marketValueCurrency: String?,
    val unrealizedReturn: String?,
    val unrealizedReturnCurrency: String?,
    val effectiveAtMs: Long,
    val warnings: String
)

@Entity(
    tableName = "portfolio_cash_balances",
    indices = [Index(value = ["accountId", "currency"]), Index("ownerId")]
)
data class PortfolioCashBalanceEntity(
    @PrimaryKey val cashBalanceId: String,
    val ownerId: String,
    val snapshotId: String,
    val accountId: String,
    val currency: String,
    val amount: String,
    val effectiveAtMs: Long
)

