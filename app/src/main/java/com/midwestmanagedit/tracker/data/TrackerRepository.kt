package com.midwestmanagedit.tracker.data

import androidx.room.withTransaction
import com.midwestmanagedit.tracker.domain.AcquisitionMode
import com.midwestmanagedit.tracker.domain.EventType
import com.midwestmanagedit.tracker.domain.GeoStamp
import com.midwestmanagedit.tracker.domain.Platform
import com.midwestmanagedit.tracker.domain.QueueMode
import com.midwestmanagedit.tracker.domain.RideState
import com.midwestmanagedit.tracker.domain.ShiftState
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class TrackerRepository(private val database: TrackerDatabase) {
    val dao = database.trackerDao()

    fun observeActiveShift(): Flow<ShiftEntity?> = dao.observeActiveShift()
    fun observeLatestCompletedShift(): Flow<ShiftEntity?> = dao.observeLatestCompletedShift()
    fun observePendingRides(shiftId: String): Flow<List<RideEntity>> = dao.observePendingRides(shiftId)
    fun observeCompletedShifts(limit: Int): Flow<List<ShiftEntity>> = dao.observeCompletedShifts(limit)

    suspend fun saveClosingNote(shiftId: String, note: String) = database.withTransaction {
        val shift = dao.shift(shiftId) ?: error("Outing not found.")
        dao.updateShift(shift.copy(closingNote = note.trim().ifBlank { null }))
    }

    // ------------------------------------------------------------
    // NORMAL GIG SHIFT WORKFLOW
    // ------------------------------------------------------------

    suspend fun startShift(
        platform: Platform,
        queueMode: QueueMode,
        startOdometer: Double,
        stamp: GeoStamp,
        startWithAcceptedRide: Boolean,
    ): String = database.withTransaction {
        check(platform != Platform.FIELD_NATION) { "Use the FieldNation start workflow." }
        check(dao.activeShift() == null) { "Finish the existing shift first." }

        val shiftId = UUID.randomUUID().toString()
        val rideId = if (startWithAcceptedRide) UUID.randomUUID().toString() else null

        dao.insertShift(
            ShiftEntity(
                id = shiftId,
                platform = platform.name,
                state = if (rideId == null) ShiftState.AVAILABLE.name else ShiftState.EN_ROUTE_PICKUP.name,
                queueMode = queueMode.name,
                startedAtEpochMs = stamp.occurredAtEpochMs,
                startOdometer = startOdometer,
                activeRideId = rideId,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )

        event(shiftId, null, EventType.SHIFT_STARTED, stamp)

        if (rideId != null) {
            dao.insertRide(
                RideEntity(
                    id = rideId,
                    shiftId = shiftId,
                    sequence = 1,
                    acquisitionMode = if (queueMode == QueueMode.AUTO)
                        AcquisitionMode.AUTO_QUEUE.name
                    else
                        AcquisitionMode.MANUAL_ACCEPT.name,
                    state = RideState.EN_ROUTE_PICKUP.name,
                    queuedAtEpochMs = stamp.occurredAtEpochMs,
                    trackingStartedAtEpochMs = stamp.occurredAtEpochMs,
                ),
            )
        }

        shiftId
    }

    suspend fun startOldestPending(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.AVAILABLE)
        val ride = dao.pendingRides(shift.id).firstOrNull() ?: error("No pending ride.")

        dao.updateRide(
            ride.copy(
                state = RideState.EN_ROUTE_PICKUP.name,
                trackingStartedAtEpochMs = stamp.occurredAtEpochMs,
            ),
        )

        dao.updateShift(shift.copy(state = ShiftState.EN_ROUTE_PICKUP.name, activeRideId = ride.id))
        event(shift.id, ride.id, EventType.NEXT_PICKUP_STARTED, stamp)
    }

    /** Records a newly accepted ride as the active pickup. */
    suspend fun acceptRide(acquisitionMode: AcquisitionMode, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.AVAILABLE)
        val ride = RideEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shift.id,
            sequence = dao.nextSequence(shift.id),
            acquisitionMode = acquisitionMode.name,
            state = RideState.EN_ROUTE_PICKUP.name,
            queuedAtEpochMs = stamp.occurredAtEpochMs,
            trackingStartedAtEpochMs = stamp.occurredAtEpochMs,
        )

        dao.insertRide(ride)
        dao.updateShift(
            shift.copy(
                state = ShiftState.EN_ROUTE_PICKUP.name,
                activeRideId = ride.id,
            ),
        )
        event(
            shift.id,
            ride.id,
            if (acquisitionMode == AcquisitionMode.AUTO_QUEUE) EventType.RIDE_AUTO_QUEUED else EventType.RIDE_ACCEPTED,
            stamp,
        )
    }

    /** Records an offer that was queued while another ride is active. */
    suspend fun queueRide(acquisitionMode: AcquisitionMode, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        check(shift.state in setOf(ShiftState.EN_ROUTE_PICKUP.name, ShiftState.PASSENGER.name)) {
            "Queue another ride only while a ride is active."
        }

        val ride = RideEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shift.id,
            sequence = dao.nextSequence(shift.id),
            acquisitionMode = acquisitionMode.name,
            state = RideState.PENDING.name,
            queuedAtEpochMs = stamp.occurredAtEpochMs,
        )

        dao.insertRide(ride)
        event(
            shift.id,
            ride.id,
            if (acquisitionMode == AcquisitionMode.AUTO_QUEUE) EventType.RIDE_AUTO_QUEUED else EventType.RIDE_ACCEPTED,
            stamp,
        )
    }

    suspend fun dropoffPassenger(startNext: Boolean, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.PASSENGER)
        val ride = requireRide(shift.activeRideId)

        dao.updateRide(
            ride.copy(
                state = RideState.COMPLETED.name,
                dropoffAtEpochMs = stamp.occurredAtEpochMs,
            ),
        )

        event(shift.id, ride.id, EventType.PASSENGER_DROPPED_OFF, stamp)

        val next = if (startNext) dao.pendingRides(shift.id).firstOrNull() else null

        if (next == null) {
            dao.updateShift(shift.copy(state = ShiftState.AVAILABLE.name, activeRideId = null))
        } else {
            dao.updateRide(
                next.copy(
                    state = RideState.EN_ROUTE_PICKUP.name,
                    trackingStartedAtEpochMs = stamp.occurredAtEpochMs,
                ),
            )
            dao.updateShift(shift.copy(state = ShiftState.EN_ROUTE_PICKUP.name, activeRideId = next.id))
            event(shift.id, next.id, EventType.NEXT_PICKUP_STARTED, stamp)
        }
    }

    suspend fun loseOldestPending(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        val ride = dao.pendingRides(shift.id).firstOrNull() ?: error("No pending ride.")
        dao.updateRide(ride.copy(state = RideState.LOST.name))
        event(shift.id, ride.id, EventType.QUEUE_DISAPPEARED, stamp)
    }

    /** Cancels the active pre-pickup ride and returns the shift to availability. */
    suspend fun cancelActiveRide(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.EN_ROUTE_PICKUP)
        val ride = requireRide(shift.activeRideId)

        dao.updateRide(ride.copy(state = RideState.CANCELLED.name))
        dao.updateShift(
            shift.copy(
                state = ShiftState.AVAILABLE.name,
                activeRideId = null,
            ),
        )
        event(shift.id, ride.id, EventType.RIDE_CANCELLED, stamp)
    }

    // ------------------------------------------------------------
    // FIELD NATION WORKFLOW (NO RIDES)
    // ------------------------------------------------------------

    suspend fun startFieldNation(
        workOrderNumber: String,
        roundTrip: Boolean,
        startOdometer: Double,
        stamp: GeoStamp,
    ) = database.withTransaction {
        check(dao.activeShift() == null) { "Finish the existing shift first." }

        val shiftId = UUID.randomUUID().toString()

        dao.insertShift(
            ShiftEntity(
                id = shiftId,
                platform = Platform.FIELD_NATION.name,
                state = ShiftState.EN_ROUTE_SITE.name,
                queueMode = QueueMode.MANUAL.name,
                startedAtEpochMs = stamp.occurredAtEpochMs,
                startOdometer = startOdometer,
                workOrderNumber = workOrderNumber,
                roundTripExpected = roundTrip,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )

        event(
            shiftId,
            null,
            EventType.SHIFT_STARTED,
            stamp,
            JSONObject()
                .put("workOrderNumber", workOrderNumber)
                .put("roundTripExpected", roundTrip)
                .toString(),
        )
        event(shiftId, null, EventType.FN_TRIP_STARTED, stamp)
    }

    suspend fun fieldArriveSite(stamp: GeoStamp) = database.withTransaction {
        val shift = requireFieldShift(ShiftState.EN_ROUTE_SITE)
        dao.updateShift(shift.copy(state = ShiftState.ON_SITE.name))
        event(shift.id, null, EventType.FN_ARRIVED_SITE, stamp)
    }

    suspend fun fieldStartWork(stamp: GeoStamp) = database.withTransaction {
        val shift = requireFieldShift(ShiftState.ON_SITE)
        dao.updateShift(shift.copy(state = ShiftState.WORKING.name))
        event(shift.id, null, EventType.FN_CHECKED_IN, stamp)
    }

    suspend fun fieldCompleteWork(stamp: GeoStamp) = database.withTransaction {
        val shift = requireFieldShift(ShiftState.WORKING)
        dao.updateShift(shift.copy(state = ShiftState.WRAP_UP.name))
        event(shift.id, null, EventType.FN_WORK_COMPLETED, stamp)
    }

    suspend fun fieldCheckOut(stamp: GeoStamp) = database.withTransaction {
        val shift = requireFieldShift(ShiftState.WRAP_UP)
        dao.updateShift(
            shift.copy(
                state = ShiftState.RETURNING_HOME.name,
                endedAtEpochMs = stamp.occurredAtEpochMs,
            ),
        )
        event(shift.id, null, EventType.FN_CHECKED_OUT, stamp)
        if (shift.roundTripExpected) {
            event(shift.id, null, EventType.FN_RETURN_STARTED, stamp)
        }
    }

    // ------------------------------------------------------------
    // BREAK / END SHIFT / ARRIVE HOME (shared)
    // ------------------------------------------------------------

    suspend fun setBreak(start: Boolean, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        if (start) {
            check(shift.state == ShiftState.AVAILABLE.name) { "Finish the active ride first." }
            dao.updateShift(shift.copy(state = ShiftState.BREAK.name))
            event(shift.id, null, EventType.BREAK_STARTED, stamp)
        } else {
            check(shift.state == ShiftState.BREAK.name) { "Shift is not on break." }
            dao.updateShift(shift.copy(state = ShiftState.AVAILABLE.name))
            event(shift.id, null, EventType.BREAK_ENDED, stamp)
        }
    }

    suspend fun endShift(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        check(shift.activeRideId == null) { "Finish the active ride first." }
        check(dao.pendingRides(shift.id).isEmpty()) { "Resolve pending rides first." }

        dao.updateShift(
            shift.copy(
                state = ShiftState.RETURNING_HOME.name,
                endedAtEpochMs = stamp.occurredAtEpochMs,
            ),
        )

        event(shift.id, null, EventType.SHIFT_ENDED, stamp)
    }

    suspend fun arriveHome(shiftId: String, odometer: Double, stamp: GeoStamp) = database.withTransaction {
        val shift = dao.shift(shiftId) ?: error("Shift not found.")
        check(shift.state == ShiftState.RETURNING_HOME.name)
        check(odometer >= shift.startOdometer) { "Ending odometer cannot precede starting odometer." }

        dao.updateShift(
            shift.copy(
                state = ShiftState.COMPLETE.name,
                homeArrivedAtEpochMs = stamp.occurredAtEpochMs,
                endOdometer = odometer,
                completedAtEpochMs = stamp.occurredAtEpochMs,
            ),
        )

        val completionEvent = if (shift.platform == Platform.FIELD_NATION.name) {
            if (shift.roundTripExpected) EventType.FN_RETURN_COMPLETED else EventType.OUTING_COMPLETED
        } else {
            EventType.HOME_ARRIVED
        }
        event(shift.id, null, completionEvent, stamp)
    }

    // ------------------------------------------------------------
    // EXPORTER SUPPORT
    // ------------------------------------------------------------

    suspend fun shiftWithRides(id: String) = dao.shiftWithRides(id)
    suspend fun events(id: String) = dao.events(id)
    suspend fun locations(id: String) = dao.locations(id)

    // ------------------------------------------------------------
    // INTERNAL HELPERS
    // ------------------------------------------------------------

    suspend fun requireShift(required: ShiftState? = null): ShiftEntity {
        val shift = dao.activeShift() ?: error("No active shift.")
        if (required != null) check(shift.state == required.name) { "Expected ${required.name}; found ${shift.state}." }
        return shift
    }

    suspend fun requireRide(id: String?): RideEntity =
        id?.let { dao.ride(it) } ?: error("Active ride not found.")

    suspend fun requireFieldShift(required: ShiftState): ShiftEntity {
        val shift = requireShift(required)
        check(shift.platform == Platform.FIELD_NATION.name) { "This action requires a FieldNation outing." }
        return shift
    }

    suspend fun pickupPassenger(stamp: GeoStamp) = database.withTransaction {
    val shift = requireShift(ShiftState.EN_ROUTE_PICKUP)
    val ride = requireRide(shift.activeRideId)

    dao.updateRide(
        ride.copy(
            state = RideState.PASSENGER.name,
            pickupAtEpochMs = stamp.occurredAtEpochMs,
        ),
    )

    dao.updateShift(shift.copy(state = ShiftState.PASSENGER.name))
    event(shift.id, ride.id, EventType.PASSENGER_PICKED_UP, stamp)
}

    private suspend fun event(
        shiftId: String,
        rideId: String?,
        type: EventType,
        stamp: GeoStamp,
        payload: String = "{}",
    ) {
        dao.insertEvent(
            TrackingEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = shiftId,
                rideId = rideId,
                type = type.name,
                occurredAtEpochMs = stamp.occurredAtEpochMs,
                latitude = stamp.latitude,
                longitude = stamp.longitude,
                accuracyMeters = stamp.accuracyMeters,
                payloadJson = payload,
            ),
        )
    }
}
