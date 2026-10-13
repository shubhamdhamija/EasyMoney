package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.IexApiService
import com.invest.easymoney.data.api.YahooFinanceApiService
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.WatchlistEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StockRepositoryWatchlistTest {

    private lateinit var iexApi: IexApiService
    private lateinit var yahooApi: YahooFinanceApiService
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var alertDao: AlertDao

    private lateinit var repository: StockRepositoryImpl

    @Before
    fun setUp() {
        iexApi = mockk()
        yahooApi = mockk()
        watchlistDao = mockk()
        alertDao = mockk()

        repository = StockRepositoryImpl(
            iexApi = iexApi,
            api = yahooApi,
            watchlistDao = watchlistDao,
            alertDao = alertDao
        )
    }

    @Test
    fun `addToWatchlist normalizes symbol and inserts entity`() = runTest {
        val entitySlot = slot<WatchlistEntity>()

        coEvery {
            watchlistDao.insert(
                capture(entitySlot)
            )
        } just Runs

        repository.addToWatchlist(
            symbol = " aapl ",
            companyName = "Apple Inc."
        )

        coVerify(exactly = 1) {
            watchlistDao.insert(any())
        }

        assertEquals(
            "AAPL",
            entitySlot.captured.symbol
        )

        assertEquals(
            "Apple Inc.",
            entitySlot.captured.companyName
        )
    }

    @Test
    fun `addToWatchlist uses symbol when company name is blank`() = runTest {
        val entitySlot = slot<WatchlistEntity>()

        coEvery {
            watchlistDao.insert(
                capture(entitySlot)
            )
        } just Runs

        repository.addToWatchlist(
            symbol = "tsla",
            companyName = ""
        )

        assertEquals(
            "TSLA",
            entitySlot.captured.symbol
        )

        assertEquals(
            "TSLA",
            entitySlot.captured.companyName
        )
    }

    @Test
    fun `removeFromWatchlist normalizes symbol before deleting`() = runTest {
        coEvery {
            watchlistDao.delete(any())
        } just Runs

        repository.removeFromWatchlist(
            " tsla "
        )

        coVerify(exactly = 1) {
            watchlistDao.delete("TSLA")
        }
    }

    @Test
    fun `isInWatchlist normalizes symbol and returns DAO result`() = runTest {
        coEvery {
            watchlistDao.contains("AAPL")
        } returns true

        val result =
            repository.isInWatchlist(
                " aapl "
            )

        assertTrue(result)

        coVerify(exactly = 1) {
            watchlistDao.contains("AAPL")
        }
    }

    @Test
    fun `getWatchlist exposes DAO emissions`() = runTest {
        val initial = emptyList<WatchlistEntity>()

        val updated = listOf(
            WatchlistEntity(
                symbol = "AAPL",
                companyName = "Apple Inc."
            )
        )

        every {
            watchlistDao.getAll()
        } returns flowOf(
            initial,
            updated
        )

        repository.getWatchlist().test {
            assertEquals(
                initial,
                awaitItem()
            )

            assertEquals(
                updated,
                awaitItem()
            )

            awaitComplete()
        }
    }
}


