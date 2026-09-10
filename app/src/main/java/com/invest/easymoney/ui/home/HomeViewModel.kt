package com.invest.easymoney.ui.home

import android.util.Log
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

private const val TAG = "HomeViewModel"

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

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _searchState = MutableStateFlow<Resource<List<com.invest.easymoney.domain.model.StockSearchResult>>>(Resource.Success(emptyList()))
    val searchState = _searchState

    private val _popularSymbols = MutableStateFlow<List<String>>(emptyList())
    val popularSymbols: StateFlow<List<String>> = _popularSymbols

    init {
        loadStocks()
    }

    fun loadStocks() {
        viewModelScope.launch {
            Log.d(TAG, "Loading stocks...")
            _topStocksState.value = Resource.Loading
            val result = repository.getTopStocks()
            _topStocksState.value = result
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Stocks loaded: ${result.data?.size} stocks")
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to load stocks: ${result.message}")
                }
                is Resource.Loading -> {
                    Log.d(TAG, "Stocks loading...")
                }
            }
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

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun searchSymbols() {
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _searchState.value = Resource.Loading
            _searchState.value = repository.searchSymbols(q)
        }
    }

    fun loadPopularSymbols() {
        viewModelScope.launch {
            when (val result = repository.getPopularStocksFromApi()) {
                is Resource.Success -> _popularSymbols.value = result.data ?: emptyList()
                else -> _popularSymbols.value = Constants.POPULAR_STOCKS
            }
        }
    }
}
