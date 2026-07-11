package com.avtar.cabbilling.data.repository

import com.avtar.cabbilling.data.local.TripDao
import com.avtar.cabbilling.data.local.entity.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TripRepository(private val tripDao: TripDao) {

    fun observeAll(): Flow<List<TripEntity>> = tripDao.observeAll()

    fun observeActive(): Flow<TripEntity?> = tripDao.observeActive()

    suspend fun getActive(): TripEntity? = withContext(Dispatchers.IO) { tripDao.getActive() }

    suspend fun startTrip(startAddress: String): Result<TripEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = tripDao.getActive()
            if (existing != null) error("A trip is already active.")
            val entity = TripEntity(
                startTime = System.currentTimeMillis(),
                startAddress = startAddress,
                isActive = true
            )
            val id = tripDao.insert(entity)
            entity.copy(id = id)
        }
    }

    suspend fun updateTrip(trip: TripEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { tripDao.update(trip) }
    }

    suspend fun endTrip(
        endAddress: String,
        distanceKm: Double,
        maxSpeedKmh: Double,
        avgSpeedKmh: Double,
        path: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val active = tripDao.getActive() ?: error("No active trip.")
            tripDao.update(
                active.copy(
                    endTime = System.currentTimeMillis(),
                    endAddress = endAddress,
                    distanceKm = distanceKm,
                    maxSpeedKmh = maxSpeedKmh,
                    avgSpeedKmh = avgSpeedKmh,
                    isActive = false,
                    path = path
                )
            )
        }
    }

    suspend fun deleteTrip(trip: TripEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { tripDao.delete(trip) }
    }
}
