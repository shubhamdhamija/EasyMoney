package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.util.Resource

interface BackendRepository {
    /**
     * Notify the backend that a user has added or removed a stock from their watchlist.
     * This is fire-and-forget and does not return a value.
     */
    suspend fun notifyWatchlistChange(deviceId: String, symbol: String, action: String)

    /**
     * Get an explanation for why a stock moved.
     */
    suspend fun explainMove(symbol: String): Resource<String>

    /**
     * Get earnings report for a symbol.
     */
    suspend fun getEarningsReport(symbol: String): Resource<EarningsReport>

    /**
     * Get upcoming earnings for a list of symbols.
     */
    suspend fun getUpcomingEarnings(symbols: List<String>): Resource<List<UpcomingEarning>>
}

