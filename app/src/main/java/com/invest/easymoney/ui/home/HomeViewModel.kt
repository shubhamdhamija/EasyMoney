package com.invest.easymoney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Constants
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StockRepository,
    private val aiRepository: AiInsightRepository
) : ViewModel() {

    private val _topStocksState = MutableStateFlow<Resource<List<Stock>>>(Resource.Loading)
    val topStocksState: StateFlow<Resource<List<Stock>>> = _topStocksState

    val gainers: StateFlow<List<Stock>> = _topStocksState
        .map { res -> (res as? Resource.Success)?.data?.sortedByDescending { it.changePercent }?.take(5) ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val losers: StateFlow<List<Stock>> = _topStocksState
        .map { res -> (res as? Resource.Success)?.data?.sortedBy { it.changePercent }?.take(5) ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trending: StateFlow<List<Stock>> = _topStocksState
        .map { res ->
            (res as? Resource.Success)?.data
                ?.filter { it.symbol in Constants.TRENDING_STOCKS }
                ?.take(5) ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null = not requested yet; Loading/Success/Error = in-flight or done */
    private val _aiPicksState = MutableStateFlow<Resource<List<AiPick>>?>(null)
    val aiPicksState: StateFlow<Resource<List<AiPick>>?> = _aiPicksState

    private var aiPicksJob: Job? = null

    init {
        loadStocks()
    }

    fun loadStocks() {
        viewModelScope.launch {
            _topStocksState.value = Resource.Loading
            _topStocksState.value = repository.getTopStocks()
        }
    }

    fun loadAiPicks() {
        if (aiPicksJob?.isActive == true) return
        aiPicksJob = viewModelScope.launch {
            _aiPicksState.value = Resource.Loading
            val stocks = (topStocksState.value as? Resource.Success)?.data
            if (stocks.isNullOrEmpty()) {
                _aiPicksState.value = Resource.Error("Load stock data first")
                return@launch
            }
            val topStocks = stocks.sortedByDescending { it.changePercent }.take(8)
            _aiPicksState.value = aiRepository.getAiPicks(topStocks)
        }
    }

    fun dismissAiPicks() {
        _aiPicksState.value = null
    }
}

