package com.invest.easymoney.data.portfolio.connection

import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.dto.CreateConnectionSessionRequestDto
import com.invest.easymoney.data.api.dto.ConnectionSessionResponseDto
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.connection.ConnectedPortfolio
import com.invest.easymoney.domain.portfolio.connection.ConnectionStatus
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnection
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionException
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionRepository
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnectionSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

const val SNAPTRADE_REDIRECT_URI = "easymoney://portfolio/snaptrade"

/**
 * Uses the existing authenticated backend client. Deliberately never logs portal URLs,
 * connection IDs, holdings or balances.
 */
@Singleton
class PortfolioConnectionRepositoryImpl @Inject constructor(
    private val api: BackendApiService
) : PortfolioConnectionRepository {

    private val _connectedPortfolio = MutableStateFlow<ConnectedPortfolio?>(null)
    override val connectedPortfolio: StateFlow<ConnectedPortfolio?> = _connectedPortfolio.asStateFlow()

    /** Never run two syncs at once. */
    private val syncRunning = AtomicBoolean(false)

    private suspend fun <T> call(
        context: PortfolioConnectionErrors.Context = PortfolioConnectionErrors.Context.GENERAL,
        block: suspend () -> T
    ): NetworkResult<T> = try {
        NetworkResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error(PortfolioConnectionErrors.map(e, context))
    }

    private fun session(dto: ConnectionSessionResponseDto): PortfolioConnectionSession {
        val id = dto.connectionSessionId
        val url = dto.portalUrl
        if (id.isNullOrBlank() || url.isNullOrBlank() || !url.startsWith("https://")) {
            throw PortfolioConnectionException("PROVIDER_UNAVAILABLE", Messages.SESSION_FAILED, dto.requestId, false)
        }
        return PortfolioConnectionSession(id, url, dto.expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() })
    }

    override suspend fun createSandboxConnectionSession(): NetworkResult<PortfolioConnectionSession> =
        call(PortfolioConnectionErrors.Context.SESSION) {
            session(api.createSnapTradeSession(CreateConnectionSessionRequestDto(SNAPTRADE_REDIRECT_URI)))
        }

    override suspend fun completeSandboxConnection(sessionId: String): NetworkResult<ConnectedPortfolio> =
        call { publish(ConnectedPortfolioMapper.toConnectedPortfolio(api.completeSnapTradeConnection(sessionId))) }

    override suspend fun getConnections(): NetworkResult<List<PortfolioConnection>> = call {
        api.getPortfolioConnections().connections.orEmpty().map(ConnectedPortfolioMapper::toConnection)
    }

    override suspend fun loadStoredPortfolio(connectionId: String): NetworkResult<ConnectedPortfolio> =
        call { publish(ConnectedPortfolioMapper.toConnectedPortfolio(api.getConnectionPortfolio(connectionId))) }

    override suspend fun syncConnection(connectionId: String): NetworkResult<ConnectedPortfolio> {
        if (!syncRunning.compareAndSet(false, true)) {
            return NetworkResult.Error(
                PortfolioConnectionException("SYNC_IN_PROGRESS", Messages.PROVIDER_UNAVAILABLE, null, true)
            )
        }
        return try {
            call { publish(ConnectedPortfolioMapper.toConnectedPortfolio(api.syncPortfolioConnection(connectionId))) }
        } finally {
            syncRunning.set(false)
        }
    }

    override suspend fun createReconnectSession(connectionId: String): NetworkResult<PortfolioConnectionSession> =
        call(PortfolioConnectionErrors.Context.SESSION) { session(api.createReconnectSession(connectionId)) }

    override suspend fun disconnect(connectionId: String): NetworkResult<Unit> = call {
        // Not retried by this layer: a single explicit user action.
        val response = api.disconnectPortfolioConnection(connectionId)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        _connectedPortfolio.value = null
    }

    override suspend fun deleteSynchronizedData(connectionId: String): NetworkResult<Unit> = call {
        val response = api.deleteConnectionData(connectionId, true)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        _connectedPortfolio.value = null
    }

    private fun publish(result: ConnectedPortfolio): ConnectedPortfolio {
        // A connection without a usable snapshot must not leave stale data on screen.
        _connectedPortfolio.value =
            if (result.connection.status == ConnectionStatus.DISCONNECTED) null else result
        return result
    }
}
