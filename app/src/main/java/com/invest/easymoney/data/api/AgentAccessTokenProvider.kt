package com.invest.easymoney.data.api

interface AgentAccessTokenProvider {
    suspend fun getAccessToken(): String?
}

