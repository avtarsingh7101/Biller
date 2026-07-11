package com.avtar.cabbilling.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.avtar.cabbilling.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): TripEntity?

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<TripEntity?>

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("DELETE FROM trips")
    suspend fun clear()
}
