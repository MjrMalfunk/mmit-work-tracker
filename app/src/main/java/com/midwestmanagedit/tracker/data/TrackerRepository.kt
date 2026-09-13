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

class TrackerRepository(private val database: TrackerDatabase) {
    private val dao = database.trackerDao()

    fun observeActiveShift(): Flow<ShiftEntity?> = dao.observeActiveShift()
    fun observeLatestCompletedShift(): Flow<ShiftEntity?> = dao.observeLatestCompletedShift()
    fun observePendingRides(shiftId: String): Flow<List<RideEntity>> = dao.observePendingRides(shiftId)

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
                    acquisitionMode = if (queueMode == QueueMode.AUTO) {
                        AcquisitionMode.AUTO_QUEUE.name
                    } else {
                        AcquisitionMode.MANUAL_ACCEPT.name
                    },
                    state = RideState.EN_ROUTE_PICKUP.name,
                    queuedAtEpochMs = stamp.occurredAtEpochMs,
                    trackingStartedAtEpochMs = stamp.occurredAtEpochMs,
                ),
            )
            event(shiftId, rideId, EventType.RIDE_ACCEPTED, stamp)
            event(shiftId, rideId, EventType.NEXT_PICKUP_STARTED, stamp)
        }
        shiftId
    }

    suspend fun startFieldNation(
        workOrderNumber: String,
        roundTripExpected: Boolean,
        startOdometer: Double,
        stamp: GeoStamp,
    ): String = database.withTransaction {
        check(dao.activeShift() == null) { "Finish the existing outing first." }
        val cleanNumber = workOrderNumber.trim()
        check(cleanNumber.isNotEmpty()) { "Enter the FieldNation work-order number." }
        val shiftId = UUID.randomUUID().toString()
        dao.insertShift(
            ShiftEntity(
                id = shiftId,
                platform = Platform.FIELD_NATION.name,
                state = ShiftState.EN_ROUTE_SITE.name,
                queueMode = QueueMode.MANUAL.name,
                startedAtEpochMs = stamp.occurredAtEpochMs,
                startOdometer = startOdometer,
                workOrderNumber = cleanNumber,
                roundTripExpected = roundTripExpected,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        event(
            shiftId,
            null,
            EventType.SHIFT_STARTED,
            stamp,
            "{\"workOrderNumber\":\"${jsonEscape(cleanNumber)}\",\"roundTripExpected\":$roundTripExpected}",
        )
        event(shiftId, null, EventType.FN_TRIP_STARTED, stamp)
        shiftId
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

    suspend fun changeQueueMode(mode: QueueMode, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        dao.updateShift(shift.copy(queueMode = mode.name))
        event(shift.id, shift.activeRideId, EventType.QUEUE_MODE_CHANGED, stamp, "{\"mode\":\"${mode.name}\"}")
    }

    suspend fun acceptOrQueueRide(mode: AcquisitionMode, stamp: GeoStamp): String = database.withTransaction {
        val shift = requireShift()
        check(shift.state !in setOf(ShiftState.BREAK.name, ShiftState.RETURNING_HOME.name, ShiftState.COMPLETE.name))
        val rideId = UUID.randomUUID().toString()
        val activatesNow = shift.activeRideId == null && shift.state == ShiftState.AVAILABLE.name
        if (!activatesNow) {
            check(dao.pendingRides(shift.id).isEmpty()) { "Resolve the queued ride first." }
        }
        dao.insertRide(
            RideEntity(
                id = rideId,
                shiftId = shift.id,
                sequence = dao.nextSequence(shift.id),
                acquisitionMode = mode.name,
                state = if (activatesNow) RideState.EN_ROUTE_PICKUP.name else RideState.PENDING.name,
                queuedAtEpochMs = stamp.occurredAtEpochMs,
                trackingStartedAtEpochMs = stamp.occurredAtEpochMs.takeIf { activatesNow },
            ),
        )
        event(
            shift.id,
            rideId,
            if (mode == AcquisitionMode.AUTO_QUEUE) EventType.RIDE_AUTO_QUEUED else EventType.RIDE_ACCEPTED,
            stamp,
        )
        if (activatesNow) {
            dao.updateShift(shift.copy(state = ShiftState.EN_ROUTE_PICKUP.name, activeRideId = rideId))
            event(shift.id, rideId, EventType.NEXT_PICKUP_STARTED, stamp)
        }
        rideId
    }

    suspend fun pickup(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.EN_ROUTE_PICKUP)
        val ride = requireRide(shift.activeRideId)
        dao.updateRide(ride.copy(state = RideState.PASSENGER.name, pickupAtEpochMs = stamp.occurredAtEpochMs))
        dao.updateShift(shift.copy(state = ShiftState.PASSENGER.name))
        event(shift.id, ride.id, EventType.PASSENGER_PICKED_UP, stamp)
    }

    suspend fun dropOff(startNext: Boolean, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.PASSENGER)
        val ride = requireRide(shift.activeRideId)
        dao.updateRide(ride.copy(state = RideState.COMPLETED.name, dropoffAtEpochMs = stamp.occurredAtEpochMs))
        event(shift.id, ride.id, EventType.PASSENGER_DROPPED_OFF, stamp)

        val next = if (startNext) dao.pendingRides(shift.id).firstOrNull() else null
        if (next == null) {
            dao.updateShift(shift.copy(state = ShiftState.AVAILABLE.name, activeRideId = null))
        } else {
            dao.updateRide(next.copy(state = RideState.EN_ROUTE_PICKUP.name, trackingStartedAtEpochMs = stamp.occurredAtEpochMs))
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

class TrackerRepository(private val database: TrackerDatabase) {
    private val dao = database.trackerDao()

    fun observeActiveShift(): Flow<ShiftEntity?> = dao.observeActiveShift()
    fun observeLatestCompletedShift(): Flow<ShiftEntity?> = dao.observeLatestCompletedShift()
    fun observePendingRides(shiftId: String): Flow<List<RideEntity>> = dao.observePendingRides(shiftId)

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
                    acquisitionMode = if (queueMode == QueueMode.AUTO) {
                        AcquisitionMode.AUTO_QUEUE.name
                    } else {
                        AcquisitionMode.MANUAL_ACCEPT.name
                    },
                    state = RideState.EN_ROUTE_PICKUP.name,
                    queuedAtEpochMs = stamp.occurredAtEpochMs,
                    trackingStartedAtEpochMs = stamp.occurredAtEpochMs,
                ),
            )
            event(shiftId, rideId, EventType.RIDE_ACCEPTED, stamp)
            event(shiftId, rideId, EventType.NEXT_PICKUP_STARTED, stamp)
        }
        shiftId
    }

    suspend fun startFieldNation(
        workOrderNumber: String,
        roundTripExpected: Boolean,
        startOdometer: Double,
        stamp: GeoStamp,
    ): String = database.withTransaction {
        check(dao.activeShift() == null) { "Finish the existing outing first." }
        val cleanNumber = workOrderNumber.trim()
        check(cleanNumber.isNotEmpty()) { "Enter the FieldNation work-order number." }
        val shiftId = UUID.randomUUID().toString()
        dao.insertShift(
            ShiftEntity(
                id = shiftId,
                platform = Platform.FIELD_NATION.name,
                state = ShiftState.EN_ROUTE_SITE.name,
                queueMode = QueueMode.MANUAL.name,
                startedAtEpochMs = stamp.occurredAtEpochMs,
                startOdometer = startOdometer,
                workOrderNumber = cleanNumber,
                roundTripExpected = roundTripExpected,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        event(
            shiftId,
            null,
            EventType.SHIFT_STARTED,
            stamp,
            "{\"workOrderNumber\":\"${jsonEscape(cleanNumber)}\",\"roundTripExpected\":$roundTripExpected}",
        )
        event(shiftId, null, EventType.FN_TRIP_STARTED, stamp)
        shiftId
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

    suspend fun changeQueueMode(mode: QueueMode, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        dao.updateShift(shift.copy(queueMode = mode.name))
        event(shift.id, shift.activeRideId, EventType.QUEUE_MODE_CHANGED, stamp, "{\"mode\":\"${mode.name}\"}")
    }

    suspend fun acceptOrQueueRide(mode: AcquisitionMode, stamp: GeoStamp): String = database.withTransaction {
        val shift = requireShift()
        check(shift.state !in setOf(ShiftState.BREAK.name, ShiftState.RETURNING_HOME.name, ShiftState.COMPLETE.name))
        val rideId = UUID.randomUUID().toString()
        val activatesNow = shift.activeRideId == null && shift.state == ShiftState.AVAILABLE.name
        if (!activatesNow) {
            check(dao.pendingRides(shift.id).isEmpty()) { "Resolve the queued ride first." }
        }
        dao.insertRide(
            RideEntity(
                id = rideId,
                shiftId = shift.id,
                sequence = dao.nextSequence(shift.id),
                acquisitionMode = mode.name,
                state = if (activatesNow) RideState.EN_ROUTE_PICKUP.name else RideState.PENDING.name,
                queuedAtEpochMs = stamp.occurredAtEpochMs,
                trackingStartedAtEpochMs = stamp.occurredAtEpochMs.takeIf { activatesNow },
            ),
        )
        event(
            shift.id,
            rideId,
            if (mode == AcquisitionMode.AUTO_QUEUE) EventType.RIDE_AUTO_QUEUED else EventType.RIDE_ACCEPTED,
            stamp,
        )
        if (activatesNow) {
            dao.updateShift(shift.copy(state = ShiftState.EN_ROUTE_PICKUP.name, activeRideId = rideId))
            event(shift.id, rideId, EventType.NEXT_PICKUP_STARTED, stamp)
        }
        rideId
    }

    suspend fun pickup(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.EN_ROUTE_PICKUP)
        val ride = requireRide(shift.activeRideId)
        dao.updateRide(ride.copy(state = RideState.PASSENGER.name, pickupAtEpochMs = stamp.occurredAtEpochMs))
        dao.updateShift(shift.copy(state = ShiftState.PASSENGER.name))
        event(shift.id, ride.id, EventType.PASSENGER_PICKED_UP, stamp)
    }

    suspend fun dropOff(startNext: Boolean, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift(ShiftState.PASSENGER)
        val ride = requireRide(shift.activeRideId)
        dao.updateRide(ride.copy(state = RideState.COMPLETED.name, dropoffAtEpochMs = stamp.occurredAtEpochMs))
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

    // NEW: cancel the active ride and return to AVAILABLE
    suspend fun cancelActiveRide(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        val ride = requireRide(shift.activeRideId)
        // Only allow cancel when en route to pickup
        check(ride.state == RideState.EN_ROUTE_PICKUP.name) { "Only en-route pickups can be canceled." }

        dao.updateRide(
            ride.copy(
                state = RideState.LOST.name,
                dropoffAtEpochMs = stamp.occurredAtEpochMs,
            ),
        )
        event(
            shift.id,
            ride.id,
            EventType.QUEUE_DISAPPEARED,
            stamp,
            """{"canceled":true}""",
        )

        dao.updateShift(
            shift.copy(
                state = ShiftState.AVAILABLE.name,
                activeRideId = null,
            ),
        )
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

    suspend fun setBreak(start: Boolean, stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        if (start) check(shift.state == ShiftState.AVAILABLE.name) { "Finish the active ride first." }
        else check(shift.state == ShiftState.BREAK.name) { "Shift is not on break." }
        dao.updateShift(shift.copy(state = if (start) ShiftState.BREAK.name else ShiftState.AVAILABLE.name))
        event(shift.id, null, if (start) EventType.BREAK_STARTED else EventType.BREAK_ENDED, stamp)
    }

    suspend fun endShift(stamp: GeoStamp) = database.withTransaction {
        val shift = requireShift()
        check(shift.activeRideId == null) { "Finish the active ride first." }
        check(dao.pendingRides(shift.id).isEmpty()) { "Resolve pending rides first." }
        dao.updateShift(shift.copy(state = ShiftState.RETURNING_HOME.name, endedAtEpochMs = stamp.occurredAtEpochMs))
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
        when {
            shift.platform != Platform.FIELD_NATION.name -> event(shift.id, null, EventType.HOME_ARRIVED, stamp)
            shift.roundTripExpected -> event(shift.id, null, EventType.FN_RETURN_COMPLETED, stamp)
            else -> event(shift.id, null, EventType.OUTING_COMPLETED, stamp)
        }
    }

    suspend fun activeSnapshot(): ActiveSnapshot? = dao.activeShift()?.let {
        ActiveSnapshot(it.id, it.state, it.queueMode, it.activeRideId)
    }

    suspend fun recordLocation(point: LocationPointEntity) = dao.insertLocation(point)
    suspend fun shiftWithRides(id: String) = dao.shiftWithRides(id)
    suspend fun events(id: String) = dao.events(id)
    suspend fun locations(id: String) = dao.locations(id)
    suspend fun latestCompletedShift() = dao.latestCompletedShift()

    private suspend fun requireShift(required: ShiftState? = null): ShiftEntity {
        val shift = dao.activeShift() ?: error("No active shift.")
        if (required != null) check(shift.state == required.name) { "Expected ${required.name}; found ${shift.state}." }
        return shift
    }

    private suspend fun requireRide(id: String?): RideEntity =
        id?.let { dao.ride(it) } ?: error("Active ride not found.")

    private suspend fun requireFieldShift(required: ShiftState): ShiftEntity {
        val shift = requireShift(required)
        check(shift.platform == Platform.FIELD_NATION.name) { "This action requires a FieldNation outing." }
        return shift
    }

    private fun jsonEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

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
