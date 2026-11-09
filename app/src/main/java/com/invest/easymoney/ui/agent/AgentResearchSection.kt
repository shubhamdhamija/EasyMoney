package com.invest.easymoney.ui.agent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AgentResearchSection(
    state: AgentResearchUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Box(modifier = Modifier.animateContentSize()) {
        when (state) {
            is AgentResearchUiState.Idle -> Unit
            is AgentResearchUiState.Loading -> AgentResearchLoadingCard(symbol = state.symbol, onCancel = onCancel)
            is AgentResearchUiState.Success -> AgentResearchCard(report = state.report, onDismiss = onDismiss)
            is AgentResearchUiState.Error -> AgentErrorCard(
                message = state.message,
                requestId = state.requestId,
                retryable = state.retryable,
                onRetry = if (state.retryable) onRetry else null,
                onDismiss = onClearError
            )
            is AgentResearchUiState.ApprovalRequired -> AgentApprovalSheet(
                approval = state.approval,
                isSubmitting = state.isSubmitting,
                onDismiss = onDismiss,
                onApprove = onApprove,
                onReject = onReject
            )
        }
    }
}

