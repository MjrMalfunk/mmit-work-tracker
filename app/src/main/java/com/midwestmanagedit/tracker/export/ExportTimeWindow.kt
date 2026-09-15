package com.midwestmanagedit.tracker.export

/**
 * Keeps exported GPS evidence inside the recorded outing boundaries.
 * A null completion is intentionally not bounded at the end; incomplete
 * outings are rejected by OPS and are not exportable from the app UI.
 */
object ExportTimeWindow {
    fun contains(timestamp: Long, startedAt: Long, completedAt: Long?): Boolean =
        timestamp >= startedAt && (completedAt == null || timestamp <= completedAt)
}
