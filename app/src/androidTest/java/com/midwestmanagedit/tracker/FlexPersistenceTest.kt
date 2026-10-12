package com.midwestmanagedit.tracker

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.database.sqlite.SQLiteDatabase
import com.midwestmanagedit.tracker.data.*
import com.midwestmanagedit.tracker.domain.*
import com.midwestmanagedit.tracker.export.ShiftExporter
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlexPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun stamp(minutes: Int) = GeoStamp(1_800_000_000_000L + minutes * 60_000L, null, null, null)
    private val block = FlexBlock.parse("2026-10-13", "04:15", "3.5", "79", "Test station", ZoneId.of("America/New_York"))

    @Test fun migrationPreservesLegacyOutingAndOpensWithRoomValidation() = runBlocking {
        val name = "flex-migration-regression.db"
        context.deleteDatabase(name)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("tracker-v5.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val path = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indexes = entity.getJSONArray("indices")
                for (j in 0 until indexes.length()) db.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO shifts (id,platform,state,queueMode,startedAtEpochMs,startOdometer,endOdometer,offlineOdometer,completedAtEpochMs,roundTripExpected,createdAtEpochMs) VALUES ('legacy','LYFT','COMPLETE','MANUAL',1000,85943,86217,86202,2000,0,1000)")
            db.version = 5
        }
        val db = Room.databaseBuilder(context, TrackerDatabase::class.java, name).addMigrations(TrackerDatabase.MIGRATION_5_6).build()
        try {
            val legacy = db.trackerDao().shift("legacy")!!
            assertEquals(85943.0, legacy.startOdometer, 0.0)
            assertEquals(86217.0, legacy.endOdometer!!, 0.0)
            assertEquals(86202.0, legacy.offlineOdometer!!, 0.0)
            assertNull(legacy.flexBlockPayCents)
            assertEquals("LYFT", legacy.platform)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun normalFlexOutingPersistsAndExportsWithoutPassengerRides() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        try {
            val repo = TrackerRepository(db)
            repo.startFlex(block, 1000.0, stamp(0))
            val id = repo.dao.activeShift()!!.id
            assertTrue(runCatching { repo.startFlex(block, 1000.0, stamp(0)) }.isFailure)
            repo.flexAction(FlexAction.ARRIVE_STATION, stamp(5))
            repo.flexAction(FlexAction.START_DELIVERIES, stamp(15))
            repo.flexAction(FlexAction.FINISH_DELIVERIES, stamp(110))
            repo.lockHomeArrival(id, stamp(125))
            repo.finishHomeArrival(id, 1080.0, "1070")
            val shift = repo.dao.shift(id)!!
            assertEquals("COMPLETE", shift.state)
            assertEquals(stamp(110).occurredAtEpochMs, shift.endedAtEpochMs)
            assertEquals(stamp(125).occurredAtEpochMs, shift.homeArrivedAtEpochMs)
            assertEquals(7900L, shift.flexBlockPayCents)
            assertTrue(repo.shiftWithRides(id)!!.rides.isEmpty())
            val export = ShiftExporter(context, repo).export(id)
            try {
                val json = JSONObject(export.readText())
                assertEquals("AMAZON_FLEX", json.getJSONObject("shift").getString("platform"))
                assertEquals(7900L, json.getJSONObject("flexBlock").getLong("offeredPayCents"))
                assertEquals(80.0, json.getJSONObject("mileageSummary").getDouble("totalOdometerMiles"), 0.0)
                assertEquals(10.0, json.getJSONObject("mileageSummary").getDouble("offlineOdometerMiles"), 0.0)
                assertEquals(0, json.getJSONArray("rides").length())
            } finally { export.delete() }
        } finally { db.close() }
    }

    @Test fun packageReturnAndNoRouteReleaseHaveCorrectFinishBoundaries() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        try {
            val repo = TrackerRepository(db)
            repo.startFlex(block, 1000.0, stamp(0))
            val id = repo.dao.activeShift()!!.id
            repo.flexAction(FlexAction.ARRIVE_STATION, stamp(5))
            repo.flexAction(FlexAction.START_DELIVERIES, stamp(15))
            repo.flexAction(FlexAction.RETURN_PACKAGES, stamp(110))
            assertNull(repo.dao.activeShift()!!.endedAtEpochMs)
            assertTrue(runCatching { repo.lockHomeArrival(id, stamp(120)) }.isFailure)
            repo.flexAction(FlexAction.FINISH_RETURN, stamp(140))
            assertEquals(stamp(140).occurredAtEpochMs, repo.dao.activeShift()!!.endedAtEpochMs)
            repo.lockHomeArrival(id, stamp(150))
            repo.finishHomeArrival(id, 1100.0)
            repo.startFlex(block, 1100.0, stamp(200))
            repo.flexAction(FlexAction.ARRIVE_STATION, stamp(205))
            repo.flexAction(FlexAction.RELEASE_NO_ROUTE, stamp(215))
            assertEquals(stamp(215).occurredAtEpochMs, repo.dao.activeShift()!!.endedAtEpochMs)
            assertEquals(EventType.FLEX_RELEASED_NO_ROUTE.name, repo.events(repo.dao.activeShift()!!.id).first { it.type.startsWith("FLEX_RELEASED") }.type)
        } finally { db.close() }
    }
}
