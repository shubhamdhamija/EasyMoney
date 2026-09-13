package com.invest.easymoney.ui.home

import com.invest.easymoney.domain.model.AiPick

sealed interface AiPicksState {
    data object Idle : AiPicksState
    data object Loading : AiPicksState
    data class Success(val picks: List<AiPick>) : AiPicksState
    data class Error(val message: String) : AiPicksState
}

