package com.avtar.cabbilling.ui.screens.logbook

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.data.local.entity.TripEntity
import com.avtar.cabbilling.ui.components.EmptyState
import com.avtar.cabbilling.ui.components.ScreenHeader
import com.avtar.cabbilling.ui.components.TripMiniMap
import com.avtar.cabbilling.ui.containerViewModel
import com.avtar.cabbilling.ui.theme.AppGradients
import com.avtar.cabbilling.util.InvoiceExporter
import com.avtar.cabbilling.util.TripPath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogbookScreen(onMessage: (String) -> Unit) {
    val vm = containerViewModel { LogbookViewModel(it.tripRepository) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            vm.initLocation(context)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) vm.startTrip(context)
        else onMessage("Location permission required to track trips")
    }

    state.message?.let {
        onMessage(it)
        vm.clearMessage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Trip Logbook")

        if (state.tracking && state.activeTrip != null) {
            ActiveTripCard(
                state = state,
                onStop = { vm.stopTrip(context) }
            )
        } else {
            StartTripCard(
                currentAddress = state.currentAddress,
                onStart = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                        vm.startTrip(context)
                    } else {
                        permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("Trip History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (state.trips.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Route,
                title = "No trips recorded",
                message = "Start your first trip to see it here.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.trips, key = { it.id }) { trip ->
                    TripHistoryCard(
                        trip = trip,
                        onShare = {
                            InvoiceExporter.shareTripAsImage(context, trip)
                                .onFailure { onMessage("Couldn't share trip image") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StartTripCard(currentAddress: String, onStart: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.background(AppGradients.brand())) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Ready to drive?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Start recording your trip with GPS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )

                if (currentAddress.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            currentAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(52.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Trip", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActiveTripCard(state: LogbookUiState, onStop: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .alpha(alpha)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "RECORDING",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.currentAddress.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(state.currentAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Spacer(Modifier.height(12.dp))
            TripMiniMap(
                path = state.livePath,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LiveStat(Icons.Filled.Speed, "${"%.1f".format(state.currentSpeedKmh)} km/h", "Speed")
                LiveStat(Icons.Filled.Route, "${"%.2f".format(state.liveDistanceKm)} km", "Distance")
                LiveStat(Icons.Filled.Timer, formatDuration(state.liveDurationSec), "Duration")
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(52.dp).fillMaxWidth()
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Stop Trip", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LiveStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun TripHistoryCard(trip: TripEntity, onShare: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val durationSec = if (trip.endTime > 0) (trip.endTime - trip.startTime) / 1000 else 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dateFormat.format(Date(trip.startTime)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (trip.startAddress.isNotBlank() || trip.endAddress.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${trip.startAddress.ifBlank { "—" }} → ${trip.endAddress.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (trip.path.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                TripMiniMap(
                    path = TripPath.decode(trip.path),
                    modifier = Modifier.fillMaxWidth().height(130.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TripStat("${"%.2f".format(trip.distanceKm)} km", "Distance")
                TripStat(formatDuration(durationSec), "Duration")
                TripStat("${"%.0f".format(trip.maxSpeedKmh)} km/h", "Max Speed")
                TripStat("${"%.0f".format(trip.avgSpeedKmh)} km/h", "Avg Speed")
            }
        }
    }
}

@Composable
private fun TripStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}
