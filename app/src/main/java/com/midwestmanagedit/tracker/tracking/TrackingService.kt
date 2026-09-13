package com.midwestmanagedit.tracker.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.midwestmanagedit.tracker.MainActivity
import com.midwestmanagedit.tracker.R
import com.midwestmanagedit.tracker.TrackerApplication
import com.midwestmanagedit.tracker.data.LocationPointEntity
import com.midwestmanagedit.tracker.domain.AcquisitionMode
import com.midwestmanagedit.tracker.domain.ShiftState
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { (application as TrackerApplication).repository }
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var commandJob: Job? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::record)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification("Starting tracker…"),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        startLocations()
        refreshNotification()
        scope.launch {
            repository.observeActiveShift().collect { refreshNotification() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        commandJob?.cancel()
        commandJob = scope.launch {
            val stamp = LocationMemory.stamp()
            runCatching {
                when (intent?.action) {
                    ACTION_PICKUP -> repository.pickup(stamp)
                    ACTION_DROP_OFF -> repository.dropOff(false, stamp)
                    ACTION_DROP_AND_NEXT -> repository.dropOff(true, stamp)
                    ACTION_MANUAL_RIDE -> repository.acceptOrQueueRide(AcquisitionMode.MANUAL_ACCEPT, stamp)
                    ACTION_AUTO_RIDE -> repository.acceptOrQueueRide(AcquisitionMode.AUTO_QUEUE, stamp)
                    // NEW: cancel active ride
                    ACTION_CANCEL_RIDE -> repository.cancelActiveRide(stamp)
                }
            }
            refreshNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startLocations() {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateDistanceMeters(15f)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        fused.requestLocationUpdates(request, callback, mainLooper)
    }

    private fun record(location: Location) {
        LocationMemory.update(location)
        if (location.accuracy > 100f) return
        scope.launch {
            val snapshot = repository.activeSnapshot() ?: return@launch
            repository.recordLocation(
                LocationPointEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = snapshot.shiftId,
                    rideId = snapshot.activeRideId,
                    phase = snapshot.shiftState,
                    occurredAtEpochMs = location.time,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() },
                ),
            )
        }
    }

    private fun refreshNotification() = scope.launch {
        val snapshot = repository.activeSnapshot()
        val state = snapshot?.shiftState?.let(ShiftState::valueOf)
        val builder = NotificationCompat.Builder(this@TrackingService, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("MMIT Work Tracker")
            .setContentText(state?.label() ?: "Tracker ready")
            .setContentIntent(activityIntent())
            .setOngoing(snapshot != null)
            .setOnlyAlertOnce(true)

        when (state) {
            ShiftState.AVAILABLE -> builder.addAction(action("Ride accepted", ACTION_MANUAL_RIDE))
            ShiftState.EN_ROUTE_PICKUP -> {
                // NEW: cancel ride from notification
                builder.addAction(action("Cancel ride", ACTION_CANCEL_RIDE))
                builder.addAction(action("Picked up", ACTION_PICKUP))
            }
            ShiftState.PASSENGER -> {
                builder.addAction(action("Drop off", ACTION_DROP_OFF))
                builder.addAction(action("Drop + next", ACTION_DROP_AND_NEXT))
                builder.addAction(action("Queue ride", ACTION_MANUAL_RIDE))
            }
            else -> Unit
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, builder.build())
    }

    private fun notification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
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
        val channel = NotificationChannel(CHANNEL_ID, "Active work tracking", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun ShiftState.label(): String = name.lowercase().replace('_', ' ')

    companion object {
        const val ACTION_START = "tracker.START"
        const val ACTION_PICKUP = "tracker.PICKUP"
        const val ACTION_DROP_OFF = "tracker.DROP_OFF"
        const val ACTION_DROP_AND_NEXT = "tracker.DROP_AND_NEXT"
        const val ACTION_MANUAL_RIDE = "tracker.MANUAL_RIDE"
        const val ACTION_AUTO_RIDE = "tracker.AUTO_RIDE"
        // NEW: cancel ride action
        const val ACTION_CANCEL_RIDE = "tracker.CANCEL_RIDE"
        private const val CHANNEL_ID = "active_tracking"
        private const val NOTIFICATION_ID = 41
    }
}
