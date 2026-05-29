package com.invest.easymoney.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val repository: StockRepository,
    private val aiRepository: AiInsightRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val symbol: String = checkNotNull(savedStateHandle["symbol"])

    private val _stockState = MutableStateFlow<Resource<Stock>>(Resource.Loading)
    val stockState: StateFlow<Resource<Stock>> = _stockState

    private val _newsState = MutableStateFlow<Resource<List<News>>>(Resource.Loading)
    val newsState: StateFlow<Resource<List<News>>> = _newsState

    private val _insightState = MutableStateFlow<Resource<StockInsight>>(Resource.Loading)
    val insightState: StateFlow<Resource<StockInsight>> = _insightState

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist: StateFlow<Boolean> = _isInWatchlist

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private var loadingJob: Job? = null

    init {
        loadDetail()
    }

    fun loadDetail() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _stockState.value = Resource.Loading
            _newsState.value = Resource.Loading
            _insightState.value = Resource.Loading

            // Load stock, news, and watchlist status concurrently
            val stockDeferred = async { repository.getStockDetail(symbol) }
            val newsDeferred = async { repository.getNews(symbol) }
            val watchlistDeferred = async { repository.isInWatchlist(symbol) }

            val stockResult = stockDeferred.await()
            val newsResult = newsDeferred.await()
            _isInWatchlist.value = watchlistDeferred.await()

            _stockState.value = stockResult
            _newsState.value = newsResult

            // Fetch AI insight after stock+news data is available
            val stock = (stockResult as? Resource.Success)?.data
            val newsList = (newsResult as? Resource.Success)?.data ?: emptyList()

            _insightState.value = if (stock != null) {
                aiRepository.getStockInsight(symbol, stock, newsList)
            } else {
                Resource.Error("AI insight requires stock data")
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                repository.removeFromWatchlist(symbol)
                _isInWatchlist.value = false
                _snackbarMessage.value = "$symbol removed from watchlist"
            } else {
                repository.addToWatchlist(symbol)
                _isInWatchlist.value = true
                _snackbarMessage.value = "$symbol added to watchlist"
            }
        }
    }

    fun setAlert(percentage: Float, type: AlertType) {
        viewModelScope.launch {
            repository.addAlert(Alert(symbol = symbol, percentage = percentage, type = type))
            _snackbarMessage.value = "Alert set for $symbol: ${if (type == AlertType.INCREASE) "+" else "-"}${percentage}%"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

