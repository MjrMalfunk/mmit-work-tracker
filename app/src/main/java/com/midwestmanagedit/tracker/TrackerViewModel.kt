package com.midwestmanagedit.tracker

import android.app.Application
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midwestmanagedit.tracker.data.RideEntity
import com.midwestmanagedit.tracker.data.ShiftEntity
import com.midwestmanagedit.tracker.data.TrackerRepository
import com.midwestmanagedit.tracker.domain.AcquisitionMode
import com.midwestmanagedit.tracker.domain.Platform
import com.midwestmanagedit.tracker.domain.QueueMode
import com.midwestmanagedit.tracker.tracking.LocationMemory
import com.midwestmanagedit.tracker.tracking.TrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import com.midwestmanagedit.tracker.export.ShiftExporter

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TrackerRepository = (application as TrackerApplication).repository

    val activeShift: StateFlow<ShiftEntity?> = repository.observeActiveShift()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pendingRides: StateFlow<List<RideEntity>> = activeShift
        .flatMapLatest { shift -> shift?.let { repository.observePendingRides(it.id) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestCompleted: StateFlow<ShiftEntity?> = repository.observeLatestCompletedShift()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val completedOutings: StateFlow<List<ShiftEntity>> = repository.observeCompletedShifts(500)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val message = MutableStateFlow<String?>(null)

    fun start(platform: Platform, queueMode: QueueMode, odometer: Double, firstRide: Boolean) = act {
        requireLocation()
        repository.startShift(platform, queueMode, odometer, LocationMemory.stamp(), firstRide)
        startTrackingService()
    }

    fun startFieldNation(workOrderNumber: String, roundTrip: Boolean, odometer: Double) = act {
        requireLocation()
        repository.startFieldNation(workOrderNumber, roundTrip, odometer, LocationMemory.stamp())
        startTrackingService()
    }

    fun fieldArriveSite() = act { repository.fieldArriveSite(LocationMemory.stamp()) }
    fun fieldStartWork() = act { repository.fieldStartWork(LocationMemory.stamp()) }
    fun fieldCompleteWork() = act { repository.fieldCompleteWork(LocationMemory.stamp()) }
    fun fieldCheckOut() = act { repository.fieldCheckOut(LocationMemory.stamp()) }

    fun acceptRide(mode: AcquisitionMode) = act {
        repository.acceptRide(mode, LocationMemory.stamp())
    }

    fun queueRide(mode: AcquisitionMode) = act {
        repository.queueRide(mode, LocationMemory.stamp())
    }

    // OLD: pickup() — removed in new repo
    fun pickup() = act {
    repository.pickupPassenger(LocationMemory.stamp())
}

    // OLD: dropOff(startNext) — removed in new repo
    fun dropOff(startNext: Boolean) = act {
        repository.dropoffPassenger(startNext, LocationMemory.stamp())
    }

    fun startPending() = act { repository.startOldestPending(LocationMemory.stamp()) }
    fun losePending() = act { repository.loseOldestPending(LocationMemory.stamp()) }

    fun cancelRide() = act {
        repository.cancelActiveRide(LocationMemory.stamp())
    }

    // OLD: changeQueueMode() — removed in new repo
    fun queueMode(mode: QueueMode) = act {
        // No queue mode change in new repo — ignore or log
        message.value = "Queue mode changes are not supported in this version."
    }

    fun breakMode(start: Boolean) = act { repository.setBreak(start, LocationMemory.stamp()) }
    fun endShift() = act { repository.endShift(LocationMemory.stamp()) }

    fun lockHomeArrival() = act {
        val shift = activeShift.value ?: error("No outing to finish.")
        repository.lockHomeArrival(shift.id, LocationMemory.stamp())
        getApplication<Application>().stopService(Intent(getApplication(), TrackingService::class.java))
    }

    fun finishHomeArrival(odometer: Double) = act {
        val shift = activeShift.value ?: error("No outing to finish.")
        repository.finishHomeArrival(shift.id, odometer)
    }

    fun correctStartingOdometer(odometer: Double) = act {
        val shift = activeShift.value ?: error("No outing to correct.")
        repository.correctStartingOdometer(shift.id, odometer)
    }

    fun correctFieldNationWorkOrder(workOrderNumber: String) = act {
        val shift = activeShift.value ?: error("No outing to correct.")
        repository.correctFieldNationWorkOrder(shift.id, workOrderNumber)
    }

    fun clearMessage() { message.value = null }

    fun exportLatest(onReady: (File) -> Unit) = act {
        val shift = latestCompleted.value ?: error("No completed outing to export.")
        onReady(ShiftExporter(getApplication<Application>(), repository).export(shift.id))
    }

    fun exportShift(shift: ShiftEntity, onReady: (File) -> Unit) = act {
        onReady(ShiftExporter(getApplication<Application>(), repository).export(shift.id))
    }

    fun saveClosingNote(shift: ShiftEntity, note: String) = act {
        repository.saveClosingNote(shift.id, note)
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { message.value = it.message ?: "That action could not be recorded." }
        }
    }

    private fun requireLocation() {
        val app = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION)
        check(fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            "Location permission is required to start tracking."
        }
    }

    private fun startTrackingService() {
        val app = getApplication<Application>()
        ContextCompat.startForegroundService(
            app,
            Intent(app, TrackingService::class.java).setAction(TrackingService.ACTION_START),
        )
    }
}
