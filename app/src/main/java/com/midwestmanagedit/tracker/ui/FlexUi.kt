package com.midwestmanagedit.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midwestmanagedit.tracker.TrackerViewModel
import com.midwestmanagedit.tracker.data.ShiftEntity
import com.midwestmanagedit.tracker.domain.FlexBlock
import com.midwestmanagedit.tracker.domain.FlexAction
import com.midwestmanagedit.tracker.domain.FlexCancellationReason
import com.midwestmanagedit.tracker.domain.FlexOutcome
import com.midwestmanagedit.tracker.domain.FlexPay
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
    var showCancellation by rememberSaveable(shift.id) { mutableStateOf(false) }
    var reason by rememberSaveable(shift.id) { mutableStateOf("") }
    var note by rememberSaveable(shift.id) { mutableStateOf("") }
    FlexSummary(shift) { cents -> vm.confirmFlexPay(shift.id, cents) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            ShiftState.EN_ROUTE_STATION -> Button({ vm.flexAction(FlexAction.ARRIVE_STATION) }, Modifier.fillMaxWidth()) { Text("ARRIVED AT PICKUP STATION") }
            ShiftState.AT_STATION -> {
                Text("Station wait and loading time are included in actual work time.")
                Button({ vm.flexAction(FlexAction.START_DELIVERIES) }, Modifier.fillMaxWidth()) { Text("LOADED — START DELIVERIES") }
                OutlinedButton({ vm.flexAction(FlexAction.RELEASE_NO_ROUTE) }, Modifier.fillMaxWidth()) { Text("Released — no route (block pay expected)") }
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
        if (state == ShiftState.EN_ROUTE_STATION || state == ShiftState.AT_STATION) {
            OutlinedButton({ showCancellation = true }, Modifier.fillMaxWidth()) { Text("Canceled / pulled (unpaid)") }
        }
    }
    if (showCancellation) AlertDialog(
        onDismissRequest = { showCancellation = false },
        title = { Text("Record an unpaid cancellation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Travel time and mileage stay in this outing. Expected pay will be $0 unless a payout is later confirmed.")
                Text("If the station sent you home because no route was available, use Released — no route instead.")
                FlexCancellationReason.entries.forEach { option ->
                    Row(Modifier.fillMaxWidth().selectable(selected = reason == option.name, role = Role.RadioButton, onClick = { reason = option.name })) {
                        RadioButton(selected = reason == option.name, onClick = null)
                        Text(option.label, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(note, { note = it.take(2000) }, label = { Text("Notes / support case (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = {
                vm.cancelFlex(FlexCancellationReason.valueOf(reason), note)
                showCancellation = false
            }) { Text("Record cancellation") }
        },
        dismissButton = { TextButton({ showCancellation = false }) { Text("Keep tracking") } },
    )
}

@Composable
private fun FlexPayEditor(shift: ShiftEntity, onConfirmPay: (Long?) -> Unit) {
    var show by rememberSaveable(shift.id) { mutableStateOf(false) }
    var amount by rememberSaveable(shift.id, shift.flexConfirmedPayCents) {
        mutableStateOf(shift.flexConfirmedPayCents?.let { "%.2f".format(java.util.Locale.US, it / 100.0) }.orEmpty())
    }
    val parsed = remember(amount) { runCatching { FlexPay.parseConfirmedPay(amount) } }
    OutlinedButton({ show = true }, Modifier.fillMaxWidth()) { Text(if (shift.flexConfirmedPayCents == null) "Confirm actual Flex pay" else "Update confirmed Flex pay") }
    if (show) AlertDialog(
        onDismissRequest = { show = false },
        title = { Text("Actual Flex pay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter the amount shown in your Flex earnings. Keep an appeal unpaid until payment is confirmed. Leave blank to clear a previous confirmation.")
                OutlinedTextField(amount, { amount = it }, label = { Text("Confirmed pay ($)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                parsed.exceptionOrNull()?.let { Text(it.message.orEmpty(), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = parsed.isSuccess, onClick = { onConfirmPay(parsed.getOrNull()); show = false }) { Text("Save pay") } },
        dismissButton = { TextButton({ show = false }) { Text("Back") } },
    )
}

@Composable
fun FlexSummary(shift: ShiftEntity, onConfirmPay: ((Long?) -> Unit)? = null) {
    shift.flexStation?.let { Text("Pickup: $it") }
    val outcome = shift.flexOutcome
    val cancelled = outcome == FlexOutcome.CANCELLED.name
    val pay = FlexPay.amountCents(shift.flexBlockPayCents, outcome, shift.flexConfirmedPayCents)
    val payLabel = if (shift.flexConfirmedPayCents != null) "Confirmed pay" else if (cancelled) "Expected pay (unpaid)" else "Expected block pay"
    when (outcome) {
        FlexOutcome.CANCELLED.name -> Text("Canceled / pulled", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        FlexOutcome.RELEASED_NO_ROUTE.name -> Text("Released without a route", fontWeight = FontWeight.Bold)
        FlexOutcome.COMPLETED.name -> Text("Route completed", fontWeight = FontWeight.Bold)
    }
    shift.flexCancellationReason?.let { saved ->
        Text(FlexCancellationReason.entries.firstOrNull { it.name == saved }?.label ?: saved)
    }
    shift.flexCancellationNote?.let { Text(it) }
    val start = shift.flexBlockStartEpochMs
    val end = shift.flexBlockEndEpochMs
    val offered = shift.flexBlockPayCents
    if (start != null && end != null && offered != null && pay != null) {
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        Text("Scheduled: ${format.format(Date(start))} — ${format.format(Date(end))}")
        Text("Original offer: $%.2f".format(offered / 100.0))
        Text("$payLabel: $%.2f".format(pay / 100.0), fontWeight = FontWeight.Bold)
        if (!cancelled) Text("$%.2f/hr scheduled".format(FlexBlock.hourly(pay, end - start)))
        val arrived = shift.flexStationArrivedAtEpochMs
        val finished = shift.endedAtEpochMs
        if (arrived != null && finished != null) {
            Text("Actual station time: %.0f min (includes waiting and any route / required returns)".format((finished - arrived) / 60_000.0))
            FlexBlock.hourly(pay, finished - arrived)?.let { Text("$payLabel / actual work: $%.2f/hr".format(it)) }
        }
        val home = shift.correctedHomeArrivedAtEpochMs ?: shift.homeArrivedAtEpochMs
        if (home != null) {
            Text("Door-to-door: %.0f min".format((home - shift.startedAtEpochMs) / 60_000.0))
            FlexBlock.hourly(pay, home - shift.startedAtEpochMs)?.let { Text("$payLabel / door-to-door: $%.2f/hr gross".format(it)) }
        }
        shift.endOdometer?.minus(shift.startOdometer)?.takeIf { it > 0 }?.let {
            Text("$payLabel / total outing miles: $%.2f/mi gross".format(pay / 100.0 / it))
        }
        Text(when {
            shift.flexConfirmedPayCents != null -> "Confirmed gross pay. Review tips and vehicle costs in OPS for final profit."
            cancelled -> "No pay recorded for this cancellation. Update actual pay if your appeal results in payment."
            else -> "Block pay is expected, not confirmed payout or profit. Confirm actual pay when it appears in Flex earnings."
        }, style = MaterialTheme.typography.bodySmall)
        if (shift.endedAtEpochMs != null && onConfirmPay != null) FlexPayEditor(shift, onConfirmPay)
    }
}
