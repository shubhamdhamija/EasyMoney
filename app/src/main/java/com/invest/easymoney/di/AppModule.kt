package com.invest.easymoney.di

import android.content.Context
import android.provider.Settings
import com.invest.easymoney.data.api.AgentAccessTokenProvider
import com.invest.easymoney.data.repository.AgentResearchRepositoryImpl
import com.invest.easymoney.data.repository.BackendRepositoryImpl
import com.invest.easymoney.data.repository.StockRepositoryImpl
import com.invest.easymoney.domain.repository.AgentResearchRepository
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

    @Binds
    @Singleton
    abstract fun bindAgentAccessTokenProvider(impl: DevelopmentAgentAccessTokenProvider): AgentAccessTokenProvider

    @Binds
    @Singleton
    abstract fun bindAgentResearchRepository(impl: AgentResearchRepositoryImpl): AgentResearchRepository

    @Binds
    @Singleton
    abstract fun bindCongressRepository(
        impl: com.invest.easymoney.data.repository.CongressRepositoryImpl
    ): com.invest.easymoney.domain.repository.CongressRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(
        impl: com.invest.easymoney.data.portfolio.PortfolioRepositoryImpl
    ): com.invest.easymoney.domain.portfolio.PortfolioRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioConnectionRepository(
        impl: com.invest.easymoney.data.portfolio.connection.PortfolioConnectionRepositoryImpl
    ): com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioResearchRepository(
        impl: com.invest.easymoney.data.portfolio.research.PortfolioResearchRepositoryImpl
    ): com.invest.easymoney.domain.portfolio.research.PortfolioResearchRepository

    companion object {
        @Provides
        @Singleton
        @DeviceId
        fun provideDeviceId(@ApplicationContext context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}
