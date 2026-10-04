package com.midwestmanagedit.tracker.domain

import org.junit.Assert.*
import org.junit.Test

class OfflineCompletionTest {
    @Test fun enteredReadingIsIncludedWithoutSeparateSave() {
        val offline = OutingMileage.offlineForCompletion(85943.0, null, 86217.0, "86202")
        val split = OutingMileage.split(85943.0, offline, 86217.0)
        assertEquals(86202.0, offline!!, 0.0)
        assertEquals(259.0, split.online!!, 0.0)
        assertEquals(15.0, split.offline!!, 0.0)
        assertEquals(274.0, split.total!!, 0.0)
    }

    @Test fun blankFieldPreservesAnAlreadySavedReading() {
        for (input in listOf(null, "", "   ")) {
            assertEquals(86202.0, OutingMileage.offlineForCompletion(85943.0, 86202.0, 86217.0, input)!!, 0.0)
        }
    }

    @Test fun missingOptionalReadingKeepsTheSplitUnknown() {
        val offline = OutingMileage.offlineForCompletion(85943.0, null, 86217.0, "")
        assertNull(offline)
        assertNull(OutingMileage.split(85943.0, offline, 86217.0).offline)
    }

    @Test fun editedReadingSupersedesTheSavedValue() {
        assertEquals(86202.0, OutingMileage.offlineForCompletion(85943.0, 86200.0, 86217.0, " 86202 ")!!, 0.0)
    }

    @Test fun malformedInputCannotSilentlyUseTheSavedValue() {
        for (input in listOf("oops", "NaN", "Infinity", "86202x")) {
            assertThrows(IllegalArgumentException::class.java) {
                OutingMileage.offlineForCompletion(85943.0, 86200.0, 86217.0, input)
            }
        }
    }

    @Test fun offlineReadingMustFitBothOdometerBoundaries() {
        for (input in listOf("85942", "86218", "10000000")) {
            assertThrows(IllegalArgumentException::class.java) {
                OutingMileage.offlineForCompletion(85943.0, null, 86217.0, input)
            }
        }
        assertEquals(85943.0, OutingMileage.offlineForCompletion(85943.0, null, 86217.0, "85943")!!, 0.0)
        assertEquals(86217.0, OutingMileage.offlineForCompletion(85943.0, null, 86217.0, "86217")!!, 0.0)
    }

    @Test fun invalidFinalReadingCannotCompleteTheOuting() {
        for (end in listOf(86201.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                OutingMileage.offlineForCompletion(85943.0, null, end, "86202")
            }
        }
    }
}
