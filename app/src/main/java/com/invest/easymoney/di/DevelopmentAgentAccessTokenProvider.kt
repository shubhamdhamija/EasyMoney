package com.invest.easymoney.di

import com.invest.easymoney.BuildConfig
import com.invest.easymoney.data.api.AgentAccessTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Development-only token source. The JWT comes from the git-ignored `local.properties`
 * (AGENT_DEV_JWT) and is only populated for debug builds; release builds always get an empty
 * value and therefore no token. The token is never logged.
 */
@Singleton
class DevelopmentAgentAccessTokenProvider @Inject constructor() : AgentAccessTokenProvider {
    override suspend fun getAccessToken(): String? =
        if (BuildConfig.DEBUG) BuildConfig.DEV_AGENT_JWT.takeIf { it.isNotBlank() } else null
}
