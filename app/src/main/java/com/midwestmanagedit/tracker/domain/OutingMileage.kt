package com.midwestmanagedit.tracker.domain

import kotlin.math.*

/** Odometer readings describe one outing. Segment estimates are never added to its total. */
object OutingMileage {
    data class Split(val total: Double?, val online: Double?, val offline: Double?)
    data class Point(val time: Long, val latitude: Double, val longitude: Double, val accuracy: Float)
    data class GpsEstimate(
        val miles: Double?, val links: Int, val rejectedLinks: Int,
        val longGaps: Int, val uncoveredStartMs: Long, val uncoveredEndMs: Long,
    )

    fun split(start: Double, offline: Double?, end: Double?): Split {
        require(start.isFinite() && start in 0.0..9_999_999.0) { "Invalid starting odometer." }
        require(offline == null || (offline.isFinite() && offline in start..9_999_999.0)) {
            "Offline odometer cannot precede the starting reading."
        }
        require(end == null || (end.isFinite() && end in (offline ?: start)..9_999_999.0)) {
            "Final odometer cannot precede the offline or starting reading."
        }
        return Split(end?.minus(start), offline?.minus(start),
            if (end != null && offline != null) end - offline else null)
    }

    fun correctedArrival(offlineAt: Long, tappedAt: Long, minutesLate: Double): Long {
        require(minutesLate.isFinite() && minutesLate >= 0 && minutesLate <= (tappedAt - offlineAt) / 60_000.0) {
            "Arrival must be between going offline and the arrival tap."
        }
        return tappedAt - (minutesLate * 60_000).roundToLong()
    }

    /** Integrate each GPS link once, clipped at time boundaries. Crossing links are estimates.
     * Keep gaps visible; neither silence nor a stationary fix proves arrival at home.
     */
    fun gps(points: List<Point>, from: Long, to: Long): GpsEstimate {
        require(to >= from)
        if (to == from) return GpsEstimate(0.0, 0, 0, 0, 0, 0)
        val sorted = points.sortedBy { it.time }.distinctBy { it.time }
        var miles = 0.0
        var links = 0
        var rejected = 0
        var gaps = 0
        var first: Long? = null
        var last: Long? = null
        fun valid(p: Point) = p.latitude.isFinite() && p.longitude.isFinite() &&
            p.latitude in -90.0..90.0 && p.longitude in -180.0..180.0 &&
            p.accuracy.isFinite() && p.accuracy in 0f..50f
        for ((a, b) in sorted.zipWithNext()) {
            val begin = maxOf(from, a.time)
            val finish = minOf(to, b.time)
            if (finish <= begin) continue
            val elapsed = b.time - a.time
            if (elapsed > 60_000) gaps++
            if (!valid(a) || !valid(b)) { rejected++; continue }
            val radians = Math.PI / 180
            val h = (sin((b.latitude - a.latitude) * radians / 2).pow(2) +
                cos(a.latitude * radians) * cos(b.latitude * radians) *
                sin((b.longitude - a.longitude) * radians / 2).pow(2)).coerceIn(0.0, 1.0)
            val meters = 6_371_008.8 * 2 * atan2(sqrt(h), sqrt(1 - h))
            if (meters / (elapsed / 1000.0) > 70) { rejected++; continue }
            miles += meters / 1609.344 * (finish - begin).toDouble() / elapsed
            links++
            if (first == null) first = begin
            last = finish
        }
        return GpsEstimate(if (links > 0) miles else null, links, rejected, gaps,
            first?.minus(from) ?: (to - from), to - (last ?: from))
    }
}
