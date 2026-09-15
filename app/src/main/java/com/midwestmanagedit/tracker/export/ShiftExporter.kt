package com.midwestmanagedit.tracker.export

import android.content.Context
import com.midwestmanagedit.tracker.data.TrackerRepository
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class ShiftExporter(
    private val context: Context,
    private val repository: TrackerRepository,
) {
    suspend fun export(shiftId: String): File {
        val bundle = repository.shiftWithRides(shiftId) ?: error("Shift not found.")
        val events = repository.events(shiftId)
        // Android can hand the location callback a cached point a few milliseconds
        // before the start tap has been committed.  It is not part of the outing.
        // Likewise, no point after the recorded completion belongs in an export.
        val points = repository.locations(shiftId).filter {
            ExportTimeWindow.contains(
                timestamp = it.occurredAtEpochMs,
                startedAt = bundle.shift.startedAtEpochMs,
                completedAt = bundle.shift.completedAtEpochMs,
            )
        }

        val root = JSONObject()
            .put("schema", "mmit.work-tracker.v2")
            .put("exportedAtEpochMs", System.currentTimeMillis())
            .put("shift", JSONObject().apply {
                put("id", bundle.shift.id)
                put("platform", bundle.shift.platform)
                put("queueModeAtEnd", bundle.shift.queueMode)
                put("workOrderNumber", bundle.shift.workOrderNumber)
                put("roundTripExpected", bundle.shift.roundTripExpected)
                put("startedAtEpochMs", bundle.shift.startedAtEpochMs)
                put("wentOfflineAtEpochMs", bundle.shift.endedAtEpochMs)
                put("homeArrivedAtEpochMs", bundle.shift.homeArrivedAtEpochMs)
                put("completedAtEpochMs", bundle.shift.completedAtEpochMs)
                put("startOdometer", bundle.shift.startOdometer)
                put("endOdometer", bundle.shift.endOdometer)
            })
            .put("rides", JSONArray().apply {
                bundle.rides.sortedBy { it.sequence }.forEach { ride ->
                    put(JSONObject().apply {
                        put("id", ride.id)
                        put("sequence", ride.sequence)
                        put("acquisitionMode", ride.acquisitionMode)
                        put("state", ride.state)
                        put("queuedAtEpochMs", ride.queuedAtEpochMs)
                        put("trackingStartedAtEpochMs", ride.trackingStartedAtEpochMs)
                        put("pickupAtEpochMs", ride.pickupAtEpochMs)
                        put("dropoffAtEpochMs", ride.dropoffAtEpochMs)
                    })
                }
            })
            .put("events", JSONArray().apply {
                events.forEach { event ->
                    put(JSONObject().apply {
                        put("id", event.id)
                        put("rideId", event.rideId)
                        put("type", event.type)
                        put("occurredAtEpochMs", event.occurredAtEpochMs)
                        put("latitude", event.latitude)
                        put("longitude", event.longitude)
                        put("accuracyMeters", event.accuracyMeters)
                        put("payload", JSONObject(event.payloadJson))
                    })
                }
            })
            .put("locations", JSONArray().apply {
                points.forEach { point ->
                    put(JSONObject().apply {
                        put("rideId", point.rideId)
                        put("phase", point.phase)
                        put("occurredAtEpochMs", point.occurredAtEpochMs)
                        put("latitude", point.latitude)
                        put("longitude", point.longitude)
                        put("accuracyMeters", point.accuracyMeters)
                        put("speedMetersPerSecond", point.speedMetersPerSecond)
                    })
                }
            })

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(directory, "mmit-work-$shiftId.json").also { it.writeText(root.toString(2)) }
    }
}
