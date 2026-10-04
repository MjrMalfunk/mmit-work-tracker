package com.midwestmanagedit.tracker.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GpsStatus {
    STOPPED, WAITING, RECORDING, STALE, PERMISSION_REQUIRED, APPROXIMATE_ONLY,
    LOCATION_DISABLED, REQUEST_FAILED, SAVE_FAILED, SERVICE_UNAVAILABLE,
}

data class GpsHealth(
    val shiftId: String? = null,
    val status: GpsStatus = GpsStatus.STOPPED,
    val checkedAtElapsedMs: Long = 0,
    val lastSavedAtEpochMs: Long? = null,
    val lastSavedAtElapsedMs: Long? = null,
    val accuracyMeters: Float? = null,
) {
    // A dead service cannot update its own status. The visible screen ages its heartbeat.
    fun effectiveStatus(now: Long): GpsStatus = when {
        status == GpsStatus.STOPPED -> status
        now - checkedAtElapsedMs > SERVICE_TIMEOUT_MS -> GpsStatus.SERVICE_UNAVAILABLE
        status == GpsStatus.RECORDING && lastSavedAtElapsedMs?.let { now - it > FIX_TIMEOUT_MS } == true -> GpsStatus.STALE
        else -> status
    }

    companion object {
        const val FIX_TIMEOUT_MS = 120_000L
        const val SERVICE_TIMEOUT_MS = 90_000L
        const val DIAGNOSTIC_EVENT = "GPS_HEALTH"
    }
}

object GpsHealthState {
    private val mutable = MutableStateFlow(GpsHealth())
    val state = mutable.asStateFlow()
    fun publish(health: GpsHealth) { mutable.value = health }
}

/** Monotonic clocks prevent wall-clock changes from hiding a stalled subscription. */
class GpsRecoveryPolicy(private val startedAt: Long) {
    private var lastAttemptAt: Long? = null
    private var pendingAt: Long? = null
    private var registered = false
    private var requestFailed = false
    private var saveFailed = false
    var lastSavedAt: Long? = null
        private set

    fun shouldRequest(now: Long, permission: Boolean, enabled: Boolean, force: Boolean = false): Boolean {
        if (!permission || !enabled || pendingAt != null) return false
        val retryDelay = if (force) 5_000L else RETRY_INTERVAL_MS
        if (lastAttemptAt?.let { now - it < retryDelay } == true) return false
        return force || !registered || requestFailed ||
            now - (lastSavedAt ?: startedAt) > GpsHealth.FIX_TIMEOUT_MS
    }

    fun requestStarted(now: Long) {
        lastAttemptAt = now
        pendingAt = now
    }

    fun requestFinished(success: Boolean) {
        pendingAt = null
        registered = success
        requestFailed = !success
    }

    fun expirePending(now: Long): Boolean {
        if (pendingAt?.let { now - it >= REQUEST_TIMEOUT_MS } != true) return false
        requestFinished(false)
        return true
    }

    fun savedFix(elapsedMs: Long) {
        lastSavedAt = elapsedMs
        saveFailed = false
    }

    fun saveFailed() { saveFailed = true }

    fun status(now: Long, permission: Boolean, precise: Boolean, enabled: Boolean): GpsStatus = when {
        !permission -> GpsStatus.PERMISSION_REQUIRED
        !enabled -> GpsStatus.LOCATION_DISABLED
        !precise -> GpsStatus.APPROXIMATE_ONLY
        saveFailed -> GpsStatus.SAVE_FAILED
        lastSavedAt?.let { now - it <= GpsHealth.FIX_TIMEOUT_MS } == true -> GpsStatus.RECORDING
        requestFailed -> GpsStatus.REQUEST_FAILED
        now - (lastSavedAt ?: startedAt) > GpsHealth.FIX_TIMEOUT_MS -> GpsStatus.STALE
        else -> GpsStatus.WAITING
    }

    companion object {
        const val RETRY_INTERVAL_MS = 60_000L
        const val REQUEST_TIMEOUT_MS = 30_000L

        fun usableFix(now: Long, elapsedMs: Long, accuracy: Float, latitude: Double, longitude: Double): Boolean =
            elapsedMs > 0 && now - elapsedMs in 0..GpsHealth.FIX_TIMEOUT_MS &&
                accuracy.isFinite() && accuracy in 0f..100f &&
                latitude.isFinite() && latitude in -90.0..90.0 &&
                longitude.isFinite() && longitude in -180.0..180.0
    }
}
