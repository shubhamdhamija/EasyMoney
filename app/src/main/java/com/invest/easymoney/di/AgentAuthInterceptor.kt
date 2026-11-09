package com.invest.easymoney.di

import com.invest.easymoney.data.api.AgentAccessTokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentAuthInterceptor @Inject constructor(
    private val tokenProvider: AgentAccessTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = kotlinx.coroutines.runBlocking { tokenProvider.getAccessToken() }
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}

