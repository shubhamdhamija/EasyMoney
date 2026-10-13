package com.invest.easymoney.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.invest.easymoney.data.local.entity.WatchlistEntity
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To2_addsCompanyNameColumnAndPreservesWatchlistRows() = runTest {
        createV1Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()

        try {
            val watchlist = database.watchlistDao().getAllOnce()

            assertEquals(
                listOf(
                    WatchlistEntity(
                        symbol = "AAPL",
                        companyName = ""
                    )
                ),
                watchlist
            )

            assertTrue(hasCompanyNameColumn())
        } finally {
            database.close()
        }
    }

    private fun createV1Database() {
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS watchlist (symbol TEXT NOT NULL PRIMARY KEY)"
            )
            db.execSQL("DELETE FROM watchlist")
            db.execSQL("INSERT INTO watchlist (symbol) VALUES ('AAPL')")
            db.execSQL("PRAGMA user_version = 1")
        }
    }

    private fun hasCompanyNameColumn(): Boolean {
        val db = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        db.use { database ->
            database.rawQuery("PRAGMA table_info(watchlist)", null).use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "companyName") return true
                }
            }
        }

        return false
    }
}



