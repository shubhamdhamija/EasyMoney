package com.invest.easymoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invest.easymoney.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts")
    fun getAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts")
    suspend fun getAllOnce(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun delete(id: Int)
}
