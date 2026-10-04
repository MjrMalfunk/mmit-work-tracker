package com.midwestmanagedit.tracker.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.midwestmanagedit.tracker.MainActivity
import com.midwestmanagedit.tracker.TrackerApplication
import com.midwestmanagedit.tracker.data.LocationPointEntity
import com.midwestmanagedit.tracker.domain.AcquisitionMode
import com.midwestmanagedit.tracker.domain.ShiftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class TrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { (application as TrackerApplication).repository }
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val healthMutex = Mutex()
    private val commandMutex = Mutex()
    private var shiftId: String? = null
    private var policy: GpsRecoveryPolicy? = null
    private var generation = 0
    private var destroyed = false
    private var foregroundStarted = false
    private var lastStatus: GpsStatus? = null
    private var providerAvailable: Boolean? = null
    private var lastSavedEpoch: Long? = null
    private var lastAccuracy: Float? = null
    private var lastRejectedAt: Long? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::record)
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            val available = availability.isLocationAvailable
            scope.launch {
                healthMutex.withLock {
                    if (providerAvailable != available) {
                        providerAvailable = available
                        diagnostic("PROVIDER_AVAILABILITY", JSONObject().put("available", available))
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification("Starting tracker…"),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
            foregroundStarted = true
        } catch (error: Exception) {
            Log.e("MMIT-GPS", "Foreground location service could not start", error)
            GpsHealthState.publish(GpsHealthState.state.value.copy(
                status = GpsStatus.SERVICE_UNAVAILABLE, checkedAtElapsedMs = SystemClock.elapsedRealtime(),
            ))
            stopSelf()
            return
        }
        scope.launch {
            repository.observeActiveShift().catch { error ->
                Log.e("MMIT-GPS", "Outing observer failed", error)
                GpsHealthState.publish(GpsHealthState.state.value.copy(
                    status = GpsStatus.SAVE_FAILED, checkedAtElapsedMs = SystemClock.elapsedRealtime(),
                ))
            }.collect {
                checkTracking("OUTING_CHANGED")
                refreshNotification()
            }
        }
        scope.launch {
            while (true) {
                delay(30_000L)
                checkTracking("HEALTH_CHECK")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregroundStarted) return START_NOT_STICKY
        // Capture the button time immediately, and never cancel an earlier pending ride tap.
        val stamp = LocationMemory.stamp()
        scope.launch {
            commandMutex.withLock {
                try {
                when (intent?.action) {
                    ACTION_PICKUP ->
                        repository.pickupPassenger(stamp)

                    ACTION_DROP_OFF ->
                        repository.dropoffPassenger(startNext = false, stamp)

                    ACTION_DROP_AND_NEXT ->
                        repository.dropoffPassenger(startNext = true, stamp)

                    ACTION_ACCEPT_RIDE ->
                        repository.acceptRide(AcquisitionMode.MANUAL_ACCEPT, stamp)

                    ACTION_QUEUE_RIDE ->
                        repository.queueRide(AcquisitionMode.MANUAL_ACCEPT, stamp)

                    ACTION_CANCEL_RIDE ->
                        repository.cancelActiveRide(stamp)
                }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    Log.e("MMIT-GPS", "Notification action could not be saved", error)
                    diagnostic("ACTION_FAILED", JSONObject().put("action", intent?.action).put("error", error.javaClass.simpleName))
                }
            }
            checkTracking("APP_OR_NOTIFICATION", intent?.action == ACTION_RETRY)
            refreshNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        destroyed = true
        generation++
        runCatching { fused.removeLocationUpdates(callback) }
        val health = GpsHealthState.state.value
        if (health.status != GpsStatus.STOPPED) GpsHealthState.publish(health.copy(
            status = GpsStatus.SERVICE_UNAVAILABLE, checkedAtElapsedMs = SystemClock.elapsedRealtime(),
        ))
        LocationMemory.clear()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun checkTracking(reason: String, force: Boolean = false) = healthMutex.withLock {
        if (destroyed) return@withLock
        try {
            val shift = repository.dao.activeShift()
            if (shift == null || shift.homeArrivedAtEpochMs != null) {
                GpsHealthState.publish(GpsHealth(shiftId = shift?.id, checkedAtElapsedMs = SystemClock.elapsedRealtime()))
                LocationMemory.clear()
                stopSelf()
                return@withLock
            }
            val now = SystemClock.elapsedRealtime()
            if (shiftId != shift.id) {
                shiftId = shift.id
                policy = GpsRecoveryPolicy(now)
                lastSavedEpoch = null
                lastAccuracy = null
                lastStatus = null
                lastRejectedAt = null
                LocationMemory.clear()
                val power = getSystemService(PowerManager::class.java)
                val details = JSONObject()
                    .put("deviceModel", Build.MODEL).put("androidSdk", Build.VERSION.SDK_INT)
                    .put("androidVersion", Build.VERSION.RELEASE)
                    .put("appVersion", packageManager.getPackageInfo(packageName, 0).versionName)
                    .put("batterySaver", power.isPowerSaveMode)
                    .put("batteryOptimizationExempt", power.isIgnoringBatteryOptimizations(packageName))
                if (Build.VERSION.SDK_INT >= 30) {
                    // This is historical context, not proof that a past GPS gap was an app exit.
                    runCatching { getSystemService(ActivityManager::class.java)
                        .getHistoricalProcessExitReasons(packageName, 0, 1).firstOrNull() }.getOrNull()?.let { exit ->
                        details.put("previousProcessExit", JSONObject().put("timestamp", exit.timestamp)
                            .put("reason", exit.reason).put("status", exit.status))
                    }
                }
                diagnostic("SERVICE_STARTED", details)
            }
            val current = policy ?: return@withLock
            if (current.expirePending(now)) {
                generation++ // Late Task completions must not overwrite the newer request state.
                diagnostic("REQUEST_TIMEOUT")
            }
            publishHealth()
            if (current.shouldRequest(now, hasLocation(), locationEnabled(), force)) {
                startLocations(reason)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            Log.e("MMIT-GPS", "GPS health check failed", error)
            GpsHealthState.publish(GpsHealthState.state.value.copy(
                status = GpsStatus.SAVE_FAILED, checkedAtElapsedMs = SystemClock.elapsedRealtime(),
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun startLocations(reason: String) {
        val current = policy ?: return
        val requestGeneration = ++generation
        val owner = shiftId
        current.requestStarted(SystemClock.elapsedRealtime())
        diagnostic("REQUEST_STARTED", JSONObject().put("reason", reason))
        try {
            // Time-based samples also verify GPS while stopped at a pickup or on a break.
            // Reusing this callback replaces its subscription instead of adding another one.
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
                .setMinUpdateIntervalMillis(5_000L)
                .setMaxUpdateAgeMillis(0L)
                .build()
            fused.requestLocationUpdates(request, callback, mainLooper).addOnCompleteListener { task ->
                scope.launch {
                    healthMutex.withLock {
                        if (destroyed || generation != requestGeneration || shiftId != owner) return@withLock
                        current.requestFinished(task.isSuccessful)
                        val details = JSONObject().put("error", task.exception?.javaClass?.simpleName)
                        (task.exception as? ApiException)?.let { details.put("statusCode", it.statusCode) }
                        diagnostic(if (task.isSuccessful) "REQUEST_REGISTERED" else "REQUEST_FAILED", details)
                        publishHealth()
                    }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            current.requestFinished(false)
            diagnostic("REQUEST_FAILED", JSONObject().put("error", error.javaClass.simpleName))
            publishHealth()
        }
    }

    private fun record(location: Location) {
        val elapsed = location.elapsedRealtimeNanos / 1_000_000L
        val receivedAt = SystemClock.elapsedRealtime()
        if (!location.hasAccuracy() || !GpsRecoveryPolicy.usableFix(receivedAt, elapsed,
                location.accuracy, location.latitude, location.longitude)) {
            scope.launch {
                healthMutex.withLock {
                    if (destroyed || lastRejectedAt?.let { receivedAt - it < 60_000L } == true) return@withLock
                    lastRejectedAt = receivedAt
                    diagnostic("FIX_REJECTED", JSONObject().put("ageMs", receivedAt - elapsed)
                        .put("accuracyMeters", location.accuracy.takeIf { location.hasAccuracy() && it.isFinite() }))
                }
            }
            return
        }
        scope.launch {
            healthMutex.withLock {
                val owner = shiftId ?: return@withLock
                val current = policy ?: return@withLock
                if (destroyed || current.lastSavedAt?.let { elapsed <= it } == true) return@withLock
                try {
                    if (repository.saveGpsPoint(LocationPointEntity(
                        id = "$owner:${location.elapsedRealtimeNanos}", shiftId = owner,
                        rideId = null, phase = "", occurredAtEpochMs = location.time,
                        latitude = location.latitude, longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                        speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() },
                    ))) {
                        current.savedFix(elapsed)
                        lastSavedEpoch = location.time
                        lastAccuracy = location.accuracy
                        LocationMemory.update(location)
                        publishHealth()
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    current.saveFailed()
                    Log.e("MMIT-GPS", "Location could not be saved", error)
                    publishHealth()
                }
            }
        }
    }

    private fun hasPreciseLocation() = ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasLocation() = hasPreciseLocation() || ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun locationEnabled() = LocationManagerCompat.isLocationEnabled(getSystemService(LocationManager::class.java))

    private suspend fun publishHealth() {
        val current = policy ?: return
        val now = SystemClock.elapsedRealtime()
        val status = current.status(now, hasLocation(), hasPreciseLocation(), locationEnabled())
        GpsHealthState.publish(GpsHealth(shiftId, status, now, lastSavedEpoch, current.lastSavedAt, lastAccuracy))
        if (status != lastStatus) {
            lastStatus = status
            diagnostic("STATUS_CHANGED", JSONObject().put("status", status.name)
                .put("lastSavedAtEpochMs", lastSavedEpoch)
                .put("locationEnabled", locationEnabled()).put("precisePermission", hasPreciseLocation())
                .put("batterySaver", getSystemService(PowerManager::class.java).isPowerSaveMode))
            refreshNotification()
        }
    }

    private suspend fun diagnostic(reason: String, details: JSONObject = JSONObject()) {
        val owner = shiftId ?: return
        try { repository.recordGpsDiagnostic(owner, details.put("reason", reason).toString()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { Log.e("MMIT-GPS", "GPS diagnostic could not be saved", error) }
    }

    private fun refreshNotification() = scope.launch {
      try {
        val snapshot = repository.dao.activeShift()
        val state = snapshot?.state?.let(ShiftState::valueOf)

        val builder = NotificationCompat.Builder(this@TrackingService, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("MMIT Work Tracker")
            .setContentText(listOfNotNull(state?.label(), when (GpsHealthState.state.value.status) {
                GpsStatus.RECORDING -> "GPS recording"
                GpsStatus.WAITING -> "Waiting for GPS"
                GpsStatus.STOPPED -> null
                else -> "GPS needs attention — open tracker"
            }).joinToString(" • ").ifBlank { "Tracker ready" })
            .setContentIntent(activityIntent())
            .setOngoing(snapshot != null)
            .setOnlyAlertOnce(true)

        when (state) {
            ShiftState.AVAILABLE ->
                builder.addAction(action("Ride accepted", ACTION_ACCEPT_RIDE))

            ShiftState.EN_ROUTE_PICKUP -> {
                builder.addAction(action("Cancel ride", ACTION_CANCEL_RIDE))
                builder.addAction(action("Picked up", ACTION_PICKUP))
            }

            ShiftState.PASSENGER -> {
                builder.addAction(action("Drop off", ACTION_DROP_OFF))
                builder.addAction(action("Drop + next", ACTION_DROP_AND_NEXT))
                builder.addAction(action("Queue ride", ACTION_QUEUE_RIDE))
            }

            else -> Unit
        }

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, builder.build())
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (error: Exception) { Log.e("MMIT-GPS", "Tracking notification could not refresh", error) }
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("MMIT Work Tracker")
            .setContentText(text)
            .setContentIntent(activityIntent())
            .setOngoing(true)
            .build()

    private fun action(label: String, command: String): NotificationCompat.Action =
        NotificationCompat.Action.Builder(0, label, serviceIntent(command)).build()

    private fun activityIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        1,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, TrackingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active work tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun ShiftState.label(): String =
        name.lowercase().replace('_', ' ')

    companion object {
        const val ACTION_START = "tracker.START"
        const val ACTION_RETRY = "tracker.RETRY_GPS"
        const val ACTION_PICKUP = "tracker.PICKUP"
        const val ACTION_DROP_OFF = "tracker.DROP_OFF"
        const val ACTION_DROP_AND_NEXT = "tracker.DROP_AND_NEXT"
        const val ACTION_ACCEPT_RIDE = "tracker.ACCEPT_RIDE"
        const val ACTION_QUEUE_RIDE = "tracker.QUEUE_RIDE"
        const val ACTION_CANCEL_RIDE = "tracker.CANCEL_RIDE"
        private const val CHANNEL_ID = "active_tracking"
        private const val NOTIFICATION_ID = 41
    }
}
