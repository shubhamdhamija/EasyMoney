package com.invest.easymoney.di

import android.content.Context
import android.provider.Settings
import com.invest.easymoney.data.repository.BackendRepositoryImpl
import com.invest.easymoney.data.repository.StockRepositoryImpl
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindStockRepository(impl: StockRepositoryImpl): StockRepository

    @Binds
    @Singleton
    abstract fun bindBackendRepository(impl: BackendRepositoryImpl): BackendRepository

    companion object {
        @Provides
        @Singleton
        @DeviceId
        fun provideDeviceId(@ApplicationContext context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}
