package com.midwestmanagedit.tracker.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Scheduled compensation stays separate from actual station/work/home timestamps. */
data class FlexBlock(val station: String, val startEpochMs: Long, val endEpochMs: Long, val payCents: Long) {
    init {
        require(station.length <= 100 && !station.contains('\u0000')) { "Station name is too long or invalid." }
        require(startEpochMs > 0 && endEpochMs > startEpochMs && endEpochMs - startEpochMs <= 12 * 3_600_000L) { "Block duration must be greater than zero and at most 12 hours." }
        require(payCents in 0..1_000_000L) { "Enter a valid block pay, up to $10,000." }
    }
    val scheduledMinutes: Double get() = (endEpochMs - startEpochMs) / 60_000.0

    companion object {
        fun parse(date: String, time: String, hours: String, pay: String, station: String, zone: ZoneId = ZoneId.systemDefault()): FlexBlock {
            require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Use YYYY-MM-DD for the block date." }
            require(time.matches(Regex("\\d{2}:\\d{2}"))) { "Use HH:mm (24-hour time) for block start." }
            val local = LocalDate.parse(date).atTime(LocalTime.parse(time))
            require(zone.rules.getValidOffsets(local).size == 1) { "This clock time is ambiguous or unavailable; choose another start time." }
            val start = local.atZone(zone).toInstant().toEpochMilli()
            val duration = try { BigDecimal(hours.trim()).multiply(BigDecimal(3_600_000)).longValueExact() }
                catch (_: Exception) { throw IllegalArgumentException("Enter a valid block duration in hours.") }
            val cents = try { BigDecimal(pay.trim()).movePointRight(2).longValueExact() }
                catch (_: Exception) { throw IllegalArgumentException("Enter block pay with no more than two decimal places.") }
            require(duration in 1..12 * 3_600_000L) { "Block duration must be greater than zero and at most 12 hours." }
            return FlexBlock(station.trim(), start, start + duration, cents)
        }
        fun hourly(payCents: Long, durationMs: Long?): Double? = durationMs?.takeIf { it > 0 }?.let { payCents / 100.0 * 3_600_000 / it }
    }
}

enum class FlexAction { ARRIVE_STATION, START_DELIVERIES, FINISH_DELIVERIES, RETURN_PACKAGES, FINISH_RETURN, RELEASE_NO_ROUTE }

object FlexWorkflow {
    data class Transition(val state: ShiftState, val event: EventType, val workFinished: Boolean = false)
    fun transition(state: ShiftState, action: FlexAction): Transition = when (action) {
        FlexAction.ARRIVE_STATION -> { require(state == ShiftState.EN_ROUTE_STATION); Transition(ShiftState.AT_STATION, EventType.FLEX_ARRIVED_STATION) }
        FlexAction.START_DELIVERIES -> { require(state == ShiftState.AT_STATION); Transition(ShiftState.DELIVERING, EventType.FLEX_DELIVERIES_STARTED) }
        FlexAction.FINISH_DELIVERIES -> { require(state == ShiftState.DELIVERING); Transition(ShiftState.RETURNING_HOME, EventType.FLEX_DELIVERIES_FINISHED, true) }
        FlexAction.RETURN_PACKAGES -> { require(state == ShiftState.DELIVERING); Transition(ShiftState.RETURNING_PACKAGES, EventType.FLEX_PACKAGE_RETURN_STARTED) }
        FlexAction.FINISH_RETURN -> { require(state == ShiftState.RETURNING_PACKAGES); Transition(ShiftState.RETURNING_HOME, EventType.FLEX_PACKAGE_RETURN_COMPLETED, true) }
        FlexAction.RELEASE_NO_ROUTE -> { require(state == ShiftState.AT_STATION); Transition(ShiftState.RETURNING_HOME, EventType.FLEX_RELEASED_NO_ROUTE, true) }
    }
}
