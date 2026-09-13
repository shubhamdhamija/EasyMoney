package com.invest.easymoney.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.di.DeviceId
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
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
    private val backendRepository: BackendRepository,
    savedStateHandle: SavedStateHandle,
    @DeviceId private val deviceId: String
) : ViewModel() {

    val symbol: String = checkNotNull(savedStateHandle["symbol"])

    private val _stockState = MutableStateFlow<NetworkResult<Stock>?>(null)
    val stockState: StateFlow<NetworkResult<Stock>?> = _stockState

    private val _newsState = MutableStateFlow<NetworkResult<List<News>>?>(null)
    val newsState: StateFlow<NetworkResult<List<News>>?> = _newsState

    private val _insightState = MutableStateFlow<NetworkResult<StockInsight>?>(null)
    val insightState: StateFlow<NetworkResult<StockInsight>?> = _insightState

    private val _explainState = MutableStateFlow<NetworkResult<String>?>(null)
    val explainState: StateFlow<NetworkResult<String>?> = _explainState

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading

    private val _isExplainLoading = MutableStateFlow(false)
    val isExplainLoading: StateFlow<Boolean> = _isExplainLoading

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
            _isLoading.value = true
            _isInsightLoading.value = true
            _stockState.value = null
            _newsState.value = null
            _insightState.value = null

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
            val stock = (stockResult as? NetworkResult.Success)?.data
            val newsList = (newsResult as? NetworkResult.Success)?.data ?: emptyList()

            _insightState.value = if (stock != null) {
                aiRepository.getStockInsight(symbol, stock, newsList)
            } else {
                NetworkResult.Error(IllegalStateException("AI insight requires stock data"))
            }

            _isInsightLoading.value = false
            _isLoading.value = false
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                repository.removeFromWatchlist(symbol)
                _isInWatchlist.value = false
                _snackbarMessage.value = "$symbol removed from watchlist"
                // Notify community (fire-and-forget)
                launch { backendRepository.notifyWatchlistChange(deviceId, symbol, "remove") }
            } else {
                repository.addToWatchlist(symbol)
                _isInWatchlist.value = true
                _snackbarMessage.value = "$symbol added to watchlist"
                // Notify community (fire-and-forget)
                launch { backendRepository.notifyWatchlistChange(deviceId, symbol, "add") }
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

    fun explainMove() {
        if (_isExplainLoading.value) return
        viewModelScope.launch {
            _isExplainLoading.value = true
            try {
                _explainState.value = backendRepository.explainMove(symbol)
            } finally {
                _isExplainLoading.value = false
            }
        }
    }

    fun dismissExplain() {
        _explainState.value = null
    }
}

