package com.invest.easymoney.ui.portfolio.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.data.portfolio.connection.Messages
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.connection.ConnectedPortfolio
import com.invest.easymoney.domain.portfolio.connection.ConnectionStatus
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnection
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionException
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds only safe state. No provider secret, user ID or raw brokerage payload ever enters it. */
sealed interface PortfolioConnectionState {
    data object Idle : PortfolioConnectionState
    data object CreatingSession : PortfolioConnectionState
    data class OpeningPortal(val sessionId: String, val portalUri: String) : PortfolioConnectionState {
        override fun toString() = "OpeningPortal(<redacted>)"
    }
    data class WaitingForReturn(val sessionId: String) : PortfolioConnectionState {
        override fun toString() = "WaitingForReturn(<redacted>)"
    }
    data object CompletingConnection : PortfolioConnectionState
    data class Syncing(val connectionId: String) : PortfolioConnectionState {
        override fun toString() = "Syncing(<redacted>)"
    }
    data class Connected(val connection: PortfolioConnection, val portfolio: Portfolio?) : PortfolioConnectionState
    data class ReconnectRequired(val connection: PortfolioConnection) : PortfolioConnectionState
    data class Error(val message: String, val requestId: String?, val retryable: Boolean) : PortfolioConnectionState
}

