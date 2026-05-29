package com.invest.easymoney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.AlertEntity
import com.invest.easymoney.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class, AlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun alertDao(): AlertDao
}
