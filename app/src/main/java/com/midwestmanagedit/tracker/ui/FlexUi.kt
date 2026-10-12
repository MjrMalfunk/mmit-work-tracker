package com.midwestmanagedit.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midwestmanagedit.tracker.TrackerViewModel
import com.midwestmanagedit.tracker.data.ShiftEntity
import com.midwestmanagedit.tracker.domain.FlexBlock
import com.midwestmanagedit.tracker.domain.FlexAction
import com.midwestmanagedit.tracker.domain.ShiftState
import java.time.LocalDate
import java.text.DateFormat
import java.util.Date

@Composable
fun FlexStartFields(onBlock: (FlexBlock?) -> Unit) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("") }
    var pay by rememberSaveable { mutableStateOf("") }
    var station by rememberSaveable { mutableStateOf("") }
    val parsed = remember(date, time, hours, pay, station) { runCatching { FlexBlock.parse(date, time, hours, pay, station) } }
    LaunchedEffect(date, time, hours, pay, station) { onBlock(parsed.getOrNull()) }
    Text("Record the offer, then start tracking when you leave for pickup.")
    OutlinedTextField(date, { date = it }, label = { Text("Block date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(time, { time = it }, label = { Text("Block start (HH:mm, 24-hour)") }, placeholder = { Text("04:15") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(hours, { hours = it }, label = { Text("Scheduled hours") }, placeholder = { Text("3.5") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(pay, { pay = it }, label = { Text("Offered block pay ($)") }, placeholder = { Text("79.00") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(station, { station = it }, label = { Text("Pickup station (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    parsed.getOrNull()?.let { block ->
        Text("Scheduled finish: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(block.endEpochMs)))
        Text("Offered pay: $%.2f • $%.2f/hr scheduled".format(block.payCents / 100.0, FlexBlock.hourly(block.payCents, block.endEpochMs - block.startEpochMs)))
    }
    if (time.isNotBlank() && hours.isNotBlank() && pay.isNotBlank()) parsed.exceptionOrNull()?.let {
        Text(it.message ?: "Review the block details.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun FlexActiveActions(shift: ShiftEntity, vm: TrackerViewModel) {
    val state = ShiftState.valueOf(shift.state)
    FlexSummary(shift)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            ShiftState.EN_ROUTE_STATION -> Button({ vm.flexAction(FlexAction.ARRIVE_STATION) }, Modifier.fillMaxWidth()) { Text("ARRIVED AT PICKUP STATION") }
            ShiftState.AT_STATION -> {
                Text("Station wait and loading time are included in actual work time.")
                Button({ vm.flexAction(FlexAction.START_DELIVERIES) }, Modifier.fillMaxWidth()) { Text("LOADED — START DELIVERIES") }
                OutlinedButton({ vm.flexAction(FlexAction.RELEASE_NO_ROUTE) }, Modifier.fillMaxWidth()) { Text("Released without a route") }
            }
            ShiftState.DELIVERING -> {
                Text("No need to log each package. Mark the end of the route when safely stopped.")
                Button({ vm.flexAction(FlexAction.FINISH_DELIVERIES) }, Modifier.fillMaxWidth()) { Text("DELIVERIES DONE — GO HOME") }
                OutlinedButton({ vm.flexAction(FlexAction.RETURN_PACKAGES) }, Modifier.fillMaxWidth()) { Text("Deliveries done — return packages to station") }
            }
            ShiftState.RETURNING_PACKAGES -> {
                Text("Required station returns remain part of actual work time and mileage.")
                Button({ vm.flexAction(FlexAction.FINISH_RETURN) }, Modifier.fillMaxWidth()) { Text("PACKAGES RETURNED — GO HOME") }
            }
            else -> Unit
        }
    }
}

@Composable
fun FlexSummary(shift: ShiftEntity) {
    shift.flexStation?.let { Text("Pickup: $it") }
    val start = shift.flexBlockStartEpochMs
    val end = shift.flexBlockEndEpochMs
    val pay = shift.flexBlockPayCents
    if (start != null && end != null && pay != null) {
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        Text("Scheduled: ${format.format(Date(start))} — ${format.format(Date(end))}")
        Text("Offered block pay: $%.2f • $%.2f/hr scheduled".format(pay / 100.0, FlexBlock.hourly(pay, end - start)), fontWeight = FontWeight.Bold)
        val arrived = shift.flexStationArrivedAtEpochMs
        val finished = shift.endedAtEpochMs
        if (arrived != null && finished != null) {
            Text("Actual station-to-finish: %.0f min (includes loading and required returns)".format((finished - arrived) / 60_000.0))
            FlexBlock.hourly(pay, finished - arrived)?.let { Text("Offered pay / actual work: $%.2f/hr".format(it)) }
        }
        val home = shift.correctedHomeArrivedAtEpochMs ?: shift.homeArrivedAtEpochMs
        if (home != null) {
            Text("Door-to-door: %.0f min".format((home - shift.startedAtEpochMs) / 60_000.0))
            FlexBlock.hourly(pay, home - shift.startedAtEpochMs)?.let { Text("Offered pay / door-to-door: $%.2f/hr gross".format(it)) }
        }
        shift.endOdometer?.minus(shift.startOdometer)?.takeIf { it > 0 }?.let {
            Text("Offered pay / total outing miles: $%.2f/mi gross".format(pay / 100.0 / it))
        }
        Text("Offered pay is not confirmed payout or profit. Review final pay, tips and costs in OPS.", style = MaterialTheme.typography.bodySmall)
    }
}
