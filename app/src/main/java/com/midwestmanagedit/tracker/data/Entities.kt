package com.midwestmanagedit.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val state: String,
    val queueMode: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val homeArrivedAtEpochMs: Long? = null,
    val startOdometer: Double,
    val endOdometer: Double? = null,
    val activeRideId: String? = null,
    val workOrderNumber: String? = null,
    @ColumnInfo(defaultValue = "0") val roundTripExpected: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val closingNote: String? = null,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "rides",
    indices = [Index("shiftId"), Index("state")],
)
data class RideEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val sequence: Int,
    val acquisitionMode: String,
    val state: String,
    val queuedAtEpochMs: Long,
    val trackingStartedAtEpochMs: Long? = null,
    val pickupAtEpochMs: Long? = null,
    val dropoffAtEpochMs: Long? = null,
)

@Entity(
    tableName = "tracking_events",
    indices = [Index("shiftId"), Index("rideId"), Index("syncState")],
)
data class TrackingEventEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val rideId: String?,
    val type: String,
    val occurredAtEpochMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val payloadJson: String = "{}",
    val syncState: String = "PENDING",
)

@Entity(
    tableName = "location_points",
    indices = [Index("shiftId"), Index("rideId"), Index("occurredAtEpochMs")],
)
data class LocationPointEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val rideId: String?,
    val phase: String,
    val occurredAtEpochMs: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
)

data class ActiveSnapshot(
    val shiftId: String,
    val shiftState: String,
    val queueMode: String,
    val activeRideId: String?,
)

data class ShiftWithRides(
    @androidx.room.Embedded val shift: ShiftEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "shiftId")
    val rides: List<RideEntity>,
)
