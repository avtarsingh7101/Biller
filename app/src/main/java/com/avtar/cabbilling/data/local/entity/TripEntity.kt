package com.avtar.cabbilling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val startAddress: String = "",
    val endAddress: String = "",
    val distanceKm: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val isActive: Boolean = false,
    /** Recorded GPS route as "lat,lng;lat,lng;…" (empty for trips before route recording). */
    val path: String = ""
)
