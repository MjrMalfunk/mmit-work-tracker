package com.midwestmanagedit.tracker.tracking

import android.location.Location
import com.midwestmanagedit.tracker.domain.GeoStamp

object LocationMemory {
    @Volatile private var latest: Location? = null

    fun update(location: Location) {
        latest = location
    }

    fun stamp(now: Long = System.currentTimeMillis()): GeoStamp {
        val location = latest?.takeIf { now - it.time <= 120_000L }
        return GeoStamp(
            occurredAtEpochMs = now,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.accuracy,
        )
    }
}
