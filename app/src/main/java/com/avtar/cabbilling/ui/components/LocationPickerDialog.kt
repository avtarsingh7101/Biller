package com.avtar.cabbilling.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.avtar.cabbilling.data.model.IndiaLocation
import com.avtar.cabbilling.data.model.OfflineLocationSource
import com.avtar.cabbilling.data.model.OnlineLocationSearch
import com.avtar.cabbilling.ui.screens.logbook.reverseGeocode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LocationPickerDialog(
    title: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var gpsLoading by remember { mutableStateOf(false) }

    val offlineAll by produceState(emptyList<String>(), context) {
        value = OfflineLocationSource.load(context)
    }
    val offlineResults by remember(query, offlineAll) {
        derivedStateOf { OfflineLocationSource.filter(offlineAll, query) }
    }

    var online by remember { mutableStateOf<List<IndiaLocation>>(emptyList()) }
    var onlineLoading by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        online = emptyList()
        onlineLoading = false
        val q = query.trim()
        if (q.length < 3) return@LaunchedEffect
        delay(400)
        onlineLoading = true
        online = OnlineLocationSearch.search(q)
        onlineLoading = false
    }
    val onlineResults = remember(online, offlineResults) {
        val known = offlineResults.mapTo(HashSet()) { it.label.lowercase() }
        online.filter { it.label.lowercase() !in known }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            gpsLoading = true
            scope.launch {
                val addr = fetchCurrentLocation(context)
                gpsLoading = false
                if (addr.isNotBlank()) onSelected(addr)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                            gpsLoading = true
                            scope.launch {
                                val addr = fetchCurrentLocation(context)
                                gpsLoading = false
                                if (addr.isNotBlank()) onSelected(addr)
                            }
                        } else {
                            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    enabled = !gpsLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (gpsLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Getting location...")
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Use current location")
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search city, or type a custom route") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    val trimmed = query.trim()
                    if (trimmed.isNotEmpty()) {
                        item(key = "custom") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelected(trimmed) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Column {
                                    Text("Use \"$trimmed\"", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "Custom route",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }

                    items(offlineResults, key = { "off:${it.label}" }) { loc ->
                        LocationRow(loc = loc, online = false, onClick = { onSelected(loc.label) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    if (onlineLoading || onlineResults.isNotEmpty()) {
                        item(key = "online-header") { OnlineHeader(loading = onlineLoading) }
                        items(onlineResults, key = { "on:${it.label}" }) { loc ->
                            LocationRow(loc = loc, online = true, onClick = { onSelected(loc.label) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchCurrentLocation(context: Context): String = withContext(Dispatchers.IO) {
    try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: return@withContext ""
        reverseGeocode(context, loc.latitude, loc.longitude)
    } catch (_: Exception) { "" }
}

@Composable
private fun LocationRow(loc: IndiaLocation, online: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (online) {
            Icon(
                Icons.Filled.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Column {
            Text(loc.city, style = MaterialTheme.typography.bodyLarge)
            if (loc.state.isNotBlank()) {
                Text(
                    loc.state,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnlineHeader(loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "From map",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (loading) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        }
    }
}
