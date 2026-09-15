package com.invest.easymoney.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.StockSearchResult
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "HomeViewModel"

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<StockSearchResult> = emptyList(),
    val error: String? = null
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StockRepository,
    private val aiRepository: AiInsightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var aiPicksJob: Job? = null

    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val searchResults: Flow<Resource<List<StockSearchResult>>> = searchQuery
        .debounce(500.milliseconds)
        .map(String::trim)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(Resource.Success(emptyList<StockSearchResult>()))
            } else {
                repository.searchSymbols(query)
            }
        }


    init {
        loadStocks()
        viewModelScope.launch {
            searchResults.collect(::updateSearchUi)
        }
    }

    private fun updateSearchUi(result: Resource<List<StockSearchResult>>) {
        _uiState.update { current ->
            val updatedSearch = when (result) {
                is Resource.Loading ->
                    current.search.copy(
                        isLoading = true,
                        error = null
                    )

                is Resource.Success ->
                    current.search.copy(
                        results = result.data,
                        isLoading = false,
                        error = null
                    )

                is Resource.Error ->
                    current.search.copy(
                        isLoading = false,
                        results = emptyList(),
                        error = result.message
                    )
            }
            current.copy(search = updatedSearch)
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchChanged -> setSearchQuery(intent.query)
            HomeIntent.LoadStocks -> loadStocks()
            HomeIntent.LoadAiPicks -> loadAiPicks()
            HomeIntent.DismissAiPicks -> dismissAiPicks()
        }
    }

    private fun loadStocks() {
        viewModelScope.launch {
            Log.d(TAG, "Loading stocks...")
            _uiState.update { it.copy(stocks = Resource.Loading) }
            val result = repository.getTopStocks()
            _uiState.update { it.copy(stocks = result) }
            when (result) {
                is Resource.Success -> {
                    Log.d(TAG, "Stocks loaded: ${result.data.size} stocks")
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

    private fun loadAiPicks() {
        if (aiPicksJob?.isActive == true) return
        aiPicksJob = viewModelScope.launch {
            _uiState.update { it.copy(aiPicks = AiPicksState.Loading) }
            val stocks = (_uiState.value.stocks as? Resource.Success)?.data
            if (stocks.isNullOrEmpty()) {
                _uiState.update { it.copy(aiPicks = AiPicksState.Error("Load stock data first")) }
                return@launch
            }
            val topStocks = stocks.sortedByDescending { it.changePercent }.take(8)
            _uiState.update { it.copy(aiPicks = aiRepository.getAiPicks(topStocks).toAiPicksState()) }
        }
    }

    private fun dismissAiPicks() {
        _uiState.update { it.copy(aiPicks = AiPicksState.Idle) }
    }

    private fun setSearchQuery(q: String) {
        _uiState.update { it.copy(search = it.search.copy(query = q, error = null)) }
        searchQuery.value = q
    }

    private fun Resource<List<AiPick>>.toAiPicksState(): AiPicksState = when (this) {
        is Resource.Loading -> AiPicksState.Loading
        is Resource.Success -> AiPicksState.Success(data)
        is Resource.Error -> AiPicksState.Error(message)
    }

}
