package com.midwestmanagedit.tracker.domain

enum class Platform { LYFT, UBER, FIELD_NATION }
enum class QueueMode { MANUAL, AUTO }

enum class ShiftState {
    IDLE,
    AVAILABLE,
    EN_ROUTE_PICKUP,
    PASSENGER,
    BREAK,
    EN_ROUTE_SITE,
    ON_SITE,
    WORKING,
    WRAP_UP,
    RETURNING_HOME,
    COMPLETE,
}

enum class RideState {
    PENDING,
    EN_ROUTE_PICKUP,
    PASSENGER,
    COMPLETED,
    LOST,
    CANCELLED,
}

enum class AcquisitionMode { MANUAL_ACCEPT, AUTO_QUEUE }

enum class EventType {
    SHIFT_STARTED,
    QUEUE_MODE_CHANGED,
    RIDE_ACCEPTED,
    RIDE_AUTO_QUEUED,
    NEXT_PICKUP_STARTED,
    PASSENGER_PICKED_UP,
    PASSENGER_DROPPED_OFF,
    QUEUE_DISAPPEARED,
    RIDE_CANCELLED,
    BREAK_STARTED,
    BREAK_ENDED,
    SHIFT_ENDED,
    HOME_ARRIVED,
    FN_TRIP_STARTED,
    FN_ARRIVED_SITE,
    FN_CHECKED_IN,
    FN_WORK_COMPLETED,
    FN_CHECKED_OUT,
    FN_RETURN_STARTED,
    FN_RETURN_COMPLETED,
    OUTING_COMPLETED,
}

data class GeoStamp(
    val occurredAtEpochMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
)

/**
 * Pure state used by unit tests and by UI decision logic.
 * A pending ride never changes active ride mileage until explicitly activated.
 */
data class TrackerState(
    val shiftState: ShiftState = ShiftState.IDLE,
    val queueMode: QueueMode = QueueMode.MANUAL,
    val activeRideId: String? = null,
    val pendingRideIds: List<String> = emptyList(),
)

sealed interface TrackerCommand {
    data class StartShift(val firstRideId: String? = null) : TrackerCommand
    data class QueueRide(val rideId: String) : TrackerCommand
    data class StartPendingRide(val rideId: String) : TrackerCommand
    data object Pickup : TrackerCommand
    data class DropOff(val startNextRideId: String? = null) : TrackerCommand
    data object CancelActiveRide : TrackerCommand
    data class LosePendingRide(val rideId: String) : TrackerCommand
    data class ChangeQueueMode(val mode: QueueMode) : TrackerCommand
    data object StartBreak : TrackerCommand
    data object Resume : TrackerCommand
    data object EndShift : TrackerCommand
    data object ArriveHome : TrackerCommand
    data object StartFieldNation : TrackerCommand
    data object ArriveSite : TrackerCommand
    data object StartFieldWork : TrackerCommand
    data object CompleteFieldWork : TrackerCommand
    data object CheckOutFieldWork : TrackerCommand
}
