package com.midwestmanagedit.tracker.tracking

import android.location.Location
import android.os.SystemClock
import com.midwestmanagedit.tracker.domain.GeoStamp

object LocationMemory {
    @Volatile private var latest: Location? = null

    fun update(location: Location) {
        latest = location
    }

    fun clear() { latest = null }

    fun stamp(now: Long = System.currentTimeMillis()): GeoStamp {
        val elapsedNow = SystemClock.elapsedRealtime()
        val location = latest?.takeIf {
            GpsRecoveryPolicy.usableFix(elapsedNow, it.elapsedRealtimeNanos / 1_000_000L,
                it.accuracy, it.latitude, it.longitude)
        }
        return GeoStamp(
            occurredAtEpochMs = now,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.accuracy,
        )
    }
}
