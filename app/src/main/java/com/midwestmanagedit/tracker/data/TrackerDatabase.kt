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
        ShiftMaterialUsageEntity::class,
    ],
    version = 5,
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shifts ADD COLUMN offlineOdometer REAL")
                db.execSQL("ALTER TABLE shifts ADD COLUMN correctedHomeArrivedAtEpochMs INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shifts ADD COLUMN closingNote TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `shift_material_usage` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `sku` TEXT NOT NULL, `quantity` REAL NOT NULL, `notes` TEXT, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_material_usage_shiftId` ON `shift_material_usage` (`shiftId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_material_usage_shiftId_sku` ON `shift_material_usage` (`shiftId`, `sku`)")
            }
        }
    }
}
