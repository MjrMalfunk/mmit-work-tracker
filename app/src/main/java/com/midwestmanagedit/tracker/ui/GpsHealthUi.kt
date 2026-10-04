package com.midwestmanagedit.tracker.ui

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midwestmanagedit.tracker.TrackerViewModel
import com.midwestmanagedit.tracker.tracking.GpsStatus
import kotlinx.coroutines.delay

@Composable
fun GpsHealthCard(shiftId: String, vm: TrackerViewModel) {
    val health by vm.gpsHealth.collectAsState()
    var now by remember(shiftId) { mutableStateOf(SystemClock.elapsedRealtime()) }
    val context = LocalContext.current
    LaunchedEffect(shiftId) {
        while (true) { now = SystemClock.elapsedRealtime(); delay(15_000L) }
    }
    val status = if (health.shiftId != shiftId || health.status == GpsStatus.STOPPED)
        GpsStatus.SERVICE_UNAVAILABLE else health.effectiveStatus(now)
    val title = when (status) {
        GpsStatus.RECORDING -> "GPS recording"
        GpsStatus.WAITING -> "Waiting for a GPS fix"
        GpsStatus.PERMISSION_REQUIRED -> "Location permission needed"
        GpsStatus.APPROXIMATE_ONLY -> "Precise location needed"
        GpsStatus.LOCATION_DISABLED -> "Phone location is off"
        GpsStatus.REQUEST_FAILED -> "GPS could not connect"
        GpsStatus.SAVE_FAILED -> "GPS could not save"
        GpsStatus.SERVICE_UNAVAILABLE -> "GPS service needs attention"
        else -> "GPS needs attention"
    }
    val description = when (status) {
        GpsStatus.RECORDING -> "Locations are being saved on this phone."
        GpsStatus.WAITING -> "Move somewhere with a clear view of the sky."
        GpsStatus.PERMISSION_REQUIRED, GpsStatus.APPROXIMATE_ONLY -> "Allow location while using the app and enable precise location."
        GpsStatus.LOCATION_DISABLED -> "Turn on your phone’s location, then return here."
        GpsStatus.SAVE_FAILED -> "Check available phone storage, then retry GPS."
        GpsStatus.SERVICE_UNAVAILABLE -> "Retry GPS with this screen open. Check app battery settings if it stops again."
        else -> "No fresh saved location. The tracker will retry; you can also retry below."
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                fontWeight = FontWeight.Bold, color = when (status) {
                GpsStatus.RECORDING -> MaterialTheme.colorScheme.primary
                GpsStatus.WAITING -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            })
            Text(description)
            if (health.shiftId == shiftId) health.lastSavedAtElapsedMs?.let { elapsed ->
                val seconds = ((now - elapsed).coerceAtLeast(0L) / 1_000L)
                Text("Last saved fix: ${if (seconds < 60) "$seconds seconds" else "${seconds / 60} minutes"} ago")
            }
            if (status != GpsStatus.RECORDING) {
                OutlinedButton(onClick = {
                    when (status) {
                        GpsStatus.PERMISSION_REQUIRED, GpsStatus.APPROXIMATE_ONLY -> context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        GpsStatus.LOCATION_DISABLED -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        else -> vm.recoverTracking(force = true)
                    }
                }) { Text(when (status) {
                    GpsStatus.PERMISSION_REQUIRED, GpsStatus.APPROXIMATE_ONLY -> "Open app permissions"
                    GpsStatus.LOCATION_DISABLED -> "Open location settings"
                    else -> "Retry GPS"
                }) }
            }
        }
    }
}
