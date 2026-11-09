package com.invest.easymoney.di

import com.invest.easymoney.data.api.AgentAccessTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevelopmentAgentAccessTokenProvider @Inject constructor() : AgentAccessTokenProvider {
    override suspend fun getAccessToken(): String? {
        // Development-only provider for local backend access. Production builds must use a secure
        // authenticated token source, not a hard-coded value.
        return null
    }
}

