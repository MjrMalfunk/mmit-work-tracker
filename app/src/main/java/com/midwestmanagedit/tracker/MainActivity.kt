package com.midwestmanagedit.tracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.midwestmanagedit.tracker.data.RideEntity
import com.midwestmanagedit.tracker.data.ShiftEntity
import com.midwestmanagedit.tracker.domain.AcquisitionMode
import com.midwestmanagedit.tracker.domain.Platform
import com.midwestmanagedit.tracker.domain.QueueMode
import com.midwestmanagedit.tracker.domain.ShiftState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TrackerApp() } }
    }
}

@Composable
private fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val shift by vm.activeShift.collectAsState()
    val pending by vm.pendingRides.collectAsState()
    val latest by vm.latestCompleted.collectAsState()
    val message by vm.message.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    LaunchedEffect(Unit) {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("MMIT Work Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Offline-first • Rideshare + FieldNation • GPS saved locally")
            if (message != null) {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(message!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = vm::clearMessage) { Text("Dismiss") }
                    }
                }
            }
            if (shift == null) {
                StartShiftCard(vm)
                latest?.let { LastOutingCard(it, vm) }
            } else ActiveShiftCard(shift!!, pending, vm)
        }
    }
}

@Composable
private fun StartShiftCard(vm: TrackerViewModel) {
    var platform by remember { mutableStateOf(Platform.LYFT) }
    var queueMode by remember { mutableStateOf(QueueMode.MANUAL) }
    var odometer by remember { mutableStateOf("") }
    var firstRide by remember { mutableStateOf(false) }
    var workOrderNumber by remember { mutableStateOf("") }
    var roundTrip by remember { mutableStateOf(true) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Start work outing", style = MaterialTheme.typography.titleLarge)
            ChoiceRow("Work type", Platform.entries, platform, { platform = it }) {
                when (it) {
                    Platform.LYFT -> "Lyft"
                    Platform.UBER -> "Uber"
                    Platform.FIELD_NATION -> "FN"
                }
            }
            if (platform == Platform.FIELD_NATION) {
                OutlinedTextField(
                    value = workOrderNumber,
                    onValueChange = { workOrderNumber = it.trimStart() },
                    label = { Text("FieldNation work-order number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                FilterChip(
                    selected = roundTrip,
                    onClick = { roundTrip = !roundTrip },
                    label = { Text(if (roundTrip) "Round trip expected" else "One-way / next stop") },
                )
            } else {
                ChoiceRow("Queue", QueueMode.entries, queueMode, { queueMode = it }) {
                    if (it == QueueMode.AUTO) "Auto queue" else "Accept"
                }
            }
            OutlinedTextField(
                value = odometer,
                onValueChange = { odometer = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Starting odometer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (platform != Platform.FIELD_NATION) {
                FilterChip(
                    selected = firstRide,
                    onClick = { firstRide = !firstRide },
                    label = { Text("A ride is already accepted") },
                )
            }
            Button(
                onClick = {
                    odometer.toDoubleOrNull()?.let {
                        if (platform == Platform.FIELD_NATION) {
                            vm.startFieldNation(workOrderNumber, roundTrip, it)
                        } else {
                            vm.start(platform, queueMode, it, firstRide)
                        }
                    }
                },
                enabled = odometer.toDoubleOrNull() != null &&
                    (platform != Platform.FIELD_NATION || workOrderNumber.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) { Text(if (platform == Platform.FIELD_NATION) "START TRIP TO SITE" else "START TRACKING") }
        }
    }
}

@Composable
private fun ActiveShiftCard(shift: ShiftEntity, pending: List<RideEntity>, vm: TrackerViewModel) {
    val state = ShiftState.valueOf(shift.state)
    val mode = QueueMode.valueOf(shift.queueMode)
    var endingOdometer by remember { mutableStateOf("") }

    StatusCard(state, shift, pending.size)
    if (shift.platform == Platform.FIELD_NATION.name) {
        FieldNationActiveCard(state, shift, vm)
        return
    }
    if (state != ShiftState.RETURNING_HOME) {
        ChoiceRow("Queue mode", QueueMode.entries, mode, vm::queueMode) {
            if (it == QueueMode.AUTO) "Auto queue" else "Accept"
        }
    }

    when (state) {
        ShiftState.AVAILABLE -> {
            if (pending.isEmpty()) {
                BigButton("RIDE ACCEPTED — START PICKUP", { vm.acceptRide(mode.acquisition()) })
            } else {
                BigButton("START QUEUED PICKUP", vm::startPending)
                OutlinedButton(vm::losePending, Modifier.fillMaxWidth()) { Text("Queued ride disappeared") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ vm.breakMode(true) }, Modifier.weight(1f)) { Text("Break") }
                OutlinedButton(vm::endShift, Modifier.weight(1f)) { Text("Go offline") }
            }
        }
        ShiftState.EN_ROUTE_PICKUP -> {
            // Primary action: pick up passenger
            BigButton("PICKED UP PASSENGER", vm::pickup)

            // Secondary actions: record queued/accepted and cancel
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.queueRide(mode.acquisition()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (mode == QueueMode.AUTO) "Record auto-queued ride" else "Record accepted reserve")
                }

                OutlinedButton(
                    onClick = { vm.cancelRide() },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Ride Canceled")
                }
            }
        }
        ShiftState.PASSENGER -> {
            if (pending.isEmpty()) BigButton("DROP OFF", { vm.dropOff(false) })
            else {
                BigButton("DROP OFF + START QUEUED PICKUP", { vm.dropOff(true) })
                OutlinedButton({ vm.dropOff(false) }, Modifier.fillMaxWidth()) { Text("Drop off only — verify queue") }
                OutlinedButton(vm::losePending, Modifier.fillMaxWidth()) { Text("Queued ride disappeared") }
            }
            OutlinedButton({ vm.queueRide(mode.acquisition()) }, Modifier.fillMaxWidth()) {
                Text(if (mode == QueueMode.AUTO) "Record auto-queued ride" else "Record accepted reserve")
            }
        }
        ShiftState.BREAK -> BigButton("RESUME", { vm.breakMode(false) })
        ShiftState.RETURNING_HOME -> {
            Text("GPS is still recording the deadhead trip home.", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = endingOdometer,
                onValueChange = { endingOdometer = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Odometer at home") },
                modifier = Modifier.fillMaxWidth(),
            )
            BigButton(
                "ARRIVED HOME — COMPLETE",
                { endingOdometer.toDoubleOrNull()?.let(vm::arriveHome) },
                endingOdometer.toDoubleOrNull()?.let { it >= shift.startOdometer } == true,
            )
        }
        ShiftState.IDLE,
        ShiftState.COMPLETE,
        ShiftState.EN_ROUTE_SITE,
        ShiftState.ON_SITE,
        ShiftState.WORKING,
        ShiftState.WRAP_UP -> Unit
    }
}

@Composable
private fun FieldNationActiveCard(state: ShiftState, shift: ShiftEntity, vm: TrackerViewModel) {
    var endingOdometer by remember { mutableStateOf("") }
    Text("Work order ${shift.workOrderNumber}", fontWeight = FontWeight.Bold)
    Text(if (shift.roundTripExpected) "Round trip expected" else "One-way / continuing elsewhere")
    when (state) {
        ShiftState.EN_ROUTE_SITE -> BigButton("ARRIVED AT SITE", vm::fieldArriveSite)
        ShiftState.ON_SITE -> BigButton("CHECK IN — START WORK", vm::fieldStartWork)
        ShiftState.WORKING -> BigButton("WORK COMPLETE", vm::fieldCompleteWork)
        ShiftState.WRAP_UP -> BigButton(
            if (shift.roundTripExpected) "CHECK OUT — START RETURN" else "CHECK OUT — END TRAVEL",
            vm::fieldCheckOut,
        )
        ShiftState.RETURNING_HOME -> {
            Text(
                if (shift.roundTripExpected) {
                    "Return GPS is recording. Enter the odometer before leaving the vehicle."
                } else {
                    "Enter the final odometer to complete this work outing."
                },
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = endingOdometer,
                onValueChange = { endingOdometer = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(if (shift.roundTripExpected) "Odometer at return" else "Final odometer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            BigButton(
                if (shift.roundTripExpected) "RETURN COMPLETE" else "COMPLETE OUTING",
                { endingOdometer.toDoubleOrNull()?.let(vm::arriveHome) },
                endingOdometer.toDoubleOrNull()?.let { it >= shift.startOdometer } == true,
            )
        }
        else -> Unit
    }
}

@Composable
private fun LastOutingCard(shift: ShiftEntity, vm: TrackerViewModel) {
    val context = LocalContext.current
    val miles = shift.endOdometer?.minus(shift.startOdometer)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Last completed outing", style = MaterialTheme.typography.titleLarge)
            val label = if (shift.platform == Platform.FIELD_NATION.name) {
                "FieldNation ${shift.workOrderNumber}"
            } else {
                shift.platform
            }
            Text("$label • ${miles?.let { "%.1f miles".format(it) } ?: "miles pending"}")
            OutlinedButton(
                onClick = {
                    vm.exportLatest { file ->
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Export ride data",
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export JSON for OPS") }
        }
    }
}

@Composable
private fun StatusCard(state: ShiftState, shift: ShiftEntity, pending: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.name.replace('_', ' '), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${shift.platform.replace('_', ' ')} • started at ${shift.startOdometer}")
            if (shift.platform != Platform.FIELD_NATION.name) {
                Text("Pending queue: $pending", color = if (pending > 0) Color(0xFF9A6700) else Color.Unspecified)
            }
        }
    }
}

@Composable
private fun BigButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick, Modifier.fillMaxWidth().height(64.dp), enabled = enabled) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun <T> ChoiceRow(label: String, values: List<T>, selected: T, choose: (T) -> Unit, text: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                FilterChip(selected == value, { choose(value) }, { Text(text(value)) })
            }
        }
    }
}

private fun QueueMode.acquisition(): AcquisitionMode =
    if (this == QueueMode.AUTO) AcquisitionMode.AUTO_QUEUE else AcquisitionMode.MANUAL_ACCEPT
