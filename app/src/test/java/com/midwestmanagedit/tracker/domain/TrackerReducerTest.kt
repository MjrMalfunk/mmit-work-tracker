package com.midwestmanagedit.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackerReducerTest {
    @Test
    fun `queued ride stays pending until current passenger is dropped off`() {
        var state = TrackerReducer.reduce(TrackerState(), TrackerCommand.StartShift("ride-1"))
        state = TrackerReducer.reduce(state, TrackerCommand.Pickup)
        state = TrackerReducer.reduce(state, TrackerCommand.QueueRide("ride-2"))

        assertEquals("ride-1", state.activeRideId)
        assertEquals(listOf("ride-2"), state.pendingRideIds)

        state = TrackerReducer.reduce(state, TrackerCommand.DropOff("ride-2"))

        assertEquals(ShiftState.EN_ROUTE_PICKUP, state.shiftState)
        assertEquals("ride-2", state.activeRideId)
        assertEquals(emptyList<String>(), state.pendingRideIds)
    }

    @Test
    fun `disappeared auto queued ride returns to available without phantom ride`() {
        var state = TrackerState(
            shiftState = ShiftState.PASSENGER,
            queueMode = QueueMode.AUTO,
            activeRideId = "ride-1",
            pendingRideIds = listOf("ride-2"),
        )
        state = TrackerReducer.reduce(state, TrackerCommand.LosePendingRide("ride-2"))
        state = TrackerReducer.reduce(state, TrackerCommand.DropOff())

        assertEquals(ShiftState.AVAILABLE, state.shiftState)
        assertNull(state.activeRideId)
        assertEquals(emptyList<String>(), state.pendingRideIds)
    }

    @Test
    fun `queue mode can change during one shift`() {
        val manual = TrackerReducer.reduce(TrackerState(), TrackerCommand.StartShift())
        val auto = TrackerReducer.reduce(manual, TrackerCommand.ChangeQueueMode(QueueMode.AUTO))
        assertEquals(QueueMode.AUTO, auto.queueMode)
    }

    @Test
    fun `going offline keeps the outing open until home arrival`() {
        var state = TrackerReducer.reduce(TrackerState(), TrackerCommand.StartShift())
        state = TrackerReducer.reduce(state, TrackerCommand.EndShift)
        assertEquals(ShiftState.RETURNING_HOME, state.shiftState)

        state = TrackerReducer.reduce(state, TrackerCommand.ArriveHome)
        assertEquals(ShiftState.COMPLETE, state.shiftState)
    }

    @Test
    fun `field nation round trip follows travel work and return phases`() {
        var state = TrackerReducer.reduce(TrackerState(), TrackerCommand.StartFieldNation)
        assertEquals(ShiftState.EN_ROUTE_SITE, state.shiftState)
        state = TrackerReducer.reduce(state, TrackerCommand.ArriveSite)
        state = TrackerReducer.reduce(state, TrackerCommand.StartFieldWork)
        state = TrackerReducer.reduce(state, TrackerCommand.CompleteFieldWork)
        state = TrackerReducer.reduce(state, TrackerCommand.CheckOutFieldWork)
        assertEquals(ShiftState.RETURNING_HOME, state.shiftState)
        state = TrackerReducer.reduce(state, TrackerCommand.ArriveHome)
        assertEquals(ShiftState.COMPLETE, state.shiftState)
    }
}
