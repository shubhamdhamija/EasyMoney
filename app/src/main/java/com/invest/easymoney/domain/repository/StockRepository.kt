package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    suspend fun getTopStocks(): Resource<List<Stock>>
    suspend fun getStockDetail(symbol: String): Resource<Stock>
    suspend fun getNews(symbol: String): Resource<List<News>>
    suspend fun fetchStocksForSymbols(symbols: List<String>): Resource<List<Stock>>

    // Watchlist
    fun getWatchlistSymbols(): Flow<List<String>>
    suspend fun addToWatchlist(symbol: String)
    suspend fun removeFromWatchlist(symbol: String)
    suspend fun isInWatchlist(symbol: String): Boolean

    // Alerts
    fun getAlerts(): Flow<List<Alert>>
    suspend fun addAlert(alert: Alert)
    suspend fun deleteAlert(alertId: Int)
    suspend fun getAllAlertsOnce(): List<Alert>
}
