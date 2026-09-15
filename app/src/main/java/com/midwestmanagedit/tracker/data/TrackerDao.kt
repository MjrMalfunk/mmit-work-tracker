package com.midwestmanagedit.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRide(ride: RideEntity)

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: TrackingEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(point: LocationPointEntity)

    @Query("SELECT * FROM shifts WHERE state != 'COMPLETE' ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActiveShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE state != 'COMPLETE' ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun activeShift(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :shiftId")
    suspend fun shift(shiftId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE state = 'COMPLETE' ORDER BY completedAtEpochMs DESC LIMIT 1")
    suspend fun latestCompletedShift(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE state = 'COMPLETE' ORDER BY completedAtEpochMs DESC LIMIT 1")
    fun observeLatestCompletedShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE state = 'COMPLETE' ORDER BY completedAtEpochMs DESC LIMIT :limit")
    fun observeCompletedShifts(limit: Int): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun ride(rideId: String): RideEntity?

    @Query("SELECT * FROM rides WHERE shiftId = :shiftId AND state = 'PENDING' ORDER BY sequence")
    fun observePendingRides(shiftId: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE shiftId = :shiftId AND state = 'PENDING' ORDER BY sequence")
    suspend fun pendingRides(shiftId: String): List<RideEntity>

    @Query("SELECT COALESCE(MAX(sequence), 0) + 1 FROM rides WHERE shiftId = :shiftId")
    suspend fun nextSequence(shiftId: String): Int

    @Transaction
    @Query("SELECT * FROM shifts WHERE id = :shiftId")
    suspend fun shiftWithRides(shiftId: String): ShiftWithRides?

    @Query("SELECT * FROM tracking_events WHERE shiftId = :shiftId ORDER BY occurredAtEpochMs, rowid")
    suspend fun events(shiftId: String): List<TrackingEventEntity>

    @Query("UPDATE tracking_events SET payloadJson = :payloadJson WHERE shiftId = :shiftId AND type = 'SHIFT_STARTED'")
    suspend fun updateShiftStartedPayload(shiftId: String, payloadJson: String)

    @Query("SELECT * FROM location_points WHERE shiftId = :shiftId ORDER BY occurredAtEpochMs, id")
    suspend fun locations(shiftId: String): List<LocationPointEntity>
}
