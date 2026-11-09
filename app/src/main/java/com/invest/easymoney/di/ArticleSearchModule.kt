package com.invest.easymoney.di

import com.invest.easymoney.data.api.ArticleApiService
import com.invest.easymoney.data.repository.ArticleRepositoryImpl
import com.invest.easymoney.domain.repository.ArticleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArticleSearchModule {
    @Binds
    @Singleton
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    companion object {
        @Provides
        @Singleton
        fun provideArticleApiService(@BackendRetrofit retrofit: Retrofit): ArticleApiService =
            retrofit.create(ArticleApiService::class.java)
    }
}
