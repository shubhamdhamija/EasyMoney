package com.invest.easymoney.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.invest.easymoney.data.local.AppDatabase
import com.invest.easymoney.data.local.entity.WatchlistEntity
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
                companyName = "Apple Inc."
            )

            dao.insert(apple)

            assertEquals(
                listOf(apple),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}

