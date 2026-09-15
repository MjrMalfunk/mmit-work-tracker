package com.midwestmanagedit.tracker.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTimeWindowTest {
    @Test
    fun `excludes a cached GPS point before the official start`() {
        assertFalse(ExportTimeWindow.contains(1_000L, startedAt = 1_001L, completedAt = 2_000L))
    }

    @Test
    fun `keeps boundary points and excludes a point after completion`() {
        assertTrue(ExportTimeWindow.contains(1_001L, startedAt = 1_001L, completedAt = 2_000L))
        assertTrue(ExportTimeWindow.contains(2_000L, startedAt = 1_001L, completedAt = 2_000L))
        assertFalse(ExportTimeWindow.contains(2_001L, startedAt = 1_001L, completedAt = 2_000L))
    }
}
