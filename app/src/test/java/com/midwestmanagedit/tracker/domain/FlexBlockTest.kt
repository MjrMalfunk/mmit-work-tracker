package com.midwestmanagedit.tracker.domain

import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class FlexBlockTest {
    private val zone = ZoneId.of("America/New_York")

    @Test fun firstBlockKeepsScheduledPayAndFinish() {
        val block = FlexBlock.parse("2026-10-13", "04:15", "3.5", "79.00", " Station ", zone)
        assertEquals("Station", block.station)
        assertEquals(7900L, block.payCents)
        assertEquals(210.0, block.scheduledMinutes, 0.0)
        assertEquals(22.571428, FlexBlock.hourly(block.payCents, block.endEpochMs - block.startEpochMs)!!, 0.00001)
        val finish = java.time.Instant.ofEpochMilli(block.endEpochMs).atZone(zone)
        assertEquals("07:45", finish.toLocalTime().toString())
    }

    @Test fun earlyActualFinishDoesNotRewriteScheduledBlock() {
        val block = FlexBlock.parse("2026-10-13", "04:15", "3.5", "79", "", zone)
        assertEquals(45.142857, FlexBlock.hourly(block.payCents, 105 * 60_000L)!!, 0.00001)
        assertEquals(210.0, block.scheduledMinutes, 0.0)
        assertNull(FlexBlock.hourly(7900, 0))
        assertNull(FlexBlock.hourly(7900, null))
    }

    @Test fun invalidScheduleAndMoneyCannotStartAnOuting() {
        for ((hours, pay) in listOf("0" to "79", "-1" to "79", "13" to "79", "NaN" to "79", "3.5" to "-1", "3.5" to "79.001")) {
            assertTrue(runCatching { FlexBlock.parse("2026-10-13", "04:15", hours, pay, "", zone) }.isFailure)
        }
        assertTrue(runCatching { FlexBlock.parse("2026-02-30", "04:15", "3.5", "79", "", zone) }.isFailure)
        assertTrue(runCatching { FlexBlock.parse("2026-10-13", "25:15", "3.5", "79", "", zone) }.isFailure)
        assertTrue(runCatching { FlexBlock.parse("2026-11-01", "01:30", "3.5", "79", "", zone) }.isFailure)
    }

    @Test fun midnightBlockKeepsNextDayFinish() {
        val block = FlexBlock.parse("2026-10-13", "23:30", "3", "79", "", zone)
        assertEquals("2026-10-14T02:30", java.time.Instant.ofEpochMilli(block.endEpochMs).atZone(zone).toLocalDateTime().toString())
    }

    @Test fun returnsRemainWorkUntilStationReturnFinishes() {
        val loading = FlexWorkflow.transition(ShiftState.EN_ROUTE_STATION, FlexAction.ARRIVE_STATION)
        val route = FlexWorkflow.transition(loading.state, FlexAction.START_DELIVERIES)
        val returning = FlexWorkflow.transition(route.state, FlexAction.RETURN_PACKAGES)
        assertEquals(ShiftState.RETURNING_PACKAGES, returning.state)
        assertFalse(returning.workFinished)
        val done = FlexWorkflow.transition(returning.state, FlexAction.FINISH_RETURN)
        assertEquals(ShiftState.RETURNING_HOME, done.state)
        assertTrue(done.workFinished)
        assertEquals(EventType.FLEX_PACKAGE_RETURN_COMPLETED, done.event)
    }

    @Test fun noRouteAndNormalFinishAreDistinct() {
        assertEquals(EventType.FLEX_RELEASED_NO_ROUTE, FlexWorkflow.transition(ShiftState.AT_STATION, FlexAction.RELEASE_NO_ROUTE).event)
        assertEquals(EventType.FLEX_DELIVERIES_FINISHED, FlexWorkflow.transition(ShiftState.DELIVERING, FlexAction.FINISH_DELIVERIES).event)
        assertTrue(runCatching { FlexWorkflow.transition(ShiftState.PASSENGER, FlexAction.FINISH_DELIVERIES) }.isFailure)
        assertTrue(runCatching { FlexWorkflow.transition(ShiftState.RETURNING_HOME, FlexAction.FINISH_RETURN) }.isFailure)
        assertTrue(runCatching { FlexWorkflow.transition(ShiftState.DELIVERING, FlexAction.ARRIVE_STATION) }.isFailure)
    }

    @Test fun cancellationKeepsReturnTripAndCannotReplaceAnActiveDelivery() {
        for (state in listOf(ShiftState.EN_ROUTE_STATION, ShiftState.AT_STATION)) {
            val cancelled = FlexWorkflow.transition(state, FlexAction.CANCEL_BLOCK)
            assertEquals(ShiftState.RETURNING_HOME, cancelled.state)
            assertEquals(EventType.FLEX_BLOCK_CANCELLED, cancelled.event)
            assertTrue(cancelled.workFinished)
        }
        for (state in listOf(ShiftState.DELIVERING, ShiftState.RETURNING_PACKAGES, ShiftState.RETURNING_HOME, ShiftState.COMPLETE)) {
            assertTrue(runCatching { FlexWorkflow.transition(state, FlexAction.CANCEL_BLOCK) }.isFailure)
        }
    }

    @Test fun unpaidCancellationDoesNotInheritOfferedPayAndConfirmedAppealCanOverride() {
        assertEquals(7900L, FlexPay.amountCents(7900L, FlexOutcome.RELEASED_NO_ROUTE.name, null))
        assertEquals(0L, FlexPay.amountCents(7900L, FlexOutcome.CANCELLED.name, null))
        assertEquals(7900L, FlexPay.amountCents(7900L, FlexOutcome.CANCELLED.name, 7900L))
        assertEquals(0L, FlexPay.amountCents(7900L, FlexOutcome.RELEASED_NO_ROUTE.name, 0L))
        assertEquals(7900L, FlexPay.amountCents(7900L, FlexOutcome.COMPLETED.name, null))
    }

    @Test fun confirmedPayKeepsExactCentsAndRejectsInvalidAmounts() {
        assertNull(FlexPay.parseConfirmedPay(""))
        assertEquals(0L, FlexPay.parseConfirmedPay("0"))
        assertEquals(7901L, FlexPay.parseConfirmedPay("79.01"))
        for (value in listOf("-1", "79.001", "NaN", "10000.01")) assertTrue(runCatching { FlexPay.parseConfirmedPay(value) }.isFailure)
    }
}
