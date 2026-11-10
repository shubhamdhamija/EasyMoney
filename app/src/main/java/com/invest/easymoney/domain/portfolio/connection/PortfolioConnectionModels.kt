package com.invest.easymoney.domain.portfolio.connection

import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.Portfolio
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Read-only automatic portfolio connection (SnapTrade Sandbox in this milestone).
 * There is intentionally no order, trade, transfer, deposit or withdrawal capability.
 */
enum class ConnectionStatus {
    PENDING, CONNECTED, SYNCING, READY, DISABLED, RECONNECT_REQUIRED, ERROR, DISCONNECTED, UNKNOWN
}

/** Safe connection metadata. Holds no secrets, provider IDs or brokerage payloads. */
data class PortfolioConnection(
    val connectionId: String,
    val institutionName: String,
    val status: ConnectionStatus,
    val createdAt: Instant?,
    val lastSuccessfulSyncAt: Instant?,
    val accountCount: Int,
    val positionCount: Int
) {
    /** Always true; no model or request may change the access level. */
    val isReadOnly: Boolean get() = true
}

/** The portal URL is short-lived and must never be logged. */
data class PortfolioConnectionSession(
    val sessionId: String,
    val portalUrl: String,
    val expiresAt: Instant?
) {
    override fun toString(): String = "PortfolioConnectionSession(sessionId=<redacted>, portalUrl=<redacted>)"
}

data class ConnectedPortfolio(
    val connection: PortfolioConnection,
    val portfolio: Portfolio?,
    val warnings: List<String> = emptyList()
)

/** Safe failure: stable code, user-presentable message and optional request ID for support. */
class PortfolioConnectionException(
    val code: String,
    override val message: String,
    val requestId: String? = null,
    val retryable: Boolean = false
) : Exception(message)

interface PortfolioConnectionRepository {
    /** Latest synchronized sandbox portfolio, held in memory only (not Room). */
    val connectedPortfolio: StateFlow<ConnectedPortfolio?>

    suspend fun createSandboxConnectionSession(): NetworkResult<PortfolioConnectionSession>
    suspend fun completeSandboxConnection(sessionId: String): NetworkResult<ConnectedPortfolio>
    suspend fun getConnections(): NetworkResult<List<PortfolioConnection>>
    suspend fun loadStoredPortfolio(connectionId: String): NetworkResult<ConnectedPortfolio>
    suspend fun syncConnection(connectionId: String): NetworkResult<ConnectedPortfolio>
    suspend fun createReconnectSession(connectionId: String): NetworkResult<PortfolioConnectionSession>
    suspend fun disconnect(connectionId: String): NetworkResult<Unit>
    suspend fun deleteSynchronizedData(connectionId: String): NetworkResult<Unit>
}
