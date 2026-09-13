package com.midwestmanagedit.tracker.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrackerDatabase_Impl extends TrackerDatabase {
  private volatile TrackerDao _trackerDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `shifts` (`id` TEXT NOT NULL, `platform` TEXT NOT NULL, `state` TEXT NOT NULL, `queueMode` TEXT NOT NULL, `startedAtEpochMs` INTEGER NOT NULL, `endedAtEpochMs` INTEGER, `homeArrivedAtEpochMs` INTEGER, `startOdometer` REAL NOT NULL, `endOdometer` REAL, `activeRideId` TEXT, `workOrderNumber` TEXT, `roundTripExpected` INTEGER NOT NULL DEFAULT 0, `completedAtEpochMs` INTEGER, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `rides` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `acquisitionMode` TEXT NOT NULL, `state` TEXT NOT NULL, `queuedAtEpochMs` INTEGER NOT NULL, `trackingStartedAtEpochMs` INTEGER, `pickupAtEpochMs` INTEGER, `dropoffAtEpochMs` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rides_shiftId` ON `rides` (`shiftId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rides_state` ON `rides` (`state`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tracking_events` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `rideId` TEXT, `type` TEXT NOT NULL, `occurredAtEpochMs` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `accuracyMeters` REAL, `payloadJson` TEXT NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracking_events_shiftId` ON `tracking_events` (`shiftId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracking_events_rideId` ON `tracking_events` (`rideId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracking_events_syncState` ON `tracking_events` (`syncState`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `location_points` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `rideId` TEXT, `phase` TEXT NOT NULL, `occurredAtEpochMs` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracyMeters` REAL NOT NULL, `speedMetersPerSecond` REAL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_location_points_shiftId` ON `location_points` (`shiftId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_location_points_rideId` ON `location_points` (`rideId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_location_points_occurredAtEpochMs` ON `location_points` (`occurredAtEpochMs`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '94d7ce9b133879493d1da506bc9fd32c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `shifts`");
        db.execSQL("DROP TABLE IF EXISTS `rides`");
        db.execSQL("DROP TABLE IF EXISTS `tracking_events`");
        db.execSQL("DROP TABLE IF EXISTS `location_points`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsShifts = new HashMap<String, TableInfo.Column>(14);
        _columnsShifts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("platform", new TableInfo.Column("platform", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("state", new TableInfo.Column("state", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("queueMode", new TableInfo.Column("queueMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("startedAtEpochMs", new TableInfo.Column("startedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("endedAtEpochMs", new TableInfo.Column("endedAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("homeArrivedAtEpochMs", new TableInfo.Column("homeArrivedAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("startOdometer", new TableInfo.Column("startOdometer", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("endOdometer", new TableInfo.Column("endOdometer", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("activeRideId", new TableInfo.Column("activeRideId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("workOrderNumber", new TableInfo.Column("workOrderNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("roundTripExpected", new TableInfo.Column("roundTripExpected", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("completedAtEpochMs", new TableInfo.Column("completedAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShifts.put("createdAtEpochMs", new TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShifts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShifts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoShifts = new TableInfo("shifts", _columnsShifts, _foreignKeysShifts, _indicesShifts);
        final TableInfo _existingShifts = TableInfo.read(db, "shifts");
        if (!_infoShifts.equals(_existingShifts)) {
          return new RoomOpenHelper.ValidationResult(false, "shifts(com.midwestmanagedit.tracker.data.ShiftEntity).\n"
                  + " Expected:\n" + _infoShifts + "\n"
                  + " Found:\n" + _existingShifts);
        }
        final HashMap<String, TableInfo.Column> _columnsRides = new HashMap<String, TableInfo.Column>(9);
        _columnsRides.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("shiftId", new TableInfo.Column("shiftId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("sequence", new TableInfo.Column("sequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("acquisitionMode", new TableInfo.Column("acquisitionMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("state", new TableInfo.Column("state", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("queuedAtEpochMs", new TableInfo.Column("queuedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("trackingStartedAtEpochMs", new TableInfo.Column("trackingStartedAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("pickupAtEpochMs", new TableInfo.Column("pickupAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("dropoffAtEpochMs", new TableInfo.Column("dropoffAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRides = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRides = new HashSet<TableInfo.Index>(2);
        _indicesRides.add(new TableInfo.Index("index_rides_shiftId", false, Arrays.asList("shiftId"), Arrays.asList("ASC")));
        _indicesRides.add(new TableInfo.Index("index_rides_state", false, Arrays.asList("state"), Arrays.asList("ASC")));
        final TableInfo _infoRides = new TableInfo("rides", _columnsRides, _foreignKeysRides, _indicesRides);
        final TableInfo _existingRides = TableInfo.read(db, "rides");
        if (!_infoRides.equals(_existingRides)) {
          return new RoomOpenHelper.ValidationResult(false, "rides(com.midwestmanagedit.tracker.data.RideEntity).\n"
                  + " Expected:\n" + _infoRides + "\n"
                  + " Found:\n" + _existingRides);
        }
        final HashMap<String, TableInfo.Column> _columnsTrackingEvents = new HashMap<String, TableInfo.Column>(10);
        _columnsTrackingEvents.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("shiftId", new TableInfo.Column("shiftId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("rideId", new TableInfo.Column("rideId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("occurredAtEpochMs", new TableInfo.Column("occurredAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("accuracyMeters", new TableInfo.Column("accuracyMeters", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("payloadJson", new TableInfo.Column("payloadJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrackingEvents.put("syncState", new TableInfo.Column("syncState", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrackingEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrackingEvents = new HashSet<TableInfo.Index>(3);
        _indicesTrackingEvents.add(new TableInfo.Index("index_tracking_events_shiftId", false, Arrays.asList("shiftId"), Arrays.asList("ASC")));
        _indicesTrackingEvents.add(new TableInfo.Index("index_tracking_events_rideId", false, Arrays.asList("rideId"), Arrays.asList("ASC")));
        _indicesTrackingEvents.add(new TableInfo.Index("index_tracking_events_syncState", false, Arrays.asList("syncState"), Arrays.asList("ASC")));
        final TableInfo _infoTrackingEvents = new TableInfo("tracking_events", _columnsTrackingEvents, _foreignKeysTrackingEvents, _indicesTrackingEvents);
        final TableInfo _existingTrackingEvents = TableInfo.read(db, "tracking_events");
        if (!_infoTrackingEvents.equals(_existingTrackingEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "tracking_events(com.midwestmanagedit.tracker.data.TrackingEventEntity).\n"
                  + " Expected:\n" + _infoTrackingEvents + "\n"
                  + " Found:\n" + _existingTrackingEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsLocationPoints = new HashMap<String, TableInfo.Column>(9);
        _columnsLocationPoints.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("shiftId", new TableInfo.Column("shiftId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("rideId", new TableInfo.Column("rideId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("phase", new TableInfo.Column("phase", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("occurredAtEpochMs", new TableInfo.Column("occurredAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("accuracyMeters", new TableInfo.Column("accuracyMeters", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationPoints.put("speedMetersPerSecond", new TableInfo.Column("speedMetersPerSecond", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocationPoints = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocationPoints = new HashSet<TableInfo.Index>(3);
        _indicesLocationPoints.add(new TableInfo.Index("index_location_points_shiftId", false, Arrays.asList("shiftId"), Arrays.asList("ASC")));
        _indicesLocationPoints.add(new TableInfo.Index("index_location_points_rideId", false, Arrays.asList("rideId"), Arrays.asList("ASC")));
        _indicesLocationPoints.add(new TableInfo.Index("index_location_points_occurredAtEpochMs", false, Arrays.asList("occurredAtEpochMs"), Arrays.asList("ASC")));
        final TableInfo _infoLocationPoints = new TableInfo("location_points", _columnsLocationPoints, _foreignKeysLocationPoints, _indicesLocationPoints);
        final TableInfo _existingLocationPoints = TableInfo.read(db, "location_points");
        if (!_infoLocationPoints.equals(_existingLocationPoints)) {
          return new RoomOpenHelper.ValidationResult(false, "location_points(com.midwestmanagedit.tracker.data.LocationPointEntity).\n"
                  + " Expected:\n" + _infoLocationPoints + "\n"
                  + " Found:\n" + _existingLocationPoints);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "94d7ce9b133879493d1da506bc9fd32c", "cf1a540fb3ec559705076706f111005f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "shifts","rides","tracking_events","location_points");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `shifts`");
      _db.execSQL("DELETE FROM `rides`");
      _db.execSQL("DELETE FROM `tracking_events`");
      _db.execSQL("DELETE FROM `location_points`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TrackerDao.class, TrackerDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TrackerDao trackerDao() {
    if (_trackerDao != null) {
      return _trackerDao;
    } else {
      synchronized(this) {
        if(_trackerDao == null) {
          _trackerDao = new TrackerDao_Impl(this);
        }
        return _trackerDao;
      }
    }
  }
}
