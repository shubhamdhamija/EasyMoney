package com.invest.easymoney.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val _stocksState = MutableStateFlow<Resource<List<Stock>>>(Resource.Loading)
    val stocksState: StateFlow<Resource<List<Stock>>> = _stocksState

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
            }
        }
    }
}
