package com.midwestmanagedit.tracker

import android.app.Application
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import com.midwestmanagedit.tracker.inventory.parseInventorySnapshot
import com.midwestmanagedit.tracker.inventory.readBytesLimited
import org.json.JSONObject
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midwestmanagedit.tracker.data.RideEntity
import com.midwestmanagedit.tracker.data.ShiftEntity
import com.midwestmanagedit.tracker.data.ShiftMaterialUsageEntity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.File
import com.midwestmanagedit.tracker.export.ShiftExporter

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TrackerRepository = (application as TrackerApplication).repository
    private val inventoryRepository = (application as TrackerApplication).inventoryCatalog
    val inventoryCatalog = inventoryRepository.state
    val savingMaterial = MutableStateFlow(false)
    private val connectivity = application.getSystemService(ConnectivityManager::class.java)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { refreshInventory() }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) refreshInventory()
        }
    }

    init {
        connectivity.registerDefaultNetworkCallback(networkCallback)
        refreshInventory()
    }

    fun refreshInventory(force: Boolean = false) {
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { inventoryRepository.refresh(force) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message.value = "Inventory could not refresh. Your saved catalog is still available." }
        }
    }

    fun connectInventory(serverUrl: String, email: String, password: String, code: String, onConnected: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { inventoryRepository.signIn(serverUrl, email, password, code) }
                if (inventoryCatalog.value.authenticated) onConnected()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message.value = error.message ?: "Could not connect to OPS V2." }
        }
    }

    fun disconnectInventory() = act {
        withContext(Dispatchers.IO) { inventoryRepository.signOut() }
    }

    fun importInventoryCatalog(uri: Uri) = act {
        withContext(Dispatchers.IO) {
            val input = getApplication<Application>().contentResolver.openInputStream(uri) ?: error("Could not open this catalog file.")
            val imported = input.use { parseInventorySnapshot(JSONObject(String(it.readBytesLimited(2 * 1024 * 1024), Charsets.UTF_8))) }
            inventoryRepository.importCatalog(imported)
        }
        message.value = "Inventory catalog saved. You can search these items offline."
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }

    val activeShift: StateFlow<ShiftEntity?> = repository.observeActiveShift()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pendingRides: StateFlow<List<RideEntity>> = activeShift
        .flatMapLatest { shift -> shift?.let { repository.observePendingRides(it.id) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val materialUsage: StateFlow<List<ShiftMaterialUsageEntity>> = activeShift
        .flatMapLatest { shift -> shift?.let { repository.observeMaterialUsage(it.id) } ?: flowOf(emptyList()) }
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
    fun addFieldNationMaterial(sku: String, quantity: Double, notes: String, onSaved: () -> Unit = {}) {
        if (savingMaterial.value) return
        val shiftId = activeShift.value?.id ?: run { message.value = "No active outing."; return }
        savingMaterial.value = true
        viewModelScope.launch {
            try {
                check(inventoryCatalog.value.items.any { it.sku == sku }) { "Select an item from your inventory catalog." }
                repository.addFieldNationMaterialUsage(shiftId, sku, quantity, notes)
                onSaved()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message.value = error.message ?: "Material use could not be recorded." }
            finally { savingMaterial.value = false }
        }
    }

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

    fun saveOfflineOdometer(odometer: Double) = act {
        repository.saveOfflineOdometer(activeShift.value?.id ?: error("No active outing."), odometer)
    }

    fun correctHomeArrival(minutesLate: Double) = act {
        repository.correctHomeArrival(activeShift.value?.id ?: error("No active outing."), minutesLate)
    }

    fun lockHomeArrival() = act {
        val shift = activeShift.value ?: error("No outing to finish.")
        repository.lockHomeArrival(shift.id, LocationMemory.stamp())
        getApplication<Application>().stopService(Intent(getApplication(), TrackingService::class.java))
    }

    fun finishHomeArrival(odometer: Double, offlineReadingDraft: String? = null) = act {
        val shift = activeShift.value ?: error("No outing to finish.")
        repository.finishHomeArrival(shift.id, odometer, offlineReadingDraft)
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
