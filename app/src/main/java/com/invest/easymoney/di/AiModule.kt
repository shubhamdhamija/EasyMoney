package com.invest.easymoney.di

import com.invest.easymoney.data.repository.AiInsightRepositoryImpl
import com.invest.easymoney.domain.repository.AiInsightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** AI features are served by the EasyMoney backend (see NetworkModule). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiInsightRepository(impl: AiInsightRepositoryImpl): AiInsightRepository
}

