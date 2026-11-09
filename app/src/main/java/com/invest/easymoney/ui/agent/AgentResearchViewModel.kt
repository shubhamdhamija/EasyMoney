package com.invest.easymoney.ui.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.AgentResearchResult
import com.invest.easymoney.domain.model.AgentResearchStage
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.repository.AgentResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.invest.easymoney.data.repository.AgentResearchException

@HiltViewModel
class AgentResearchViewModel @Inject constructor(
    private val repository: AgentResearchRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow<AgentResearchUiState>(AgentResearchUiState.Idle)
    val state: StateFlow<AgentResearchUiState> = _state

    private var pendingStartJob: Job? = null
    private var currentSymbol: String = savedStateHandle["symbol"] ?: ""

    fun startAgentResearch(symbol: String) {
        val normalized = symbol.trim().uppercase()
        if (normalized.isBlank()) return
        if (pendingStartJob?.isActive == true) return
        currentSymbol = normalized
        pendingStartJob = viewModelScope.launch {
            _state.value = AgentResearchUiState.Loading(symbol = normalized, stage = AgentResearchStage.RESEARCHING_AVAILABLE_INFORMATION)
            when (val result = repository.startResearch(normalized)) {
                is NetworkResult.Success -> {
                    when (val outcome = result.data) {
                        is AgentResearchResult.Completed -> {
                            _state.value = AgentResearchUiState.Success(outcome.report)
                        }
                        is AgentResearchResult.ApprovalRequired -> {
                            _state.value = AgentResearchUiState.ApprovalRequired(
                                threadId = outcome.threadId,
                                approval = outcome.approval,
                                requestId = outcome.requestId,
                                isSubmitting = false,
                            )
                        }
                        is AgentResearchResult.Failed -> {
                            _state.value = AgentResearchUiState.Error(
                                message = outcome.message,
                                requestId = outcome.requestId,
                                retryable = outcome.retryable,
                            )
                        }
                    }
                }
                is NetworkResult.Error -> {
                    val throwable = result.throwable
                    if (throwable is CancellationException) throw throwable
                    val message = throwable.message ?: "AI research could not be completed."
                    val requestId = (throwable as? AgentResearchException)?.requestId
                    _state.value = AgentResearchUiState.Error(
                        message = message,
                        requestId = requestId,
                        retryable = true,
                    )
                }
            }
        }
    }

    fun approvePendingAction() {
        val current = (_state.value as? AgentResearchUiState.ApprovalRequired) ?: return
        if (current.isSubmitting) return
        _state.value = current.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                val result = repository.submitApproval(
                    threadId = current.threadId,
                    approvalId = current.approval.approvalId,
                    approved = true,
                )
                when (result) {
                    is NetworkResult.Success -> when (val outcome = result.data) {
                        is AgentResearchResult.Completed -> _state.value = AgentResearchUiState.Success(outcome.report)
                        is AgentResearchResult.ApprovalRequired -> _state.value = AgentResearchUiState.ApprovalRequired(
                            threadId = outcome.threadId,
                            approval = outcome.approval,
                            requestId = outcome.requestId,
                            isSubmitting = false,
                        )
                        is AgentResearchResult.Failed -> _state.value = AgentResearchUiState.Error(
                            message = outcome.message,
                            requestId = outcome.requestId,
                            retryable = outcome.retryable,
                        )
                    }
                    is NetworkResult.Error -> {
                        val throwable = result.throwable
                        if (throwable is CancellationException) throw throwable
                        val requestId = (throwable as? AgentResearchException)?.requestId
                        _state.value = AgentResearchUiState.Error(
                            message = throwable.message ?: "AI research could not be completed.",
                            requestId = requestId,
                            retryable = (throwable as? AgentResearchException)?.retryable ?: true,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AgentResearchUiState.Error(
                    message = e.message ?: "AI research could not be completed.",
                    requestId = null,
                    retryable = true,
                )
            }
        }
    }

    fun rejectPendingAction() {
        val current = (_state.value as? AgentResearchUiState.ApprovalRequired) ?: return
        if (current.isSubmitting) return
        _state.value = current.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                val result = repository.submitApproval(
                    threadId = current.threadId,
                    approvalId = current.approval.approvalId,
                    approved = false,
                )
                when (result) {
                    is NetworkResult.Success -> when (val outcome = result.data) {
                        is AgentResearchResult.Completed -> _state.value = AgentResearchUiState.Success(outcome.report)
                        is AgentResearchResult.ApprovalRequired -> _state.value = AgentResearchUiState.ApprovalRequired(
                            threadId = outcome.threadId,
                            approval = outcome.approval,
                            requestId = outcome.requestId,
                            isSubmitting = false,
                        )
                        is AgentResearchResult.Failed -> _state.value = AgentResearchUiState.Error(
                            message = outcome.message,
                            requestId = outcome.requestId,
                            retryable = outcome.retryable,
                        )
                    }
                    is NetworkResult.Error -> {
                        val throwable = result.throwable
                        if (throwable is CancellationException) throw throwable
                        val requestId = (throwable as? AgentResearchException)?.requestId
                        _state.value = AgentResearchUiState.Error(
                            message = throwable.message ?: "AI research could not be completed.",
                            requestId = requestId,
                            retryable = (throwable as? AgentResearchException)?.retryable ?: true,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AgentResearchUiState.Error(
                    message = e.message ?: "AI research could not be completed.",
                    requestId = null,
                    retryable = true,
                )
            }
        }
    }

    fun retryAgentResearch() {
        if (currentSymbol.isBlank()) return
        startAgentResearch(currentSymbol)
    }

    fun dismissAgentResearch() {
        _state.value = AgentResearchUiState.Idle
    }

    /** Cancels an in-flight research request and returns to the idle state. */
    fun cancelAgentResearch() {
        pendingStartJob?.cancel()
        pendingStartJob = null
        if (_state.value is AgentResearchUiState.Loading) {
            _state.value = AgentResearchUiState.Idle
        }
    }

    fun clearAgentError() {
        if (_state.value is AgentResearchUiState.Error) {
            _state.value = AgentResearchUiState.Idle
        }
    }
}

