package com.invest.easymoney.ui.home

import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.util.Resource

data class HomeUiState(
    val stocks: Resource<List<Stock>> = Resource.Loading,
    val aiPicks: AiPicksState = AiPicksState.Idle,
    val search: SearchUiState = SearchUiState(),
    val popularSymbols: List<String> = emptyList()
)

