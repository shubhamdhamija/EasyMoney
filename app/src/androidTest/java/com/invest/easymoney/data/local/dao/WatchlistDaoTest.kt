package com.invest.easymoney.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.invest.easymoney.data.local.AppDatabase
import com.invest.easymoney.data.local.entity.WatchlistEntity
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchlistDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: WatchlistDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = database.watchlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_emitsUpdatedWatchlist() = runTest {
        dao.getAll().test {
            assertEquals(emptyList<WatchlistEntity>(), awaitItem())

            val apple = WatchlistEntity(
                symbol = "AAPL",
                companyName = "Apple ."
            )


            dao.insert(apple)

            assertEquals(
                listOf(apple),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun duplicateSymbol_replacesExistingEntity() = runTest {
        val original = WatchlistEntity(
            symbol = "AAPL",
            companyName = "Apple"
        )

        val updated = WatchlistEntity(
            symbol = "AAPL",
            companyName = "Apple Inc."
        )

        dao.insert(original)
        dao.insert(updated)

        val result = dao.getAll().first()

        assertEquals(1, result.size)
        assertEquals("AAPL", result.first().symbol)
        assertEquals(
            "Apple Inc.",
            result.first().companyName
        )
    }

    @Test
    fun count_returnsMembershipCount() = runTest {
        dao.insert(
            WatchlistEntity(
                symbol = "AAPL",
                companyName = "Apple Inc."
            )
        )

        assertEquals(
            1,
            dao.count("AAPL")
        )

        assertEquals(
            0,
            dao.count("TSLA")
        )
    }

    @Test
    fun delete_removesStockAndReEmitsWatchlist() = runTest {
        val apple = WatchlistEntity(
            symbol = "AAPL",
            companyName = "Apple Inc."
        )

        dao.getAll().test {
            // Initial Room emission
            assertEquals(
                emptyList<WatchlistEntity>(),
                awaitItem()
            )

            // Insert changes the table
            dao.insert(apple)

            // Room reruns the query and emits the updated result
            assertEquals(
                listOf(apple),
                awaitItem()
            )

            // Delete changes the table again
            dao.delete("AAPL")

            // Room reruns the query and emits an empty list
            assertEquals(
                emptyList<WatchlistEntity>(),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun contains_returnsWhetherSymbolExists() = runTest {
        dao.insert(
            WatchlistEntity(
                symbol = "AAPL",
                companyName = "Apple Inc."
            )
        )

        assertTrue(
            dao.contains("AAPL")
        )

        assertFalse(
            dao.contains("TSLA")
        )
    }
}
