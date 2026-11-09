package com.invest.easymoney.ui.portfolio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.PortfolioImportPreview
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioReplacementDecision
import com.invest.easymoney.domain.portfolio.PortfolioRepository
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import com.invest.easymoney.domain.portfolio.PortfolioSnapshot
import com.invest.easymoney.domain.portfolio.PortfolioSummary
import com.invest.easymoney.domain.portfolio.PortfolioSummaryCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PortfolioImportState {
    data object Idle : PortfolioImportState
    data class ReadingFile(val fileName: String?) : PortfolioImportState
    data class Preview(
        val preview: PortfolioImportPreview,
        val selectedReplacementDecision: PortfolioReplacementDecision
    ) : PortfolioImportState
    data class Importing(val preview: PortfolioImportPreview) : PortfolioImportState
    data class Success(val snapshot: PortfolioSnapshot) : PortfolioImportState
    data class Error(val message: String, val retryable: Boolean) : PortfolioImportState
}

/** Holds only masked, normalized data. Raw CSV bytes and account numbers never enter this state. */
@HiltViewModel
class PortfolioImportViewModel @Inject constructor(
    private val repository: PortfolioRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PortfolioImportState>(PortfolioImportState.Idle)
    val state: StateFlow<PortfolioImportState> = _state.asStateFlow()

    private var lastUri: Uri? = null
    private var job: Job? = null

    fun selectFile(uri: Uri) {
        if (job?.isActive == true) return
        lastUri = uri
        _state.value = PortfolioImportState.ReadingFile(null)
        job = viewModelScope.launch {
            _state.value = when (val result = repository.previewWealthsimpleImport(uri)) {
                is NetworkResult.Success -> {
                    val p = result.data
                    PortfolioImportState.Preview(
                        p,
                        if (p.duplicateStatus == com.invest.easymoney.domain.portfolio.DuplicateStatus.OLDER_SNAPSHOT)
                            PortfolioReplacementDecision.CANCEL else PortfolioReplacementDecision.REPLACE
                    )
                }
                is NetworkResult.Error -> PortfolioImportState.Error(
                    result.throwable.message ?: "Unable to read the portfolio file.", retryable = true
                )
            }
        }
    }

    fun chooseReplacementDecision(decision: PortfolioReplacementDecision) =
        _state.update { s -> if (s is PortfolioImportState.Preview) s.copy(selectedReplacementDecision = decision) else s }

    fun confirmImport() {
        val current = _state.value as? PortfolioImportState.Preview ?: return
        if (job?.isActive == true) return
        _state.value = PortfolioImportState.Importing(current.preview)
        job = viewModelScope.launch {
            _state.value = when (val result = repository.confirmImport(current.preview, current.selectedReplacementDecision)) {
                is NetworkResult.Success -> PortfolioImportState.Success(result.data)
                is NetworkResult.Error -> PortfolioImportState.Error(
                    result.throwable.message ?: "Unable to import the portfolio.", retryable = false
                )
            }
        }
    }

    fun cancelImport() {
        job?.cancel()
        lastUri = null
        _state.value = PortfolioImportState.Idle
    }

    fun retry() {
        val uri = lastUri ?: return
        _state.value = PortfolioImportState.Idle
        selectFile(uri)
    }

    fun clearError() {
        if (_state.value is PortfolioImportState.Error) _state.value = PortfolioImportState.Idle
    }

    fun dismissSuccess() {
        if (_state.value is PortfolioImportState.Success) _state.value = PortfolioImportState.Idle
    }
}

enum class HoldingSort { MARKET_VALUE, SYMBOL, UNREALIZED_RETURN, NAME }

data class PortfolioFilters(
    val accountId: String? = null,
    val currency: String? = null,
    val securityType: PortfolioSecurityType? = null,
    val sort: HoldingSort = HoldingSort.MARKET_VALUE
)

data class PortfolioUiState(
    val portfolio: Portfolio? = null,
    val summary: PortfolioSummary? = null,
    val holdings: List<PortfolioPosition> = emptyList(),
    val filters: PortfolioFilters = PortfolioFilters()
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository
) : ViewModel() {

    private val filters = MutableStateFlow(PortfolioFilters())

    val uiState: StateFlow<PortfolioUiState> = combine(repository.observeActivePortfolio(), filters) { portfolio, f ->
        if (portfolio == null) {
            PortfolioUiState(filters = f)
        } else {
            val filtered = portfolio.positions
                .filter { f.accountId == null || it.accountId == f.accountId }
                .filter { f.currency == null || it.marketValueCurrency?.value == f.currency }
                .filter { f.securityType == null || it.securityType == f.securityType }
            PortfolioUiState(portfolio, PortfolioSummaryCalculator.calculate(portfolio), sorted(filtered, f.sort), f)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioUiState())

    fun setAccount(id: String?) = filters.update { it.copy(accountId = id) }
    fun setCurrency(currency: String?) = filters.update { it.copy(currency = currency) }
    fun setSecurityType(type: PortfolioSecurityType?) = filters.update { it.copy(securityType = type) }
    fun setSort(sort: HoldingSort) = filters.update { it.copy(sort = sort) }

    fun deleteAll() {
        viewModelScope.launch { repository.deletePortfolioData() }
    }

    // Values are only compared within the same currency; currencies are grouped, never mixed.
    private fun sorted(list: List<PortfolioPosition>, sort: HoldingSort): List<PortfolioPosition> = when (sort) {
        HoldingSort.MARKET_VALUE -> list.sortedWith(
            compareBy<PortfolioPosition> { it.marketValueCurrency?.value.orEmpty() }
                .thenByDescending { it.marketValue }
        )
        HoldingSort.UNREALIZED_RETURN -> list.sortedWith(
            compareBy<PortfolioPosition> { it.unrealizedReturnCurrency?.value.orEmpty() }
                .thenByDescending { it.unrealizedReturn }
        )
        HoldingSort.SYMBOL -> list.sortedBy { it.symbol.orEmpty().uppercase() }
        HoldingSort.NAME -> list.sortedBy { it.securityName.uppercase() }
    }
}

