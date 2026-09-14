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
    val dao = database.trackerDao()

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

    // ------------------------------------------------------------
    // REQUIRED BY ShiftExporter.kt
    // ------------------------------------------------------------

    suspend fun shiftWithRides(shiftId: String): ShiftWithRides? =
        dao.shiftWithRides(shiftId)

    suspend fun events(shiftId: String): List<TrackingEventEntity> =
        dao.events(shiftId)

    suspend fun locations(shiftId: String): List<LocationPointEntity> =
        dao.locations(shiftId)

    // ------------------------------------------------------------
    // INTERNAL HELPERS
    // ------------------------------------------------------------

    suspend fun requireShift(required: ShiftState? = null): ShiftEntity {
        val shift = dao.activeShift() ?: error("No active shift.")
        if (required != null) check(shift.state == required.name) { "Shift must be $required." }
        return shift
    }

    suspend fun requireRide(id: String?): RideEntity =
        id?.let { dao.ride(it) } ?: error("Active ride not found.")

    suspend fun requireFieldShift(required: ShiftState): ShiftEntity {
        val shift = requireShift(required)
        check(shift.platform == Platform.FIELD_NATION.name) { "This action requires a FieldNation outing." }
        return shift
    }

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    suspend fun event(
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
