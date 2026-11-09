package com.invest.easymoney.di

import android.content.Context
import androidx.room.Room
import com.invest.easymoney.data.local.AppDatabase
import com.invest.easymoney.data.local.DatabaseMigrations
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "easymoney.db")
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3)
            .build()

    @Provides
    fun providePortfolioDao(db: AppDatabase): com.invest.easymoney.data.local.dao.PortfolioDao = db.portfolioDao()

    @Provides
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()
}
