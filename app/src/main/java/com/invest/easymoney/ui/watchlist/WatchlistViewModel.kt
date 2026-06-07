package com.invest.easymoney.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: StockRepository,
    private val backendRepository: BackendRepository
) : ViewModel() {

    private val _stocksState = MutableStateFlow<Resource<List<Stock>>>(Resource.Loading)
    val stocksState: StateFlow<Resource<List<Stock>>> = _stocksState

    private val _earningsMap = MutableStateFlow<Map<String, Resource<EarningsReport>>>(emptyMap())
    val earningsMap: StateFlow<Map<String, Resource<EarningsReport>>> = _earningsMap

    private val _upcomingEarnings = MutableStateFlow<Resource<List<UpcomingEarning>>>(Resource.Loading)
    val upcomingEarnings: StateFlow<Resource<List<UpcomingEarning>>> = _upcomingEarnings

    private val earningsJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            repository.getWatchlistSymbols()
                .distinctUntilChanged()
                .collect { symbols ->
                    _stocksState.value = Resource.Loading
                    _stocksState.value = if (symbols.isEmpty()) {
                        Resource.Success(emptyList())
                    } else {
                        repository.fetchStocksForSymbols(symbols)
                    }

                    // Prune earnings for removed symbols
                    val symbolSet = symbols.toSet()
                    val removed = _earningsMap.value.keys - symbolSet
                    if (removed.isNotEmpty()) {
                        removed.forEach { sym -> earningsJobs[sym]?.cancel(); earningsJobs.remove(sym) }
                        _earningsMap.update { current -> current.filterKeys { it in symbolSet } }
                    }

                    // Load earnings for new symbols
                    symbols.forEach { symbol ->
                        if (!_earningsMap.value.containsKey(symbol)) {
                            earningsJobs[symbol]?.cancel()
                            earningsJobs[symbol] = launch {
                                _earningsMap.update { it + (symbol to Resource.Loading) }
                                val result = backendRepository.getEarningsReport(symbol)
                                _earningsMap.update { it + (symbol to result) }
                            }
                        }
                    }

                    // Load upcoming earnings for all watchlisted symbols
                    if (symbols.isNotEmpty()) {
                        launch {
                            _upcomingEarnings.value = Resource.Loading
                            _upcomingEarnings.value = backendRepository.getUpcomingEarnings(symbols)
                        }
                    } else {
                        _upcomingEarnings.value = Resource.Success(emptyList())
                    }
                }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = (_stocksState.value as? Resource.Success)?.data?.map { it.symbol } ?: return@launch
            if (current.isNotEmpty()) {
                _stocksState.value = Resource.Loading
                _stocksState.value = repository.fetchStocksForSymbols(current)

                // Reload all earnings
                earningsJobs.values.forEach { it.cancel() }
                earningsJobs.clear()
                _earningsMap.value = emptyMap()
                current.forEach { symbol ->
                    earningsJobs[symbol] = launch {
                        _earningsMap.update { it + (symbol to Resource.Loading) }
                        val result = backendRepository.getEarningsReport(symbol)
                        _earningsMap.update { it + (symbol to result) }
                    }
                }

                launch {
                    _upcomingEarnings.value = Resource.Loading
                    _upcomingEarnings.value = backendRepository.getUpcomingEarnings(current)
                }
            }
        }
    }
}

