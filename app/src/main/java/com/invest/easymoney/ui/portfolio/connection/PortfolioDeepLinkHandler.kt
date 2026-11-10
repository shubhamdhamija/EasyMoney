package com.invest.easymoney.ui.portfolio.connection

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A validated return from the SnapTrade Connection Portal. It is a hint, never proof of success. */
sealed interface PortalCallback {
    /** Success hint: the backend must still verify the connection with SnapTrade. */
    data class Returned(val sessionId: String) : PortalCallback
    data class Canceled(val sessionId: String) : PortalCallback
    data class Failed(val sessionId: String) : PortalCallback
}

object PortfolioDeepLink {
    const val SCHEME = "easymoney"
    const val HOST = "portfolio"
    const val PATH = "/snaptrade"

    /**
     * Pure validation of the redirect. Returns null (ignored) unless scheme, host and path match, a
     * session is pending, the status is recognized and any session parameter equals the pending one.
     * A missing status is accepted only as "returned": the backend decides.
     */
    fun validate(
        scheme: String?,
        host: String?,
        path: String?,
        status: String?,
        sessionParam: String?,
        pendingSessionId: String?
    ): PortalCallback? {
        if (scheme != SCHEME || host != HOST || path != PATH) return null
        val pending = pendingSessionId ?: return null
        if (sessionParam != null && sessionParam != pending) return null
        return when (status?.uppercase()) {
            null, "", "SUCCESS" -> PortalCallback.Returned(pending)
            "ABANDONED", "CANCELED", "CANCELLED" -> PortalCallback.Canceled(pending)
            "ERROR" -> PortalCallback.Failed(pending)
            else -> null
        }
    }
}

/** Holds the pending session and the single, consume-once callback. */
@Singleton
class PortfolioDeepLinkHandler @Inject constructor() {
    @Volatile
    private var pendingSessionId: String? = null
    private val _callback = MutableStateFlow<PortalCallback?>(null)
    val callback: StateFlow<PortalCallback?> = _callback.asStateFlow()

    fun registerPending(sessionId: String) {
        pendingSessionId = sessionId
        _callback.value = null
    }

    fun clearPending() {
        pendingSessionId = null
        _callback.value = null
    }

    /** Called from MainActivity. Returns true if the intent data was a valid callback. */
    fun onUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val cb = PortfolioDeepLink.validate(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            status = uri.getQueryParameter("status"),
            sessionParam = uri.getQueryParameter("session"),
            pendingSessionId = pendingSessionId
        ) ?: return false
        _callback.value = cb
        return true
    }

    /** Returns true exactly once per callback. */
    fun consume(expected: PortalCallback): Boolean {
        val ok = _callback.compareAndSet(expected, null)
        if (ok) pendingSessionId = null
        return ok
    }
}
