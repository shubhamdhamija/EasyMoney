package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.YahooFinanceApiService
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.AlertEntity
import com.invest.easymoney.data.local.entity.WatchlistEntity
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Constants
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: YahooFinanceApiService,
    private val watchlistDao: WatchlistDao,
    private val alertDao: AlertDao
) : StockRepository {

    // ── Stocks ────────────────────────────────────────────────────────────────

    override suspend fun getTopStocks(): Resource<List<Stock>> = runCatching {
        val stocks = fetchCharts(Constants.POPULAR_STOCKS)
            .filter { it.currentPrice > 0 }
        Resource.Success(stocks)
    }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch stocks") }

    override suspend fun getStockDetail(symbol: String): Resource<Stock> = runCatching {
        val meta = api.getChart(symbol).chart?.result?.firstOrNull()?.meta
            ?: return Resource.Error("No data for $symbol")
        Resource.Success(meta.toStock())
    }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch $symbol") }

    override suspend fun fetchStocksForSymbols(symbols: List<String>): Resource<List<Stock>> {
        if (symbols.isEmpty()) return Resource.Success(emptyList())
        return runCatching {
            Resource.Success(fetchCharts(symbols))
        }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch stocks") }
    }

    /** Fetches each symbol's chart concurrently and maps to [Stock]. */
    private suspend fun fetchCharts(symbols: List<String>): List<Stock> = coroutineScope {
        symbols.map { symbol ->
            async {
                runCatching {
                    api.getChart(symbol).chart?.result?.firstOrNull()?.meta?.toStock()
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getNews(symbol: String): Resource<List<News>> = runCatching {
        val response = api.searchNews(symbol)
        val news = response.news
            ?.filter { it.title.isNotEmpty() }
            ?.map { dto ->
                News(
                    id = dto.uuid.hashCode().toLong(),
                    headline = dto.title,
                    source = dto.publisher,
                    url = dto.link,
                    summary = "",
                    imageUrl = "",
                    datetime = dto.publishTime
                )
            } ?: emptyList()
        Resource.Success(news)
    }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch news") }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    override fun getWatchlistSymbols(): Flow<List<String>> =
        watchlistDao.getAll().map { list -> list.map { it.symbol } }

    override suspend fun addToWatchlist(symbol: String) =
        watchlistDao.insert(WatchlistEntity(symbol = symbol))

    override suspend fun removeFromWatchlist(symbol: String) =
        watchlistDao.delete(symbol)

    override suspend fun isInWatchlist(symbol: String): Boolean =
        watchlistDao.count(symbol) > 0

    // ── Alerts ────────────────────────────────────────────────────────────────

    override fun getAlerts(): Flow<List<Alert>> =
        alertDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addAlert(alert: Alert) =
        alertDao.insert(AlertEntity(symbol = alert.symbol, percentage = alert.percentage, type = alert.type.name))

    override suspend fun deleteAlert(alertId: Int) = alertDao.delete(alertId)

    override suspend fun getAllAlertsOnce(): List<Alert> =
        alertDao.getAllOnce().map { it.toDomain() }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun com.invest.easymoney.data.api.dto.ChartMeta.toStock(): Stock {
        val prevClose = if (chartPreviousClose > 0) chartPreviousClose else previousClose
        val change = if (prevClose > 0) regularMarketPrice - prevClose else 0.0
        val changePercent = if (prevClose > 0) change / prevClose * 100 else 0.0
        return Stock(
            symbol = symbol,
            name = shortName.ifEmpty { longName.ifEmpty { symbol } },
            currentPrice = regularMarketPrice,
            change = change,
            changePercent = changePercent,
            highPrice = dayHigh,
            lowPrice = dayLow,
            openPrice = prevClose,
            previousClose = prevClose,
            marketCap = 0.0,
            exchange = exchange,
            industry = ""
        )
    }

    private fun AlertEntity.toDomain() = Alert(
        id = id,
        symbol = symbol,
        percentage = percentage,
        type = if (type == "INCREASE") AlertType.INCREASE else AlertType.DECREASE
    )
}
