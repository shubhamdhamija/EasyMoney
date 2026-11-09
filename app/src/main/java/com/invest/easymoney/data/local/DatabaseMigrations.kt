package com.invest.easymoney.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE watchlist ADD COLUMN companyName TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    /** Adds the normalized, read-only portfolio tables. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `portfolio_snapshots` (`snapshotId` TEXT NOT NULL, " +
                    "`ownerId` TEXT NOT NULL, `broker` TEXT NOT NULL, `importedAtMs` INTEGER NOT NULL, " +
                    "`effectiveAtMs` INTEGER NOT NULL, `sourceFileName` TEXT, `accountCount` INTEGER NOT NULL, " +
                    "`positionCount` INTEGER NOT NULL, `cashBalanceCount` INTEGER NOT NULL, " +
                    "`fingerprint` TEXT NOT NULL, PRIMARY KEY(`snapshotId`))"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_portfolio_snapshots_ownerId_fingerprint` " +
                    "ON `portfolio_snapshots` (`ownerId`, `fingerprint`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `portfolio_accounts` (`accountId` TEXT NOT NULL, " +
                    "`ownerId` TEXT NOT NULL, `snapshotId` TEXT NOT NULL, `broker` TEXT NOT NULL, " +
                    "`accountName` TEXT NOT NULL, `accountType` TEXT NOT NULL, `accountClassification` TEXT, " +
                    "`maskedAccountNumber` TEXT NOT NULL, `accountNumberHash` TEXT NOT NULL, " +
                    "`baseCurrency` TEXT, `effectiveAtMs` INTEGER NOT NULL, PRIMARY KEY(`accountId`))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_accounts_snapshotId` ON `portfolio_accounts` (`snapshotId`)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_portfolio_accounts_ownerId_accountNumberHash` " +
                    "ON `portfolio_accounts` (`ownerId`, `accountNumberHash`)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `portfolio_positions` (`positionId` TEXT NOT NULL, " +
                    "`ownerId` TEXT NOT NULL, `snapshotId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                    "`symbol` TEXT, `exchange` TEXT, `mic` TEXT, `securityName` TEXT NOT NULL, " +
                    "`securityType` TEXT NOT NULL, `quantity` TEXT NOT NULL, `direction` TEXT NOT NULL, " +
                    "`marketPrice` TEXT, `marketPriceCurrency` TEXT, `bookValueCad` TEXT, " +
                    "`bookValueCadCurrency` TEXT, `bookValueMarket` TEXT, `bookValueMarketCurrency` TEXT, " +
                    "`marketValue` TEXT, `marketValueCurrency` TEXT, `unrealizedReturn` TEXT, " +
                    "`unrealizedReturnCurrency` TEXT, `effectiveAtMs` INTEGER NOT NULL, " +
                    "`warnings` TEXT NOT NULL, PRIMARY KEY(`positionId`))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_positions_accountId` ON `portfolio_positions` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_positions_symbol` ON `portfolio_positions` (`symbol`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_positions_mic` ON `portfolio_positions` (`mic`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_positions_ownerId` ON `portfolio_positions` (`ownerId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `portfolio_cash_balances` (`cashBalanceId` TEXT NOT NULL, " +
                    "`ownerId` TEXT NOT NULL, `snapshotId` TEXT NOT NULL, `accountId` TEXT NOT NULL, " +
                    "`currency` TEXT NOT NULL, `amount` TEXT NOT NULL, `effectiveAtMs` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`cashBalanceId`))"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_portfolio_cash_balances_accountId_currency` " +
                    "ON `portfolio_cash_balances` (`accountId`, `currency`)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_cash_balances_ownerId` ON `portfolio_cash_balances` (`ownerId`)")
        }
    }
}

