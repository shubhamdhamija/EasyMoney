package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.AgentResearchResult
import com.invest.easymoney.domain.model.NetworkResult

interface AgentResearchRepository {
    suspend fun startResearch(symbol: String): NetworkResult<AgentResearchResult>

    suspend fun submitApproval(
        threadId: String,
        approvalId: String,
        approved: Boolean,
        reviewerNote: String? = null
    ): NetworkResult<AgentResearchResult>

    suspend fun retryResearch(symbol: String): NetworkResult<AgentResearchResult>
}