@HiltViewModel
class PortfolioConnectionViewModel @Inject constructor(
    private val repository: PortfolioConnectionRepository,
    private val deepLinks: PortfolioDeepLinkHandler
) : ViewModel() {

    private val _state = MutableStateFlow<PortfolioConnectionState>(PortfolioConnectionState.Idle)
    val state: StateFlow<PortfolioConnectionState> = _state.asStateFlow()

    /** Latest synchronized portfolio (in memory). Rendered with the same components as CSV. */
    val connectedPortfolio: StateFlow<ConnectedPortfolio?> = repository.connectedPortfolio

    private var job: Job? = null

    init {
        viewModelScope.launch {
            deepLinks.callback.filterNotNull().collect { callback -> handleCallback(callback) }
        }
        restore()
    }

    private val busy: Boolean
        get() = job?.isActive == true || when (_state.value) {
            PortfolioConnectionState.CreatingSession,
            PortfolioConnectionState.CompletingConnection,
            is PortfolioConnectionState.Syncing -> true
            else -> false
        }

    /** Quiet load of an existing connection. Never surfaces an error and never opens a portal. */
    private fun restore() {
        if (repository.connectedPortfolio.value != null) {
            publishFromRepository(); return
        }
        job = viewModelScope.launch {
            val connections = (repository.getConnections() as? NetworkResult.Success)?.data ?: return@launch
            val active = connections.firstOrNull { it.status != ConnectionStatus.DISCONNECTED } ?: return@launch
            if (active.status == ConnectionStatus.RECONNECT_REQUIRED) {
                _state.value = PortfolioConnectionState.ReconnectRequired(active); return@launch
            }
            when (val loaded = repository.loadStoredPortfolio(active.connectionId)) {
                is NetworkResult.Success -> publish(loaded.data)
                is NetworkResult.Error -> Unit
            }
        }
    }

    /** Explicit user action only. Recomposition and rotation never start a second session. */
    fun connectSandbox() {
        if (busy) return
        when (_state.value) {
            is PortfolioConnectionState.OpeningPortal, is PortfolioConnectionState.WaitingForReturn -> return
            else -> Unit
        }
        _state.value = PortfolioConnectionState.CreatingSession
        job = viewModelScope.launch {
            when (val result = repository.createSandboxConnectionSession()) {
                is NetworkResult.Success -> openPortal(result.data.sessionId, result.data.portalUrl)
                is NetworkResult.Error -> fail(result.throwable)
            }
        }
    }

    fun reconnect() {
        if (busy) return
        val connection = currentConnection() ?: return
        _state.value = PortfolioConnectionState.CreatingSession
        job = viewModelScope.launch {
            when (val result = repository.createReconnectSession(connection.connectionId)) {
                is NetworkResult.Success -> openPortal(result.data.sessionId, result.data.portalUrl)
                is NetworkResult.Error -> fail(result.throwable)
            }
        }
    }

    private fun openPortal(sessionId: String, portalUrl: String) {
        deepLinks.registerPending(sessionId)
        _state.value = PortfolioConnectionState.OpeningPortal(sessionId, portalUrl)
    }

    /** Called once by the UI right after the Custom Tab was launched. */
    fun onPortalOpened() {
        val s = _state.value as? PortfolioConnectionState.OpeningPortal ?: return
        _state.value = PortfolioConnectionState.WaitingForReturn(s.sessionId)
    }

    private fun handleCallback(callback: PortalCallback) {
        if (!deepLinks.consume(callback)) return // consumed once
        when (callback) {
            is PortalCallback.Returned -> onConnectionRedirect(callback.sessionId)
            is PortalCallback.Canceled -> setError(Messages.CANCELED, null, true)
            is PortalCallback.Failed -> setError(Messages.INCOMPLETE, null, true)
        }
    }

    /** Redirect hint received. The backend verifies the connection with SnapTrade. */
    fun onConnectionRedirect(sessionId: String) {
        // The session ID was already validated against the pending session by the deep-link handler.
        if (_state.value == PortfolioConnectionState.CompletingConnection) return
        completeConnection(sessionId)
    }

    private fun completeConnection(sessionId: String) {
        _state.value = PortfolioConnectionState.CompletingConnection
        job = viewModelScope.launch {
            when (val result = repository.completeSandboxConnection(sessionId)) {
                is NetworkResult.Success -> publish(result.data)
                is NetworkResult.Error -> fail(result.throwable)
            }
        }
    }

    fun completeConnection() {
        val s = _state.value as? PortfolioConnectionState.WaitingForReturn ?: return
        completeConnection(s.sessionId)
    }

    fun sync() {
        if (busy) return
        val connection = currentConnection() ?: return
        _state.value = PortfolioConnectionState.Syncing(connection.connectionId)
        job = viewModelScope.launch {
            when (val result = repository.syncConnection(connection.connectionId)) {
                is NetworkResult.Success -> publish(result.data)
                is NetworkResult.Error -> fail(result.throwable, connection)
            }
        }
    }

    /** The UI must confirm with the user first. Never retried automatically. */
    fun disconnect() {
        if (busy) return
        val connection = currentConnection() ?: return
        job = viewModelScope.launch {
            when (val result = repository.disconnect(connection.connectionId)) {
                is NetworkResult.Success -> _state.value = PortfolioConnectionState.Idle
                is NetworkResult.Error -> fail(result.throwable)
            }
        }
    }

    fun deleteSynchronizedData() {
        if (busy) return
        val connection = currentConnection() ?: return
        job = viewModelScope.launch {
            when (val result = repository.deleteSynchronizedData(connection.connectionId)) {
                is NetworkResult.Success -> _state.value = PortfolioConnectionState.Idle
                is NetworkResult.Error -> fail(result.throwable)
            }
        }
    }

    fun dismissError() {
        if (_state.value is PortfolioConnectionState.Error) publishFromRepository()
    }

    /** The user backed out of the browser without a redirect. */
    fun cancelPending() {
        val s = _state.value
        if (s is PortfolioConnectionState.WaitingForReturn || s is PortfolioConnectionState.OpeningPortal) {
            deepLinks.clearPending()
            publishFromRepository()
        }
    }

    private fun currentConnection(): PortfolioConnection? = when (val s = _state.value) {
        is PortfolioConnectionState.Connected -> s.connection
        is PortfolioConnectionState.ReconnectRequired -> s.connection
        is PortfolioConnectionState.Syncing, is PortfolioConnectionState.Error ->
            repository.connectedPortfolio.value?.connection
        else -> repository.connectedPortfolio.value?.connection
    }

    private fun publishFromRepository() {
        val current = repository.connectedPortfolio.value
        _state.value = if (current != null) stateFor(current) else PortfolioConnectionState.Idle
    }

    private fun publish(result: ConnectedPortfolio) {
        _state.value = stateFor(result)
    }

    private fun stateFor(result: ConnectedPortfolio): PortfolioConnectionState =
        if (result.connection.status == ConnectionStatus.RECONNECT_REQUIRED) {
            PortfolioConnectionState.ReconnectRequired(result.connection)
        } else {
            PortfolioConnectionState.Connected(result.connection, result.portfolio)
        }

    private fun fail(error: Throwable, connection: PortfolioConnection? = null) {
        val e = error as? PortfolioConnectionException
        if (e?.code == "RECONNECT_REQUIRED" && connection != null) {
            _state.value = PortfolioConnectionState.ReconnectRequired(connection.copy(status = ConnectionStatus.RECONNECT_REQUIRED))
            return
        }
        setError(e?.message ?: Messages.PROVIDER_UNAVAILABLE, e?.requestId, e?.retryable ?: false)
    }

    private fun setError(message: String, requestId: String?, retryable: Boolean) {
        _state.value = PortfolioConnectionState.Error(message, requestId, retryable)
    }
}
