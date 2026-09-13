package com.midwestmanagedit.tracker.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrackerDao_Impl implements TrackerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ShiftEntity> __insertionAdapterOfShiftEntity;

  private final EntityInsertionAdapter<RideEntity> __insertionAdapterOfRideEntity;

  private final EntityInsertionAdapter<TrackingEventEntity> __insertionAdapterOfTrackingEventEntity;

  private final EntityInsertionAdapter<LocationPointEntity> __insertionAdapterOfLocationPointEntity;

  private final EntityDeletionOrUpdateAdapter<ShiftEntity> __updateAdapterOfShiftEntity;

  private final EntityDeletionOrUpdateAdapter<RideEntity> __updateAdapterOfRideEntity;

  public TrackerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfShiftEntity = new EntityInsertionAdapter<ShiftEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `shifts` (`id`,`platform`,`state`,`queueMode`,`startedAtEpochMs`,`endedAtEpochMs`,`homeArrivedAtEpochMs`,`startOdometer`,`endOdometer`,`activeRideId`,`workOrderNumber`,`roundTripExpected`,`completedAtEpochMs`,`createdAtEpochMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShiftEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPlatform());
        statement.bindString(3, entity.getState());
        statement.bindString(4, entity.getQueueMode());
        statement.bindLong(5, entity.getStartedAtEpochMs());
        if (entity.getEndedAtEpochMs() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getEndedAtEpochMs());
        }
        if (entity.getHomeArrivedAtEpochMs() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getHomeArrivedAtEpochMs());
        }
        statement.bindDouble(8, entity.getStartOdometer());
        if (entity.getEndOdometer() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getEndOdometer());
        }
        if (entity.getActiveRideId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getActiveRideId());
        }
        if (entity.getWorkOrderNumber() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getWorkOrderNumber());
        }
        final int _tmp = entity.getRoundTripExpected() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getCompletedAtEpochMs() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getCompletedAtEpochMs());
        }
        statement.bindLong(14, entity.getCreatedAtEpochMs());
      }
    };
    this.__insertionAdapterOfRideEntity = new EntityInsertionAdapter<RideEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `rides` (`id`,`shiftId`,`sequence`,`acquisitionMode`,`state`,`queuedAtEpochMs`,`trackingStartedAtEpochMs`,`pickupAtEpochMs`,`dropoffAtEpochMs`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getShiftId());
        statement.bindLong(3, entity.getSequence());
        statement.bindString(4, entity.getAcquisitionMode());
        statement.bindString(5, entity.getState());
        statement.bindLong(6, entity.getQueuedAtEpochMs());
        if (entity.getTrackingStartedAtEpochMs() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getTrackingStartedAtEpochMs());
        }
        if (entity.getPickupAtEpochMs() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getPickupAtEpochMs());
        }
        if (entity.getDropoffAtEpochMs() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDropoffAtEpochMs());
        }
      }
    };
    this.__insertionAdapterOfTrackingEventEntity = new EntityInsertionAdapter<TrackingEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `tracking_events` (`id`,`shiftId`,`rideId`,`type`,`occurredAtEpochMs`,`latitude`,`longitude`,`accuracyMeters`,`payloadJson`,`syncState`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrackingEventEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getShiftId());
        if (entity.getRideId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getRideId());
        }
        statement.bindString(4, entity.getType());
        statement.bindLong(5, entity.getOccurredAtEpochMs());
        if (entity.getLatitude() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getLongitude());
        }
        if (entity.getAccuracyMeters() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getAccuracyMeters());
        }
        statement.bindString(9, entity.getPayloadJson());
        statement.bindString(10, entity.getSyncState());
      }
    };
    this.__insertionAdapterOfLocationPointEntity = new EntityInsertionAdapter<LocationPointEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `location_points` (`id`,`shiftId`,`rideId`,`phase`,`occurredAtEpochMs`,`latitude`,`longitude`,`accuracyMeters`,`speedMetersPerSecond`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocationPointEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getShiftId());
        if (entity.getRideId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getRideId());
        }
        statement.bindString(4, entity.getPhase());
        statement.bindLong(5, entity.getOccurredAtEpochMs());
        statement.bindDouble(6, entity.getLatitude());
        statement.bindDouble(7, entity.getLongitude());
        statement.bindDouble(8, entity.getAccuracyMeters());
        if (entity.getSpeedMetersPerSecond() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getSpeedMetersPerSecond());
        }
      }
    };
    this.__updateAdapterOfShiftEntity = new EntityDeletionOrUpdateAdapter<ShiftEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `shifts` SET `id` = ?,`platform` = ?,`state` = ?,`queueMode` = ?,`startedAtEpochMs` = ?,`endedAtEpochMs` = ?,`homeArrivedAtEpochMs` = ?,`startOdometer` = ?,`endOdometer` = ?,`activeRideId` = ?,`workOrderNumber` = ?,`roundTripExpected` = ?,`completedAtEpochMs` = ?,`createdAtEpochMs` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShiftEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPlatform());
        statement.bindString(3, entity.getState());
        statement.bindString(4, entity.getQueueMode());
        statement.bindLong(5, entity.getStartedAtEpochMs());
        if (entity.getEndedAtEpochMs() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getEndedAtEpochMs());
        }
        if (entity.getHomeArrivedAtEpochMs() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getHomeArrivedAtEpochMs());
        }
        statement.bindDouble(8, entity.getStartOdometer());
        if (entity.getEndOdometer() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getEndOdometer());
        }
        if (entity.getActiveRideId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getActiveRideId());
        }
        if (entity.getWorkOrderNumber() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getWorkOrderNumber());
        }
        final int _tmp = entity.getRoundTripExpected() ? 1 : 0;
        statement.bindLong(12, _tmp);
        if (entity.getCompletedAtEpochMs() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getCompletedAtEpochMs());
        }
        statement.bindLong(14, entity.getCreatedAtEpochMs());
        statement.bindString(15, entity.getId());
      }
    };
    this.__updateAdapterOfRideEntity = new EntityDeletionOrUpdateAdapter<RideEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `rides` SET `id` = ?,`shiftId` = ?,`sequence` = ?,`acquisitionMode` = ?,`state` = ?,`queuedAtEpochMs` = ?,`trackingStartedAtEpochMs` = ?,`pickupAtEpochMs` = ?,`dropoffAtEpochMs` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RideEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getShiftId());
        statement.bindLong(3, entity.getSequence());
        statement.bindString(4, entity.getAcquisitionMode());
        statement.bindString(5, entity.getState());
        statement.bindLong(6, entity.getQueuedAtEpochMs());
        if (entity.getTrackingStartedAtEpochMs() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getTrackingStartedAtEpochMs());
        }
        if (entity.getPickupAtEpochMs() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getPickupAtEpochMs());
        }
        if (entity.getDropoffAtEpochMs() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDropoffAtEpochMs());
        }
        statement.bindString(10, entity.getId());
      }
    };
  }

  @Override
  public Object insertShift(final ShiftEntity shift, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfShiftEntity.insert(shift);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRide(final RideEntity ride, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRideEntity.insert(ride);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertEvent(final TrackingEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrackingEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertLocation(final LocationPointEntity point,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocationPointEntity.insert(point);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateShift(final ShiftEntity shift, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfShiftEntity.handle(shift);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRide(final RideEntity ride, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRideEntity.handle(ride);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ShiftEntity> observeActiveShift() {
    final String _sql = "SELECT * FROM shifts WHERE state != 'COMPLETE' ORDER BY startedAtEpochMs DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shifts"}, new Callable<ShiftEntity>() {
      @Override
      @Nullable
      public ShiftEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueueMode = CursorUtil.getColumnIndexOrThrow(_cursor, "queueMode");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfHomeArrivedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "homeArrivedAtEpochMs");
          final int _cursorIndexOfStartOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "startOdometer");
          final int _cursorIndexOfEndOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "endOdometer");
          final int _cursorIndexOfActiveRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "activeRideId");
          final int _cursorIndexOfWorkOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "workOrderNumber");
          final int _cursorIndexOfRoundTripExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "roundTripExpected");
          final int _cursorIndexOfCompletedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMs");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final ShiftEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final String _tmpQueueMode;
            _tmpQueueMode = _cursor.getString(_cursorIndexOfQueueMode);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final Long _tmpHomeArrivedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfHomeArrivedAtEpochMs)) {
              _tmpHomeArrivedAtEpochMs = null;
            } else {
              _tmpHomeArrivedAtEpochMs = _cursor.getLong(_cursorIndexOfHomeArrivedAtEpochMs);
            }
            final double _tmpStartOdometer;
            _tmpStartOdometer = _cursor.getDouble(_cursorIndexOfStartOdometer);
            final Double _tmpEndOdometer;
            if (_cursor.isNull(_cursorIndexOfEndOdometer)) {
              _tmpEndOdometer = null;
            } else {
              _tmpEndOdometer = _cursor.getDouble(_cursorIndexOfEndOdometer);
            }
            final String _tmpActiveRideId;
            if (_cursor.isNull(_cursorIndexOfActiveRideId)) {
              _tmpActiveRideId = null;
            } else {
              _tmpActiveRideId = _cursor.getString(_cursorIndexOfActiveRideId);
            }
            final String _tmpWorkOrderNumber;
            if (_cursor.isNull(_cursorIndexOfWorkOrderNumber)) {
              _tmpWorkOrderNumber = null;
            } else {
              _tmpWorkOrderNumber = _cursor.getString(_cursorIndexOfWorkOrderNumber);
            }
            final boolean _tmpRoundTripExpected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRoundTripExpected);
            _tmpRoundTripExpected = _tmp != 0;
            final Long _tmpCompletedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfCompletedAtEpochMs)) {
              _tmpCompletedAtEpochMs = null;
            } else {
              _tmpCompletedAtEpochMs = _cursor.getLong(_cursorIndexOfCompletedAtEpochMs);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            _result = new ShiftEntity(_tmpId,_tmpPlatform,_tmpState,_tmpQueueMode,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpHomeArrivedAtEpochMs,_tmpStartOdometer,_tmpEndOdometer,_tmpActiveRideId,_tmpWorkOrderNumber,_tmpRoundTripExpected,_tmpCompletedAtEpochMs,_tmpCreatedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object activeShift(final Continuation<? super ShiftEntity> $completion) {
    final String _sql = "SELECT * FROM shifts WHERE state != 'COMPLETE' ORDER BY startedAtEpochMs DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShiftEntity>() {
      @Override
      @Nullable
      public ShiftEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueueMode = CursorUtil.getColumnIndexOrThrow(_cursor, "queueMode");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfHomeArrivedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "homeArrivedAtEpochMs");
          final int _cursorIndexOfStartOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "startOdometer");
          final int _cursorIndexOfEndOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "endOdometer");
          final int _cursorIndexOfActiveRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "activeRideId");
          final int _cursorIndexOfWorkOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "workOrderNumber");
          final int _cursorIndexOfRoundTripExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "roundTripExpected");
          final int _cursorIndexOfCompletedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMs");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final ShiftEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final String _tmpQueueMode;
            _tmpQueueMode = _cursor.getString(_cursorIndexOfQueueMode);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final Long _tmpHomeArrivedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfHomeArrivedAtEpochMs)) {
              _tmpHomeArrivedAtEpochMs = null;
            } else {
              _tmpHomeArrivedAtEpochMs = _cursor.getLong(_cursorIndexOfHomeArrivedAtEpochMs);
            }
            final double _tmpStartOdometer;
            _tmpStartOdometer = _cursor.getDouble(_cursorIndexOfStartOdometer);
            final Double _tmpEndOdometer;
            if (_cursor.isNull(_cursorIndexOfEndOdometer)) {
              _tmpEndOdometer = null;
            } else {
              _tmpEndOdometer = _cursor.getDouble(_cursorIndexOfEndOdometer);
            }
            final String _tmpActiveRideId;
            if (_cursor.isNull(_cursorIndexOfActiveRideId)) {
              _tmpActiveRideId = null;
            } else {
              _tmpActiveRideId = _cursor.getString(_cursorIndexOfActiveRideId);
            }
            final String _tmpWorkOrderNumber;
            if (_cursor.isNull(_cursorIndexOfWorkOrderNumber)) {
              _tmpWorkOrderNumber = null;
            } else {
              _tmpWorkOrderNumber = _cursor.getString(_cursorIndexOfWorkOrderNumber);
            }
            final boolean _tmpRoundTripExpected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRoundTripExpected);
            _tmpRoundTripExpected = _tmp != 0;
            final Long _tmpCompletedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfCompletedAtEpochMs)) {
              _tmpCompletedAtEpochMs = null;
            } else {
              _tmpCompletedAtEpochMs = _cursor.getLong(_cursorIndexOfCompletedAtEpochMs);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            _result = new ShiftEntity(_tmpId,_tmpPlatform,_tmpState,_tmpQueueMode,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpHomeArrivedAtEpochMs,_tmpStartOdometer,_tmpEndOdometer,_tmpActiveRideId,_tmpWorkOrderNumber,_tmpRoundTripExpected,_tmpCompletedAtEpochMs,_tmpCreatedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object shift(final String shiftId, final Continuation<? super ShiftEntity> $completion) {
    final String _sql = "SELECT * FROM shifts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShiftEntity>() {
      @Override
      @Nullable
      public ShiftEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueueMode = CursorUtil.getColumnIndexOrThrow(_cursor, "queueMode");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfHomeArrivedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "homeArrivedAtEpochMs");
          final int _cursorIndexOfStartOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "startOdometer");
          final int _cursorIndexOfEndOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "endOdometer");
          final int _cursorIndexOfActiveRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "activeRideId");
          final int _cursorIndexOfWorkOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "workOrderNumber");
          final int _cursorIndexOfRoundTripExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "roundTripExpected");
          final int _cursorIndexOfCompletedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMs");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final ShiftEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final String _tmpQueueMode;
            _tmpQueueMode = _cursor.getString(_cursorIndexOfQueueMode);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final Long _tmpHomeArrivedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfHomeArrivedAtEpochMs)) {
              _tmpHomeArrivedAtEpochMs = null;
            } else {
              _tmpHomeArrivedAtEpochMs = _cursor.getLong(_cursorIndexOfHomeArrivedAtEpochMs);
            }
            final double _tmpStartOdometer;
            _tmpStartOdometer = _cursor.getDouble(_cursorIndexOfStartOdometer);
            final Double _tmpEndOdometer;
            if (_cursor.isNull(_cursorIndexOfEndOdometer)) {
              _tmpEndOdometer = null;
            } else {
              _tmpEndOdometer = _cursor.getDouble(_cursorIndexOfEndOdometer);
            }
            final String _tmpActiveRideId;
            if (_cursor.isNull(_cursorIndexOfActiveRideId)) {
              _tmpActiveRideId = null;
            } else {
              _tmpActiveRideId = _cursor.getString(_cursorIndexOfActiveRideId);
            }
            final String _tmpWorkOrderNumber;
            if (_cursor.isNull(_cursorIndexOfWorkOrderNumber)) {
              _tmpWorkOrderNumber = null;
            } else {
              _tmpWorkOrderNumber = _cursor.getString(_cursorIndexOfWorkOrderNumber);
            }
            final boolean _tmpRoundTripExpected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRoundTripExpected);
            _tmpRoundTripExpected = _tmp != 0;
            final Long _tmpCompletedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfCompletedAtEpochMs)) {
              _tmpCompletedAtEpochMs = null;
            } else {
              _tmpCompletedAtEpochMs = _cursor.getLong(_cursorIndexOfCompletedAtEpochMs);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            _result = new ShiftEntity(_tmpId,_tmpPlatform,_tmpState,_tmpQueueMode,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpHomeArrivedAtEpochMs,_tmpStartOdometer,_tmpEndOdometer,_tmpActiveRideId,_tmpWorkOrderNumber,_tmpRoundTripExpected,_tmpCompletedAtEpochMs,_tmpCreatedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object latestCompletedShift(final Continuation<? super ShiftEntity> $completion) {
    final String _sql = "SELECT * FROM shifts WHERE state = 'COMPLETE' ORDER BY completedAtEpochMs DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShiftEntity>() {
      @Override
      @Nullable
      public ShiftEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueueMode = CursorUtil.getColumnIndexOrThrow(_cursor, "queueMode");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfHomeArrivedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "homeArrivedAtEpochMs");
          final int _cursorIndexOfStartOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "startOdometer");
          final int _cursorIndexOfEndOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "endOdometer");
          final int _cursorIndexOfActiveRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "activeRideId");
          final int _cursorIndexOfWorkOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "workOrderNumber");
          final int _cursorIndexOfRoundTripExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "roundTripExpected");
          final int _cursorIndexOfCompletedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMs");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final ShiftEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final String _tmpQueueMode;
            _tmpQueueMode = _cursor.getString(_cursorIndexOfQueueMode);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final Long _tmpHomeArrivedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfHomeArrivedAtEpochMs)) {
              _tmpHomeArrivedAtEpochMs = null;
            } else {
              _tmpHomeArrivedAtEpochMs = _cursor.getLong(_cursorIndexOfHomeArrivedAtEpochMs);
            }
            final double _tmpStartOdometer;
            _tmpStartOdometer = _cursor.getDouble(_cursorIndexOfStartOdometer);
            final Double _tmpEndOdometer;
            if (_cursor.isNull(_cursorIndexOfEndOdometer)) {
              _tmpEndOdometer = null;
            } else {
              _tmpEndOdometer = _cursor.getDouble(_cursorIndexOfEndOdometer);
            }
            final String _tmpActiveRideId;
            if (_cursor.isNull(_cursorIndexOfActiveRideId)) {
              _tmpActiveRideId = null;
            } else {
              _tmpActiveRideId = _cursor.getString(_cursorIndexOfActiveRideId);
            }
            final String _tmpWorkOrderNumber;
            if (_cursor.isNull(_cursorIndexOfWorkOrderNumber)) {
              _tmpWorkOrderNumber = null;
            } else {
              _tmpWorkOrderNumber = _cursor.getString(_cursorIndexOfWorkOrderNumber);
            }
            final boolean _tmpRoundTripExpected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRoundTripExpected);
            _tmpRoundTripExpected = _tmp != 0;
            final Long _tmpCompletedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfCompletedAtEpochMs)) {
              _tmpCompletedAtEpochMs = null;
            } else {
              _tmpCompletedAtEpochMs = _cursor.getLong(_cursorIndexOfCompletedAtEpochMs);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            _result = new ShiftEntity(_tmpId,_tmpPlatform,_tmpState,_tmpQueueMode,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpHomeArrivedAtEpochMs,_tmpStartOdometer,_tmpEndOdometer,_tmpActiveRideId,_tmpWorkOrderNumber,_tmpRoundTripExpected,_tmpCompletedAtEpochMs,_tmpCreatedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ShiftEntity> observeLatestCompletedShift() {
    final String _sql = "SELECT * FROM shifts WHERE state = 'COMPLETE' ORDER BY completedAtEpochMs DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shifts"}, new Callable<ShiftEntity>() {
      @Override
      @Nullable
      public ShiftEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueueMode = CursorUtil.getColumnIndexOrThrow(_cursor, "queueMode");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfHomeArrivedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "homeArrivedAtEpochMs");
          final int _cursorIndexOfStartOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "startOdometer");
          final int _cursorIndexOfEndOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "endOdometer");
          final int _cursorIndexOfActiveRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "activeRideId");
          final int _cursorIndexOfWorkOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "workOrderNumber");
          final int _cursorIndexOfRoundTripExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "roundTripExpected");
          final int _cursorIndexOfCompletedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMs");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final ShiftEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlatform;
            _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final String _tmpQueueMode;
            _tmpQueueMode = _cursor.getString(_cursorIndexOfQueueMode);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final Long _tmpHomeArrivedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfHomeArrivedAtEpochMs)) {
              _tmpHomeArrivedAtEpochMs = null;
            } else {
              _tmpHomeArrivedAtEpochMs = _cursor.getLong(_cursorIndexOfHomeArrivedAtEpochMs);
            }
            final double _tmpStartOdometer;
            _tmpStartOdometer = _cursor.getDouble(_cursorIndexOfStartOdometer);
            final Double _tmpEndOdometer;
            if (_cursor.isNull(_cursorIndexOfEndOdometer)) {
              _tmpEndOdometer = null;
            } else {
              _tmpEndOdometer = _cursor.getDouble(_cursorIndexOfEndOdometer);
            }
            final String _tmpActiveRideId;
            if (_cursor.isNull(_cursorIndexOfActiveRideId)) {
              _tmpActiveRideId = null;
            } else {
              _tmpActiveRideId = _cursor.getString(_cursorIndexOfActiveRideId);
            }
            final String _tmpWorkOrderNumber;
            if (_cursor.isNull(_cursorIndexOfWorkOrderNumber)) {
              _tmpWorkOrderNumber = null;
            } else {
              _tmpWorkOrderNumber = _cursor.getString(_cursorIndexOfWorkOrderNumber);
            }
            final boolean _tmpRoundTripExpected;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRoundTripExpected);
            _tmpRoundTripExpected = _tmp != 0;
            final Long _tmpCompletedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfCompletedAtEpochMs)) {
              _tmpCompletedAtEpochMs = null;
            } else {
              _tmpCompletedAtEpochMs = _cursor.getLong(_cursorIndexOfCompletedAtEpochMs);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            _result = new ShiftEntity(_tmpId,_tmpPlatform,_tmpState,_tmpQueueMode,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpHomeArrivedAtEpochMs,_tmpStartOdometer,_tmpEndOdometer,_tmpActiveRideId,_tmpWorkOrderNumber,_tmpRoundTripExpected,_tmpCompletedAtEpochMs,_tmpCreatedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object ride(final String rideId, final Continuation<? super RideEntity> $completion) {
    final String _sql = "SELECT * FROM rides WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, rideId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RideEntity>() {
      @Override
      @Nullable
      public RideEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShiftId = CursorUtil.getColumnIndexOrThrow(_cursor, "shiftId");
          final int _cursorIndexOfSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "sequence");
          final int _cursorIndexOfAcquisitionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "acquisitionMode");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueuedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "queuedAtEpochMs");
          final int _cursorIndexOfTrackingStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "trackingStartedAtEpochMs");
          final int _cursorIndexOfPickupAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "pickupAtEpochMs");
          final int _cursorIndexOfDropoffAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "dropoffAtEpochMs");
          final RideEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpShiftId;
            _tmpShiftId = _cursor.getString(_cursorIndexOfShiftId);
            final int _tmpSequence;
            _tmpSequence = _cursor.getInt(_cursorIndexOfSequence);
            final String _tmpAcquisitionMode;
            _tmpAcquisitionMode = _cursor.getString(_cursorIndexOfAcquisitionMode);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final long _tmpQueuedAtEpochMs;
            _tmpQueuedAtEpochMs = _cursor.getLong(_cursorIndexOfQueuedAtEpochMs);
            final Long _tmpTrackingStartedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfTrackingStartedAtEpochMs)) {
              _tmpTrackingStartedAtEpochMs = null;
            } else {
              _tmpTrackingStartedAtEpochMs = _cursor.getLong(_cursorIndexOfTrackingStartedAtEpochMs);
            }
            final Long _tmpPickupAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfPickupAtEpochMs)) {
              _tmpPickupAtEpochMs = null;
            } else {
              _tmpPickupAtEpochMs = _cursor.getLong(_cursorIndexOfPickupAtEpochMs);
            }
            final Long _tmpDropoffAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfDropoffAtEpochMs)) {
              _tmpDropoffAtEpochMs = null;
            } else {
              _tmpDropoffAtEpochMs = _cursor.getLong(_cursorIndexOfDropoffAtEpochMs);
            }
            _result = new RideEntity(_tmpId,_tmpShiftId,_tmpSequence,_tmpAcquisitionMode,_tmpState,_tmpQueuedAtEpochMs,_tmpTrackingStartedAtEpochMs,_tmpPickupAtEpochMs,_tmpDropoffAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RideEntity>> observePendingRides(final String shiftId) {
    final String _sql = "SELECT * FROM rides WHERE shiftId = ? AND state = 'PENDING' ORDER BY sequence";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"rides"}, new Callable<List<RideEntity>>() {
      @Override
      @NonNull
      public List<RideEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShiftId = CursorUtil.getColumnIndexOrThrow(_cursor, "shiftId");
          final int _cursorIndexOfSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "sequence");
          final int _cursorIndexOfAcquisitionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "acquisitionMode");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueuedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "queuedAtEpochMs");
          final int _cursorIndexOfTrackingStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "trackingStartedAtEpochMs");
          final int _cursorIndexOfPickupAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "pickupAtEpochMs");
          final int _cursorIndexOfDropoffAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "dropoffAtEpochMs");
          final List<RideEntity> _result = new ArrayList<RideEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RideEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpShiftId;
            _tmpShiftId = _cursor.getString(_cursorIndexOfShiftId);
            final int _tmpSequence;
            _tmpSequence = _cursor.getInt(_cursorIndexOfSequence);
            final String _tmpAcquisitionMode;
            _tmpAcquisitionMode = _cursor.getString(_cursorIndexOfAcquisitionMode);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final long _tmpQueuedAtEpochMs;
            _tmpQueuedAtEpochMs = _cursor.getLong(_cursorIndexOfQueuedAtEpochMs);
            final Long _tmpTrackingStartedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfTrackingStartedAtEpochMs)) {
              _tmpTrackingStartedAtEpochMs = null;
            } else {
              _tmpTrackingStartedAtEpochMs = _cursor.getLong(_cursorIndexOfTrackingStartedAtEpochMs);
            }
            final Long _tmpPickupAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfPickupAtEpochMs)) {
              _tmpPickupAtEpochMs = null;
            } else {
              _tmpPickupAtEpochMs = _cursor.getLong(_cursorIndexOfPickupAtEpochMs);
            }
            final Long _tmpDropoffAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfDropoffAtEpochMs)) {
              _tmpDropoffAtEpochMs = null;
            } else {
              _tmpDropoffAtEpochMs = _cursor.getLong(_cursorIndexOfDropoffAtEpochMs);
            }
            _item = new RideEntity(_tmpId,_tmpShiftId,_tmpSequence,_tmpAcquisitionMode,_tmpState,_tmpQueuedAtEpochMs,_tmpTrackingStartedAtEpochMs,_tmpPickupAtEpochMs,_tmpDropoffAtEpochMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object pendingRides(final String shiftId,
      final Continuation<? super List<RideEntity>> $completion) {
    final String _sql = "SELECT * FROM rides WHERE shiftId = ? AND state = 'PENDING' ORDER BY sequence";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RideEntity>>() {
      @Override
      @NonNull
      public List<RideEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShiftId = CursorUtil.getColumnIndexOrThrow(_cursor, "shiftId");
          final int _cursorIndexOfSequence = CursorUtil.getColumnIndexOrThrow(_cursor, "sequence");
          final int _cursorIndexOfAcquisitionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "acquisitionMode");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfQueuedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "queuedAtEpochMs");
          final int _cursorIndexOfTrackingStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "trackingStartedAtEpochMs");
          final int _cursorIndexOfPickupAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "pickupAtEpochMs");
          final int _cursorIndexOfDropoffAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "dropoffAtEpochMs");
          final List<RideEntity> _result = new ArrayList<RideEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RideEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpShiftId;
            _tmpShiftId = _cursor.getString(_cursorIndexOfShiftId);
            final int _tmpSequence;
            _tmpSequence = _cursor.getInt(_cursorIndexOfSequence);
            final String _tmpAcquisitionMode;
            _tmpAcquisitionMode = _cursor.getString(_cursorIndexOfAcquisitionMode);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final long _tmpQueuedAtEpochMs;
            _tmpQueuedAtEpochMs = _cursor.getLong(_cursorIndexOfQueuedAtEpochMs);
            final Long _tmpTrackingStartedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfTrackingStartedAtEpochMs)) {
              _tmpTrackingStartedAtEpochMs = null;
            } else {
              _tmpTrackingStartedAtEpochMs = _cursor.getLong(_cursorIndexOfTrackingStartedAtEpochMs);
            }
            final Long _tmpPickupAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfPickupAtEpochMs)) {
              _tmpPickupAtEpochMs = null;
            } else {
              _tmpPickupAtEpochMs = _cursor.getLong(_cursorIndexOfPickupAtEpochMs);
            }
            final Long _tmpDropoffAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfDropoffAtEpochMs)) {
              _tmpDropoffAtEpochMs = null;
            } else {
              _tmpDropoffAtEpochMs = _cursor.getLong(_cursorIndexOfDropoffAtEpochMs);
            }
            _item = new RideEntity(_tmpId,_tmpShiftId,_tmpSequence,_tmpAcquisitionMode,_tmpState,_tmpQueuedAtEpochMs,_tmpTrackingStartedAtEpochMs,_tmpPickupAtEpochMs,_tmpDropoffAtEpochMs);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object nextSequence(final String shiftId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COALESCE(MAX(sequence), 0) + 1 FROM rides WHERE shiftId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object shiftWithRides(final String shiftId,
      final Continuation<? super ShiftWithRides> $completion) {
    final String _sql = "SELECT * FROM shifts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, true, _cancellationSignal, new Callable<ShiftWithRides>() {
      @Override
      @Nullable
      public ShiftWithRides call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfPlatform = CursorUtil.getColumnIndexOrThrow(_cursor, "platform");
            final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
            final int _cursorIndexOfQueueMode = CursorUtil.getColumnIndexOrThrow(_cursor, "queueMode");
            final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
            final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
            final int _cursorIndexOfHomeArrivedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "homeArrivedAtEpochMs");
            final int _cursorIndexOfStartOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "startOdometer");
            final int _cursorIndexOfEndOdometer = CursorUtil.getColumnIndexOrThrow(_cursor, "endOdometer");
            final int _cursorIndexOfActiveRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "activeRideId");
            final int _cursorIndexOfWorkOrderNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "workOrderNumber");
            final int _cursorIndexOfRoundTripExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "roundTripExpected");
            final int _cursorIndexOfCompletedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMs");
            final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
            final ArrayMap<String, ArrayList<RideEntity>> _collectionRides = new ArrayMap<String, ArrayList<RideEntity>>();
            while (_cursor.moveToNext()) {
              final String _tmpKey;
              _tmpKey = _cursor.getString(_cursorIndexOfId);
              if (!_collectionRides.containsKey(_tmpKey)) {
                _collectionRides.put(_tmpKey, new ArrayList<RideEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipridesAscomMidwestmanageditTrackerDataRideEntity(_collectionRides);
            final ShiftWithRides _result;
            if (_cursor.moveToFirst()) {
              final ShiftEntity _tmpShift;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpPlatform;
              _tmpPlatform = _cursor.getString(_cursorIndexOfPlatform);
              final String _tmpState;
              _tmpState = _cursor.getString(_cursorIndexOfState);
              final String _tmpQueueMode;
              _tmpQueueMode = _cursor.getString(_cursorIndexOfQueueMode);
              final long _tmpStartedAtEpochMs;
              _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
              final Long _tmpEndedAtEpochMs;
              if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
                _tmpEndedAtEpochMs = null;
              } else {
                _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
              }
              final Long _tmpHomeArrivedAtEpochMs;
              if (_cursor.isNull(_cursorIndexOfHomeArrivedAtEpochMs)) {
                _tmpHomeArrivedAtEpochMs = null;
              } else {
                _tmpHomeArrivedAtEpochMs = _cursor.getLong(_cursorIndexOfHomeArrivedAtEpochMs);
              }
              final double _tmpStartOdometer;
              _tmpStartOdometer = _cursor.getDouble(_cursorIndexOfStartOdometer);
              final Double _tmpEndOdometer;
              if (_cursor.isNull(_cursorIndexOfEndOdometer)) {
                _tmpEndOdometer = null;
              } else {
                _tmpEndOdometer = _cursor.getDouble(_cursorIndexOfEndOdometer);
              }
              final String _tmpActiveRideId;
              if (_cursor.isNull(_cursorIndexOfActiveRideId)) {
                _tmpActiveRideId = null;
              } else {
                _tmpActiveRideId = _cursor.getString(_cursorIndexOfActiveRideId);
              }
              final String _tmpWorkOrderNumber;
              if (_cursor.isNull(_cursorIndexOfWorkOrderNumber)) {
                _tmpWorkOrderNumber = null;
              } else {
                _tmpWorkOrderNumber = _cursor.getString(_cursorIndexOfWorkOrderNumber);
              }
              final boolean _tmpRoundTripExpected;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfRoundTripExpected);
              _tmpRoundTripExpected = _tmp != 0;
              final Long _tmpCompletedAtEpochMs;
              if (_cursor.isNull(_cursorIndexOfCompletedAtEpochMs)) {
                _tmpCompletedAtEpochMs = null;
              } else {
                _tmpCompletedAtEpochMs = _cursor.getLong(_cursorIndexOfCompletedAtEpochMs);
              }
              final long _tmpCreatedAtEpochMs;
              _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
              _tmpShift = new ShiftEntity(_tmpId,_tmpPlatform,_tmpState,_tmpQueueMode,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpHomeArrivedAtEpochMs,_tmpStartOdometer,_tmpEndOdometer,_tmpActiveRideId,_tmpWorkOrderNumber,_tmpRoundTripExpected,_tmpCompletedAtEpochMs,_tmpCreatedAtEpochMs);
              final ArrayList<RideEntity> _tmpRidesCollection;
              final String _tmpKey_1;
              _tmpKey_1 = _cursor.getString(_cursorIndexOfId);
              _tmpRidesCollection = _collectionRides.get(_tmpKey_1);
              _result = new ShiftWithRides(_tmpShift,_tmpRidesCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
            _statement.release();
          }
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object events(final String shiftId,
      final Continuation<? super List<TrackingEventEntity>> $completion) {
    final String _sql = "SELECT * FROM tracking_events WHERE shiftId = ? ORDER BY occurredAtEpochMs, rowid";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrackingEventEntity>>() {
      @Override
      @NonNull
      public List<TrackingEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShiftId = CursorUtil.getColumnIndexOrThrow(_cursor, "shiftId");
          final int _cursorIndexOfRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "rideId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfOccurredAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "occurredAtEpochMs");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracyMeters");
          final int _cursorIndexOfPayloadJson = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadJson");
          final int _cursorIndexOfSyncState = CursorUtil.getColumnIndexOrThrow(_cursor, "syncState");
          final List<TrackingEventEntity> _result = new ArrayList<TrackingEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrackingEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpShiftId;
            _tmpShiftId = _cursor.getString(_cursorIndexOfShiftId);
            final String _tmpRideId;
            if (_cursor.isNull(_cursorIndexOfRideId)) {
              _tmpRideId = null;
            } else {
              _tmpRideId = _cursor.getString(_cursorIndexOfRideId);
            }
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpOccurredAtEpochMs;
            _tmpOccurredAtEpochMs = _cursor.getLong(_cursorIndexOfOccurredAtEpochMs);
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final Float _tmpAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfAccuracyMeters)) {
              _tmpAccuracyMeters = null;
            } else {
              _tmpAccuracyMeters = _cursor.getFloat(_cursorIndexOfAccuracyMeters);
            }
            final String _tmpPayloadJson;
            _tmpPayloadJson = _cursor.getString(_cursorIndexOfPayloadJson);
            final String _tmpSyncState;
            _tmpSyncState = _cursor.getString(_cursorIndexOfSyncState);
            _item = new TrackingEventEntity(_tmpId,_tmpShiftId,_tmpRideId,_tmpType,_tmpOccurredAtEpochMs,_tmpLatitude,_tmpLongitude,_tmpAccuracyMeters,_tmpPayloadJson,_tmpSyncState);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object locations(final String shiftId,
      final Continuation<? super List<LocationPointEntity>> $completion) {
    final String _sql = "SELECT * FROM location_points WHERE shiftId = ? ORDER BY occurredAtEpochMs, id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, shiftId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocationPointEntity>>() {
      @Override
      @NonNull
      public List<LocationPointEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShiftId = CursorUtil.getColumnIndexOrThrow(_cursor, "shiftId");
          final int _cursorIndexOfRideId = CursorUtil.getColumnIndexOrThrow(_cursor, "rideId");
          final int _cursorIndexOfPhase = CursorUtil.getColumnIndexOrThrow(_cursor, "phase");
          final int _cursorIndexOfOccurredAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "occurredAtEpochMs");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracyMeters");
          final int _cursorIndexOfSpeedMetersPerSecond = CursorUtil.getColumnIndexOrThrow(_cursor, "speedMetersPerSecond");
          final List<LocationPointEntity> _result = new ArrayList<LocationPointEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocationPointEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpShiftId;
            _tmpShiftId = _cursor.getString(_cursorIndexOfShiftId);
            final String _tmpRideId;
            if (_cursor.isNull(_cursorIndexOfRideId)) {
              _tmpRideId = null;
            } else {
              _tmpRideId = _cursor.getString(_cursorIndexOfRideId);
            }
            final String _tmpPhase;
            _tmpPhase = _cursor.getString(_cursorIndexOfPhase);
            final long _tmpOccurredAtEpochMs;
            _tmpOccurredAtEpochMs = _cursor.getLong(_cursorIndexOfOccurredAtEpochMs);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpAccuracyMeters;
            _tmpAccuracyMeters = _cursor.getFloat(_cursorIndexOfAccuracyMeters);
            final Float _tmpSpeedMetersPerSecond;
            if (_cursor.isNull(_cursorIndexOfSpeedMetersPerSecond)) {
              _tmpSpeedMetersPerSecond = null;
            } else {
              _tmpSpeedMetersPerSecond = _cursor.getFloat(_cursorIndexOfSpeedMetersPerSecond);
            }
            _item = new LocationPointEntity(_tmpId,_tmpShiftId,_tmpRideId,_tmpPhase,_tmpOccurredAtEpochMs,_tmpLatitude,_tmpLongitude,_tmpAccuracyMeters,_tmpSpeedMetersPerSecond);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshipridesAscomMidwestmanageditTrackerDataRideEntity(
      @NonNull final ArrayMap<String, ArrayList<RideEntity>> _map) {
    final Set<String> __mapKeySet = _map.keySet();
    if (__mapKeySet.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchArrayMap(_map, true, (map) -> {
        __fetchRelationshipridesAscomMidwestmanageditTrackerDataRideEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`shiftId`,`sequence`,`acquisitionMode`,`state`,`queuedAtEpochMs`,`trackingStartedAtEpochMs`,`pickupAtEpochMs`,`dropoffAtEpochMs` FROM `rides` WHERE `shiftId` IN (");
    final int _inputSize = __mapKeySet.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : __mapKeySet) {
      _stmt.bindString(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "shiftId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfShiftId = 1;
      final int _cursorIndexOfSequence = 2;
      final int _cursorIndexOfAcquisitionMode = 3;
      final int _cursorIndexOfState = 4;
      final int _cursorIndexOfQueuedAtEpochMs = 5;
      final int _cursorIndexOfTrackingStartedAtEpochMs = 6;
      final int _cursorIndexOfPickupAtEpochMs = 7;
      final int _cursorIndexOfDropoffAtEpochMs = 8;
      while (_cursor.moveToNext()) {
        final String _tmpKey;
        _tmpKey = _cursor.getString(_itemKeyIndex);
        final ArrayList<RideEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final RideEntity _item_1;
          final String _tmpId;
          _tmpId = _cursor.getString(_cursorIndexOfId);
          final String _tmpShiftId;
          _tmpShiftId = _cursor.getString(_cursorIndexOfShiftId);
          final int _tmpSequence;
          _tmpSequence = _cursor.getInt(_cursorIndexOfSequence);
          final String _tmpAcquisitionMode;
          _tmpAcquisitionMode = _cursor.getString(_cursorIndexOfAcquisitionMode);
          final String _tmpState;
          _tmpState = _cursor.getString(_cursorIndexOfState);
          final long _tmpQueuedAtEpochMs;
          _tmpQueuedAtEpochMs = _cursor.getLong(_cursorIndexOfQueuedAtEpochMs);
          final Long _tmpTrackingStartedAtEpochMs;
          if (_cursor.isNull(_cursorIndexOfTrackingStartedAtEpochMs)) {
            _tmpTrackingStartedAtEpochMs = null;
          } else {
            _tmpTrackingStartedAtEpochMs = _cursor.getLong(_cursorIndexOfTrackingStartedAtEpochMs);
          }
          final Long _tmpPickupAtEpochMs;
          if (_cursor.isNull(_cursorIndexOfPickupAtEpochMs)) {
            _tmpPickupAtEpochMs = null;
          } else {
            _tmpPickupAtEpochMs = _cursor.getLong(_cursorIndexOfPickupAtEpochMs);
          }
          final Long _tmpDropoffAtEpochMs;
          if (_cursor.isNull(_cursorIndexOfDropoffAtEpochMs)) {
            _tmpDropoffAtEpochMs = null;
          } else {
            _tmpDropoffAtEpochMs = _cursor.getLong(_cursorIndexOfDropoffAtEpochMs);
          }
          _item_1 = new RideEntity(_tmpId,_tmpShiftId,_tmpSequence,_tmpAcquisitionMode,_tmpState,_tmpQueuedAtEpochMs,_tmpTrackingStartedAtEpochMs,_tmpPickupAtEpochMs,_tmpDropoffAtEpochMs);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
