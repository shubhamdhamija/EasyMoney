package com.invest.easymoney.data.repository

import com.invest.easymoney.domain.model.*
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.ui.home.HomeIntent
import com.invest.easymoney.ui.home.HomeViewModel
import com.invest.easymoney.util.Resource
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class FakeStockRepository : StockRepository {
    var searchResult:
    Resource<List<StockSearchResult>> =
    Resource.Success(emptyList())


    override suspend fun getTopStocks(): Resource<List<Stock>> {

        return Resource.Success(emptyList())

    }
    override suspend fun getStockDetail(symbol: String): NetworkResult<Stock> {
        TODO("Not yet implemented")
    }

    override suspend fun getNews(symbol: String): NetworkResult<List<News>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchStocksForSymbols(symbols: List<String>): Resource<List<Stock>> {
        TODO("Not yet implemented")
    }

    override fun searchSymbols(query: String): Flow<Resource<List<StockSearchResult>>> {

        return flow {
            emit(Resource.Loading)
            delay(1000)
            emit(

                        Resource.Success(

                                listOf(

                                        StockSearchResult(

                                                symbol = query,

                        name = "$query Inc",

                        exchange = "NASDAQ"

            )

            )

            )
            )



        }
    }

    override suspend fun getPopularStocksFromApi(): Resource<List<String>> {
        TODO("Not yet implemented")
    }

    override fun getWatchlistSymbols(): Flow<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun addToWatchlist(symbol: String) {
        TODO("Not yet implemented")
    }

    override suspend fun removeFromWatchlist(symbol: String) {
        TODO("Not yet implemented")
    }

    override suspend fun isInWatchlist(symbol: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAlerts(): Flow<List<Alert>> {
        TODO("Not yet implemented")
    }

    override suspend fun addAlert(alert: Alert) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAlert(alertId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllAlertsOnce(): List<Alert> {
        TODO("Not yet implemented")
    }
}
