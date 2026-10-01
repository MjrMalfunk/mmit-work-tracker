package com.midwestmanagedit.tracker.domain

import org.junit.Assert.*
import org.junit.Test

class OutingMileageTest {
    @Test fun wholeOutingDoesNotAddReturnTwice() {
        val split = OutingMileage.split(85556.0, 85635.0, 85640.0)
        assertEquals(84.0, split.total!!, 0.00001)
        assertEquals(79.0, split.online!!, 0.00001)
        assertEquals(5.0, split.offline!!, 0.00001)
        assertEquals(split.total!!, split.online!! + split.offline!!, 0.00001)
    }

    @Test fun oldExportKeepsKnownTotalButDoesNotInventSplit() {
        val split = OutingMileage.split(85556.0, null, 85640.0)
        assertEquals(84.0, split.total!!, 0.00001)
        assertNull(split.online)
        assertNull(split.offline)
    }

    @Test fun invalidReadingsAreRejected() {
        for (readings in listOf(Triple(100.0, 99.0, 110.0), Triple(100.0, 120.0, 110.0),
            Triple(Double.NaN, 105.0, 110.0), Triple(100.0, Double.POSITIVE_INFINITY, 110.0))) {
            assertThrows(IllegalArgumentException::class.java) { OutingMileage.split(readings.first, readings.second, readings.third) }
        }
    }

    @Test fun boundaryLinkIsAllocatedOnceEvenWithoutFixAtOfflineTap() {
        val points = listOf(point(0, 0.0), point(30_000, .003), point(60_000, .006))
        val total = OutingMileage.gps(points, 0, 60_000).miles!!
        val online = OutingMileage.gps(points, 0, 40_000).miles!!
        val offline = OutingMileage.gps(points, 40_000, 60_000).miles!!
        assertEquals(total, online + offline, 0.000001)
        assertTrue(offline > 0)
    }

    @Test fun missingGpsIsUnknownAndDoesNotBecomeZero() {
        assertNull(OutingMileage.gps(emptyList(), 0, 60_000).miles)
        assertNull(OutingMileage.gps(listOf(point(10_000, 0.0)), 0, 60_000).miles)
    }

    @Test fun stoppedAtHomeDoesNotAutomaticallyChangeArrival() {
        val estimate = OutingMileage.gps(listOf(point(0, 0.0), point(60_000, 0.0)), 0, 360_000)
        assertEquals(0.0, estimate.miles!!, 0.0)
        assertEquals(300_000, estimate.uncoveredEndMs)
        assertEquals(60_000, OutingMileage.correctedArrival(0, 360_000, 5.0))
        assertThrows(IllegalArgumentException::class.java) { OutingMileage.correctedArrival(0, 360_000, 7.0) }
    }

    @Test fun badFixesDoNotBridgeIntoFakeMileage() {
        val bad = point(30_000, .003).copy(accuracy = 500f)
        val result = OutingMileage.gps(listOf(point(0, 0.0), bad, point(60_000, .006)), 0, 60_000)
        assertNull(result.miles)
        assertEquals(2, result.rejectedLinks)
    }

    @Test fun gapsAreReportedAndDuplicatesDoNotAddMiles() {
        val points = listOf(point(0, 0.0), point(120_000, .003))
        val result = OutingMileage.gps(points + points, 0, 120_000)
        assertEquals(1, result.links)
        assertEquals(1, result.longGaps)
        assertEquals(OutingMileage.gps(points, 0, 120_000).miles!!, result.miles!!, 0.0)
    }

    private fun point(time: Long, longitude: Double) = OutingMileage.Point(time, 0.0, longitude, 5f)
}
