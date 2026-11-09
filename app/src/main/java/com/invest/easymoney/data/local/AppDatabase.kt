package com.invest.easymoney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.PortfolioDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.AlertEntity
import com.invest.easymoney.data.local.entity.PortfolioAccountEntity
import com.invest.easymoney.data.local.entity.PortfolioCashBalanceEntity
import com.invest.easymoney.data.local.entity.PortfolioPositionEntity
import com.invest.easymoney.data.local.entity.PortfolioSnapshotEntity
import com.invest.easymoney.data.local.entity.WatchlistEntity

    @Database(
        entities = [
            WatchlistEntity::class,
            AlertEntity::class,
            PortfolioSnapshotEntity::class,
            PortfolioAccountEntity::class,
            PortfolioPositionEntity::class,
            PortfolioCashBalanceEntity::class
        ],
        version = 3,
        exportSchema = true
    )
    abstract class AppDatabase : RoomDatabase() {
        abstract fun watchlistDao(): WatchlistDao
        abstract fun alertDao(): AlertDao
        abstract fun portfolioDao(): PortfolioDao
    }
