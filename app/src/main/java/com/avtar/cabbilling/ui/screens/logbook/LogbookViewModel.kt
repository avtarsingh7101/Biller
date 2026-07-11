package com.avtar.cabbilling.ui.screens.logbook

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.local.entity.TripEntity
import com.avtar.cabbilling.data.repository.TripRepository
import com.avtar.cabbilling.util.GeoPoint
import com.avtar.cabbilling.util.TripPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class LogbookUiState(
    val trips: List<TripEntity> = emptyList(),
    val activeTrip: TripEntity? = null,
    val currentSpeedKmh: Double = 0.0,
    val liveDistanceKm: Double = 0.0,
    val liveDurationSec: Long = 0,
    val currentAddress: String = "",
    val livePath: List<GeoPoint> = emptyList(),
    val tracking: Boolean = false,
    val message: String? = null
)

class LogbookViewModel(private val tripRepository: TripRepository) : ViewModel() {

    private data class Live(
        val currentSpeedKmh: Double = 0.0,
        val liveDistanceKm: Double = 0.0,
        val liveDurationSec: Long = 0,
        val currentAddress: String = "",
        val livePath: List<GeoPoint> = emptyList(),
        val tracking: Boolean = false,
        val message: String? = null
    )

    private val live = MutableStateFlow(Live())
    private var locationManager: LocationManager? = null

    // --- Trip accumulation state (guarded by the single-threaded listener) ---
    private var lastAccepted: Location? = null
    private var lastAcceptedTimeMs = 0L
    private var totalDistanceKm = 0.0
    private var maxSpeedKmh = 0.0
    private val speedSamples = mutableListOf<Double>()
    private val pathPoints = mutableListOf<GeoPoint>()
    private var timerJob: Job? = null

