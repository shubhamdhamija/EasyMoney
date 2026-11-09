package com.invest.easymoney.ui.detail

import com.invest.easymoney.domain.model.StockExplanation

sealed interface ExplainMoveUiState {
    data object Idle : ExplainMoveUiState
    data object Loading : ExplainMoveUiState

    data class Success(
        val explanation: StockExplanation
    ) : ExplainMoveUiState

    data class InsufficientEvidence(
        val explanation: StockExplanation
    ) : ExplainMoveUiState

    data class Error(
        val message: String,
        val requestId: String? = null
    ) : ExplainMoveUiState
}

