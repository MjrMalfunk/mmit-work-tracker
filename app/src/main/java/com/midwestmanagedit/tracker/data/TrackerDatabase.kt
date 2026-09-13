package com.midwestmanagedit.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ShiftEntity::class,
        RideEntity::class,
        TrackingEventEntity::class,
        LocationPointEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile private var instance: TrackerDatabase? = null

        fun get(context: Context): TrackerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TrackerDatabase::class.java,
                "mmit-tracker.db",
            ).addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shifts ADD COLUMN workOrderNumber TEXT")
                db.execSQL("ALTER TABLE shifts ADD COLUMN roundTripExpected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shifts ADD COLUMN completedAtEpochMs INTEGER")
                db.execSQL(
                    "UPDATE shifts SET completedAtEpochMs = homeArrivedAtEpochMs " +
                        "WHERE state = 'COMPLETE' AND completedAtEpochMs IS NULL",
                )
            }
        }
    }
}
