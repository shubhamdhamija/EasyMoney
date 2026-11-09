package com.invest.easymoney.ui.agent

import com.invest.easymoney.domain.model.AgentResearchReport
import com.invest.easymoney.domain.model.AgentResearchStage
import com.invest.easymoney.domain.model.ToolApproval

sealed interface AgentResearchUiState {
    data object Idle : AgentResearchUiState
    data class Loading(
        val symbol: String,
        val stage: AgentResearchStage = AgentResearchStage.RESEARCHING_AVAILABLE_INFORMATION,
    ) : AgentResearchUiState

    data class ApprovalRequired(
        val threadId: String,
        val approval: ToolApproval,
        val requestId: String?,
        val isSubmitting: Boolean = false,
    ) : AgentResearchUiState

    data class Success(
        val report: AgentResearchReport,
    ) : AgentResearchUiState

    data class Error(
        val message: String,
        val requestId: String?,
        val retryable: Boolean,
    ) : AgentResearchUiState
}

