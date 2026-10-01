package com.midwestmanagedit.tracker.export

import com.midwestmanagedit.tracker.data.LocationPointEntity
import com.midwestmanagedit.tracker.data.ShiftEntity
import com.midwestmanagedit.tracker.data.TrackingEventEntity
import com.midwestmanagedit.tracker.domain.OutingMileage
import org.json.JSONObject

/** Additive export contract: keep raw events and readings; downstream apps must not sum
 * odometer total + offline miles. GPS estimates are independent, not odometer readings.
 */
object GigMileageSummary {
    fun build(shift: ShiftEntity, locations: List<LocationPointEntity>, events: List<TrackingEventEntity>): JSONObject {
        val split = OutingMileage.split(shift.startOdometer, shift.offlineOdometer, shift.endOdometer)
        val offlineAt = shift.endedAtEpochMs
        val arrival = shift.correctedHomeArrivedAtEpochMs ?: shift.homeArrivedAtEpochMs
        val points = locations.map { OutingMileage.Point(it.occurredAtEpochMs, it.latitude, it.longitude, it.accuracyMeters) }
        val summary = JSONObject()
            .put("version", 1)
            .put("odometerScope", "ENTIRE_OUTING_INCLUDING_RETURN")
            .put("totalOdometerMiles", split.total ?: JSONObject.NULL)
            .put("beforeOfflineOdometerMiles", split.online ?: JSONObject.NULL)
            .put("offlineOdometerMiles", split.offline ?: JSONObject.NULL)
            .put("segmentOdometerSource", if (shift.offlineOdometer != null) "ODOMETER" else "NOT_RECORDED")
            .put("effectiveHomeArrivedAtEpochMs", arrival ?: JSONObject.NULL)
            .put("arrivalSource", if (shift.correctedHomeArrivedAtEpochMs != null) "DRIVER_CORRECTION" else "ARRIVAL_TAP")
        if (offlineAt != null) {
            // Breaks are off-duty time inside the online window, not extra return time.
            var breakStart: Long? = null
            var breakMs = 0L
            for (event in events.sortedBy { it.occurredAtEpochMs }) {
                val time = event.occurredAtEpochMs.coerceIn(shift.startedAtEpochMs, offlineAt)
                if (event.type == "BREAK_STARTED" && breakStart == null) breakStart = time
                if (event.type == "BREAK_ENDED" && breakStart != null) {
                    breakMs += time - breakStart
                    breakStart = null
                }
            }
            breakStart?.let { breakMs += offlineAt - it }
            summary.put("onlineMinutes", (offlineAt - shift.startedAtEpochMs - breakMs) / 60_000.0)
            summary.put("breakMinutes", breakMs / 60_000.0)
            summary.put("beforeOfflineIncludesBreaks", breakMs > 0)
            summary.put("onlineWindowGps", gpsJson(OutingMileage.gps(points, shift.startedAtEpochMs, offlineAt)))
            if (arrival != null) {
                summary.put("offlineMinutes", (arrival - offlineAt) / 60_000.0)
                summary.put("offlineGps", gpsJson(OutingMileage.gps(points, offlineAt, arrival)))
            }
        }
        return summary
    }

    private fun gpsJson(value: OutingMileage.GpsEstimate) = JSONObject()
        .put("source", "GPS_ESTIMATE")
        .put("miles", value.miles ?: JSONObject.NULL)
        .put("links", value.links)
        .put("rejectedLinks", value.rejectedLinks)
        .put("gapsOver60Seconds", value.longGaps)
        .put("uncoveredStartMs", value.uncoveredStartMs)
        .put("uncoveredEndMs", value.uncoveredEndMs)
}
