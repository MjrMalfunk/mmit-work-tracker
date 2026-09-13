package com.midwestmanagedit.tracker.domain

class InvalidTransition(message: String) : IllegalStateException(message)

object TrackerReducer {
    fun reduce(state: TrackerState, command: TrackerCommand): TrackerState = when (command) {
        is TrackerCommand.StartShift -> {
            requireState(state, ShiftState.IDLE)
            if (command.firstRideId == null) {
                state.copy(shiftState = ShiftState.AVAILABLE)
            } else {
                state.copy(
                    shiftState = ShiftState.EN_ROUTE_PICKUP,
                    activeRideId = command.firstRideId,
                )
            }
        }

        is TrackerCommand.QueueRide -> {
            if (state.shiftState !in setOf(ShiftState.EN_ROUTE_PICKUP, ShiftState.PASSENGER)) {
                throw InvalidTransition("Queue a ride only while another ride is active.")
            }
            if (command.rideId == state.activeRideId || command.rideId in state.pendingRideIds) {
                throw InvalidTransition("Ride is already active or pending.")
            }
            state.copy(pendingRideIds = state.pendingRideIds + command.rideId)
        }

        is TrackerCommand.StartPendingRide -> {
            requireState(state, ShiftState.AVAILABLE)
            if (command.rideId !in state.pendingRideIds) {
                throw InvalidTransition("Pending ride does not exist.")
            }
            state.copy(
                shiftState = ShiftState.EN_ROUTE_PICKUP,
                activeRideId = command.rideId,
                pendingRideIds = state.pendingRideIds - command.rideId,
            )
        }

        TrackerCommand.Pickup -> {
            requireState(state, ShiftState.EN_ROUTE_PICKUP)
            state.copy(shiftState = ShiftState.PASSENGER)
        }

        is TrackerCommand.DropOff -> {
            requireState(state, ShiftState.PASSENGER)
            val next = command.startNextRideId
            if (next != null && next !in state.pendingRideIds) {
                throw InvalidTransition("Next ride is not pending.")
            }
            state.copy(
                shiftState = if (next == null) ShiftState.AVAILABLE else ShiftState.EN_ROUTE_PICKUP,
                activeRideId = next,
                pendingRideIds = if (next == null) state.pendingRideIds else state.pendingRideIds - next,
            )
        }

        is TrackerCommand.LosePendingRide -> {
            if (command.rideId !in state.pendingRideIds) {
                throw InvalidTransition("Pending ride does not exist.")
            }
            state.copy(pendingRideIds = state.pendingRideIds - command.rideId)
        }

        is TrackerCommand.ChangeQueueMode -> state.copy(queueMode = command.mode)

        TrackerCommand.StartBreak -> {
            requireState(state, ShiftState.AVAILABLE)
            state.copy(shiftState = ShiftState.BREAK)
        }

        TrackerCommand.Resume -> {
            requireState(state, ShiftState.BREAK)
            state.copy(shiftState = ShiftState.AVAILABLE)
        }

        TrackerCommand.EndShift -> {
            if (state.shiftState !in setOf(ShiftState.AVAILABLE, ShiftState.BREAK)) {
                throw InvalidTransition("Finish or cancel the active ride before ending the shift.")
            }
            state.copy(shiftState = ShiftState.RETURNING_HOME, activeRideId = null)
        }

        TrackerCommand.ArriveHome -> {
            requireState(state, ShiftState.RETURNING_HOME)
            state.copy(shiftState = ShiftState.COMPLETE)
        }

        TrackerCommand.StartFieldNation -> {
            requireState(state, ShiftState.IDLE)
            state.copy(shiftState = ShiftState.EN_ROUTE_SITE)
        }

        TrackerCommand.ArriveSite -> {
            requireState(state, ShiftState.EN_ROUTE_SITE)
            state.copy(shiftState = ShiftState.ON_SITE)
        }

        TrackerCommand.StartFieldWork -> {
            requireState(state, ShiftState.ON_SITE)
            state.copy(shiftState = ShiftState.WORKING)
        }

        TrackerCommand.CompleteFieldWork -> {
            requireState(state, ShiftState.WORKING)
            state.copy(shiftState = ShiftState.WRAP_UP)
        }

        TrackerCommand.CheckOutFieldWork -> {
            requireState(state, ShiftState.WRAP_UP)
            state.copy(shiftState = ShiftState.RETURNING_HOME)
        }
    }

    private fun requireState(state: TrackerState, required: ShiftState) {
        if (state.shiftState != required) {
            throw InvalidTransition("Expected $required, found ${state.shiftState}.")
        }
    }
}
