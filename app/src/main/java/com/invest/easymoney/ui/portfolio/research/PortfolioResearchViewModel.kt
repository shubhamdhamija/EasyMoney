package com.invest.easymoney.ui.portfolio.research

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchException
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchReport
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchRepository
import com.invest.easymoney.domain.portfolio.research.ResearchConsentHandle
import com.invest.easymoney.domain.portfolio.research.ResearchDataCategory
import com.invest.easymoney.domain.portfolio.research.ResearchPortfolioTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface PortfolioResearchUiState {
    data object Idle : PortfolioResearchUiState
    /** The consent dialog is showing. Nothing has been sent anywhere. */
    data class ConsentRequired(val target: ResearchPortfolioTarget) : PortfolioResearchUiState
    data object CreatingConsent : PortfolioResearchUiState
    data object Researching : PortfolioResearchUiState
    data class Success(val report: PortfolioResearchReport) : PortfolioResearchUiState
    data class Error(
        val message: String,
        val retryable: Boolean,
        val requestId: String? = null,
        val target: ResearchPortfolioTarget? = null
    ) : PortfolioResearchUiState
}

/**
 * Research starts only from an explicit user action after an explicit consent confirmation.
 * Nothing is requested on screen open, rotation or recomposition. State holds no consent ID,
 * no account numbers and nothing is written to SavedStateHandle. Consent is revoked as soon as
 * a run finishes (best effort; it also expires server-side after about 15 minutes).
 */
@HiltViewModel
class PortfolioResearchViewModel @Inject constructor(
    private val repository: PortfolioResearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PortfolioResearchUiState>(PortfolioResearchUiState.Idle)
    val state: StateFlow<PortfolioResearchUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** The "Research my portfolio" button. Only opens the dialog. */
    fun requestResearch(target: ResearchPortfolioTarget) {
        if (job?.isActive == true) return
        _state.value = PortfolioResearchUiState.ConsentRequired(target)
    }

    fun cancelConsent() {
        if (_state.value is PortfolioResearchUiState.ConsentRequired) _state.value = PortfolioResearchUiState.Idle
    }

    fun confirmConsent(categories: Set<ResearchDataCategory>) {
        val current = _state.value as? PortfolioResearchUiState.ConsentRequired ?: return
        if (job?.isActive == true) return
        // Accounts and holdings are the minimum needed for any analysis.
        val chosen = categories + ResearchDataCategory.ACCOUNTS + ResearchDataCategory.POSITIONS
        start(current.target, chosen)
    }

    fun retry() {
        val target = (_state.value as? PortfolioResearchUiState.Error)?.target ?: return
        if (job?.isActive == true) return
        _state.value = PortfolioResearchUiState.ConsentRequired(target)
    }

    /** Back to idle (dismiss result/error, or the active source changed). Ignored mid-run. */
    fun reset() {
        if (job?.isActive == true) return
        _state.value = PortfolioResearchUiState.Idle
    }

    private fun start(target: ResearchPortfolioTarget, categories: Set<ResearchDataCategory>) {
        _state.value = PortfolioResearchUiState.CreatingConsent
        job = viewModelScope.launch {
            var handle: ResearchConsentHandle? = null
            try {
                when (val consent = repository.createResearchConsent(target, categories)) {
                    is NetworkResult.Error -> {
                        _state.value = errorState(consent.throwable, target)
                        return@launch
                    }
                    is NetworkResult.Success -> handle = consent.data
                }
                _state.value = PortfolioResearchUiState.Researching
                val active = handle ?: return@launch
                _state.value = when (val result = repository.startPortfolioResearch(active, categories)) {
                    is NetworkResult.Success -> PortfolioResearchUiState.Success(result.data)
                    is NetworkResult.Error -> errorState(result.throwable, target)
                }
            } finally {
                handle?.let { h -> withContext(NonCancellable) { repository.revokeResearchConsent(h) } }
            }
        }
    }

    private fun errorState(error: Throwable, target: ResearchPortfolioTarget): PortfolioResearchUiState.Error {
        val e = error as? PortfolioResearchException
        return PortfolioResearchUiState.Error(
            message = e?.message ?: "Portfolio research could not be completed. Please try again.",
            retryable = e?.retryable ?: true,
            requestId = e?.requestId,
            target = target
        )
    }
}
