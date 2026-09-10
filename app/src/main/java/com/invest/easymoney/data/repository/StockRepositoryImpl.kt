package com.invest.easymoney.data.repository

import android.util.Log
import com.invest.easymoney.data.api.YahooFinanceApiService
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.AlertEntity
import com.invest.easymoney.data.local.entity.WatchlistEntity
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.IntradayPricePoint
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
import com.invest.easymoney.data.api.IexApiService

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val iexApi: IexApiService,
    private val api: YahooFinanceApiService,
    private val watchlistDao: WatchlistDao,
    private val alertDao: AlertDao
) : StockRepository {

    // ── Stocks ────────────────────────────────────────────────────────────────



    override suspend fun getStockDetail(symbol: String): Resource<Stock> = runCatching {
        val result = api.getChart(symbol).chart?.result?.firstOrNull()
            ?: return Resource.Error("No data for $symbol")
        val meta = result.meta
        // Build intraday points from timestamps + indicators
        val timestamps = result.timestamp ?: emptyList()
        val closes = result.indicators?.quote?.firstOrNull()?.close ?: emptyList()
        val points = mutableListOf<IntradayPricePoint>()
        val size = minOf(timestamps.size, closes.size)
        for (i in 0 until size) {
            val ts = timestamps[i]
            val c = closes[i]
            if (c != null) points.add(IntradayPricePoint(time = ts.toString(), price = c.toFloat()))
        }
        Log.d("StockRepository", "getStockDetail: $symbol -> points=${points.size}, timestamps=${timestamps.size}, closes=${closes.size}")
        val stock = meta.toStock().copy(intradayPrices = points)
        Resource.Success(stock)
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
                        api.getChart(symbol).chart?.result?.firstOrNull()?.let { res ->
                        val meta = res.meta
                        val timestamps = res.timestamp ?: emptyList()
                        val closes = res.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                        val points = mutableListOf<IntradayPricePoint>()
                        val size = minOf(timestamps.size, closes.size)
                        for (i in 0 until size) {
                            val ts = timestamps[i]
                            val c = closes[i]
                            if (c != null) points.add(IntradayPricePoint(time = ts.toString(), price = c.toFloat()))
                        }
                        meta.toStock().copy(intradayPrices = points)
                    }
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getTopStocks(): Resource<List<Stock>> = runCatching {
        Log.d("StockRepo", "Starting getTopStocks()...")
        val popularSymbols = when (val result = getPopularStocksFromApi()) {
            is Resource.Success -> {
                Log.d("StockRepo", "Got popular symbols: ${result.data?.size}")
                result.data ?: emptyList()
            }
            is Resource.Error -> {
                Log.w("StockRepo", "Error fetching popular symbols: ${result.message}, using defaults")
                Constants.POPULAR_STOCKS
            }
            is Resource.Loading -> emptyList()
        }

        val stocks = fetchCharts(popularSymbols)
            .filter { it.currentPrice > 0 }

        Log.d("StockRepo", "Loaded ${stocks.size} stocks with prices")
        if (stocks.isNotEmpty()) {
            Resource.Success(stocks)
        } else {
            Log.w("StockRepo", "No stocks loaded, falling back to mock data")
            Resource.Success(Constants.MOCK_STOCKS)
        }
    }.getOrElse { e ->
        Log.e("StockRepo", "Error in getTopStocks: ${e.message}", e)
        // Fallback to mock data on any error
        Log.i("StockRepo", "Returning mock data as fallback")
        Resource.Success(Constants.MOCK_STOCKS)
    }


    override suspend fun getNews(symbol: String): Resource<List<News>> = runCatching {
        Log.d("StockRepo", "Fetching news for $symbol...")
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
        Log.d("StockRepo", "Got ${news.size} news items for $symbol")
        Resource.Success(news)
    }.getOrElse { e ->
        Log.w("StockRepo", "Error fetching news for $symbol: ${e.message}")
        // Return empty news list on error instead of error state
        Resource.Success(emptyList())
    }

    // Search endpoint — map quotes to lightweight domain model
    override suspend fun searchSymbols(query: String): Resource<List<com.invest.easymoney.domain.model.StockSearchResult>> = runCatching {
        val resp = api.searchNews(query, quotesCount = 10, newsCount = 0, enableFuzzyQuery = true)
        val quotes = resp.quotes ?: emptyList()
        val results = quotes.map { q ->
            com.invest.easymoney.domain.model.StockSearchResult(
                symbol = q.symbol,
                name = if (q.longName.isNotEmpty()) q.longName else q.shortName.ifEmpty { q.symbol },
                exchange = q.exchange
            )
        }
        Resource.Success(results)
    }.getOrElse { e -> Resource.Error(e.message ?: "Search failed") }

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


    override suspend fun getPopularStocksFromApi(): Resource<List<String>> = try {
        Log.d("StockRepo", "Fetching popular stocks from IEX...")
        val response = iexApi.getMostActiveStocks(Constants.IEX_API_TOKEN)
        val symbols = response.mapNotNull { it.symbol.trim().ifEmpty { null } }.distinct().take(30)
        if (symbols.isNotEmpty()) {
            Log.d("StockRepo", "Got ${symbols.size} popular symbols from IEX")
            Resource.Success(symbols)
        } else {
            Log.w("StockRepo", "IEX returned empty symbols, using defaults")
            Resource.Success(Constants.POPULAR_STOCKS)
        }
    } catch (e: Exception) {
        Log.w("StockRepo", "Failed to fetch from IEX: ${e.message}, using defaults", e)
        Resource.Success(Constants.POPULAR_STOCKS)
    }
    }
