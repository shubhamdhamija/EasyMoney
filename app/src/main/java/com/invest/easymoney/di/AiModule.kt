package com.invest.easymoney.di

import com.google.gson.Gson
import com.invest.easymoney.data.api.OpenAiApiService
import com.invest.easymoney.data.repository.AiInsightRepositoryImpl
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.util.Constants
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiInsightRepository(impl: AiInsightRepositoryImpl): AiInsightRepository

    companion object {

        @Provides
        @Singleton
        @OpenAiClient
        fun provideOpenAiOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Pollinations.ai requires no API key
                val request = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // LLM can be slow
            .build()

        @Provides
        @Singleton
        @OpenAiRetrofit
        fun provideOpenAiRetrofit(@OpenAiClient client: OkHttpClient): Retrofit = Retrofit.Builder()
            .baseUrl(Constants.OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        @Provides
        @Singleton
        fun provideOpenAiApiService(@OpenAiRetrofit retrofit: Retrofit): OpenAiApiService =
            retrofit.create(OpenAiApiService::class.java)

        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()
    }
}