    val uiState: StateFlow<LogbookUiState> =
        combine(tripRepository.observeAll(), tripRepository.observeActive(), live) { trips, active, l ->
            LogbookUiState(
                trips = trips.filter { !it.isActive },
                activeTrip = active,
                currentSpeedKmh = l.currentSpeedKmh,
                liveDistanceKm = l.liveDistanceKm,
                liveDurationSec = l.liveDurationSec,
                currentAddress = l.currentAddress,
                livePath = l.livePath,
                tracking = l.tracking,
                message = l.message
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogbookUiState())

    private val locationListener = LocationListener { location ->
        // 1) Drop unreliable fixes outright (network fixes, cold GPS, tunnels).
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return@LocationListener

        val nowMs = if (location.time > 0) location.time else System.currentTimeMillis()
        val prev = lastAccepted
        if (prev == null) {
            // First good fix — anchor the route here, no distance yet.
            lastAccepted = location
            lastAcceptedTimeMs = nowMs
            pathPoints.add(GeoPoint(location.latitude, location.longitude))
            live.update { it.copy(livePath = pathPoints.toList()) }
            return@LocationListener
        }

        val meters = prev.distanceTo(location)                 // geodesic distance, metres
        val dtSec = (nowMs - lastAcceptedTimeMs) / 1000.0
        // Movement must clear BOTH a fixed drift floor and the fix's own error radius,
        // otherwise a parked car's GPS jitter silently inflates the odometer.
        val moveFloor = maxOf(MIN_MOVE_M, if (location.hasAccuracy()) location.accuracy else 0f)

        if (meters < moveFloor || dtSec <= 0) {
            // Treated as stationary: hold distance, show zero speed.
            live.update { it.copy(currentSpeedKmh = 0.0) }
            return@LocationListener
        }

        val segmentSpeedKmh = (meters / dtSec) * 3.6
        if (segmentSpeedKmh > MAX_SANE_SPEED_KMH) {
            // Physically impossible jump (spoof / GPS teleport) — reject the segment.
            return@LocationListener
        }

        totalDistanceKm += meters / 1000.0
        if (segmentSpeedKmh > maxSpeedKmh) maxSpeedKmh = segmentSpeedKmh
        speedSamples.add(segmentSpeedKmh)
        pathPoints.add(GeoPoint(location.latitude, location.longitude))
        lastAccepted = location
        lastAcceptedTimeMs = nowMs

        // Prefer the chip's own Doppler speed when present; fall back to displacement speed.
        val shownSpeed = if (location.hasSpeed() && location.speed > 0f) location.speed * 3.6 else segmentSpeedKmh
        live.update {
            it.copy(
                currentSpeedKmh = shownSpeed,
                liveDistanceKm = totalDistanceKm,
                livePath = pathPoints.toList()
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun initLocation(context: Context) {
        if (live.value.currentAddress.isNotBlank() || live.value.tracking) return
        viewModelScope.launch {
            val loc = getLastLocation(context)
            val addr = loc?.let { withContext(Dispatchers.IO) { reverseGeocode(context, it.latitude, it.longitude) } } ?: ""
            if (addr.isNotBlank()) live.update { it.copy(currentAddress = addr) }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTrip(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm

        totalDistanceKm = 0.0
        maxSpeedKmh = 0.0
        speedSamples.clear()
        pathPoints.clear()
        lastAccepted = null
        lastAcceptedTimeMs = 0L

        viewModelScope.launch {
            val startLoc = getLastLocation(context)
            val address = startLoc?.let { withContext(Dispatchers.IO) { reverseGeocode(context, it.latitude, it.longitude) } } ?: ""
            if (startLoc != null) {
                lastAccepted = startLoc
                lastAcceptedTimeMs = if (startLoc.time > 0) startLoc.time else System.currentTimeMillis()
                pathPoints.add(GeoPoint(startLoc.latitude, startLoc.longitude))
            }

            tripRepository.startTrip(address).onSuccess {
                live.update {
                    it.copy(
                        tracking = true,
                        currentAddress = address,
                        liveDistanceKm = 0.0,
                        currentSpeedKmh = 0.0,
                        livePath = pathPoints.toList()
                    )
                }
                // Time-based updates (minDistance = 0) so speed drops to 0 when stopped —
                // real-time, not a stale last-known value.
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener)
                try {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, locationListener)
                } catch (_: Exception) { }
                startTimer()
            }.onFailure { e ->
                live.update { it.copy(message = e.message) }
            }
        }
    }

    fun stopTrip(context: Context) {
        locationManager?.removeUpdates(locationListener)
        timerJob?.cancel()

        viewModelScope.launch {
            val endLoc = getLastLocation(context)
            val endAddr = endLoc
                ?.let { withContext(Dispatchers.IO) { reverseGeocode(context, it.latitude, it.longitude) } }
                ?.ifBlank { live.value.currentAddress }
                ?: live.value.currentAddress
            if (endLoc != null) {
                val last = pathPoints.lastOrNull()
                if (last == null || endLoc.distanceTo(Location("").apply {
                        latitude = last.lat; longitude = last.lng
                    }) > MIN_MOVE_M) {
                    pathPoints.add(GeoPoint(endLoc.latitude, endLoc.longitude))
                }
            }
            val avg = if (speedSamples.isNotEmpty()) speedSamples.average() else 0.0
            tripRepository.endTrip(
                endAddress = endAddr,
                distanceKm = totalDistanceKm,
                maxSpeedKmh = maxSpeedKmh,
                avgSpeedKmh = avg,
                path = TripPath.encode(pathPoints)
            )
            live.update { it.copy(tracking = false, currentSpeedKmh = 0.0, livePath = emptyList()) }
        }
    }

    fun clearMessage() = live.update { it.copy(message = null) }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val active = tripRepository.getActive()
                if (active != null) {
                    val elapsed = (System.currentTimeMillis() - active.startTime) / 1000
                    live.update { it.copy(liveDurationSec = elapsed) }
                }
                delay(1000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        locationManager?.removeUpdates(locationListener)
        timerJob?.cancel()
    }

    private companion object {
        const val MAX_ACCURACY_M = 30f       // reject fixes whose error radius exceeds this
        const val MIN_MOVE_M = 5f            // ignore sub-threshold GPS drift
        const val MAX_SANE_SPEED_KMH = 220.0 // reject teleport / spoof segments
    }
}

fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    return try {
        @Suppress("DEPRECATION")
        val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
        if (addresses.isNullOrEmpty()) return "${"%.4f".format(lat)}, ${"%.4f".format(lng)}"
        val a = addresses[0]
        listOfNotNull(a.locality, a.subAdminArea, a.adminArea)
            .distinct()
            .joinToString(", ")
            .ifBlank { a.getAddressLine(0) ?: "${"%.4f".format(lat)}, ${"%.4f".format(lng)}" }
    } catch (_: Exception) {
        "${"%.4f".format(lat)}, ${"%.4f".format(lng)}"
    }
}
