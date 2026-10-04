package com.midwestmanagedit.tracker.tracking

import org.junit.Assert.*
import org.junit.Test

class GpsRecoveryTest {
    @Test fun restartRequestsLocationsWithoutChangingAnOuting() {
        assertTrue(GpsRecoveryPolicy(0).shouldRequest(0, true, true))
    }

    @Test fun healthyResumeDoesNotRegisterAnotherListener() {
        val policy = subscribed()
        policy.savedFix(10_000)
        assertFalse(policy.shouldRequest(80_000, true, true))
        assertEquals(GpsStatus.RECORDING, policy.status(80_000, true, true, true))
    }

    @Test fun stoppedCallbacksTriggerRecoveryAndRemainStaleUntilAFixIsSaved() {
        val policy = subscribed()
        policy.savedFix(10_000)
        assertTrue(policy.shouldRequest(130_001, true, true))
        policy.requestStarted(130_001)
        policy.requestFinished(true)
        assertEquals(GpsStatus.STALE, policy.status(130_002, true, true, true))
        policy.savedFix(135_000)
        assertEquals(GpsStatus.RECORDING, policy.status(135_000, true, true, true))
    }

    @Test fun aRegistrationThatSucceedsWithoutAFixCannotAppearHealthy() {
        val policy = subscribed()
        assertEquals(GpsStatus.WAITING, policy.status(30_000, true, true, true))
        assertEquals(GpsStatus.STALE, policy.status(120_001, true, true, true))
        assertTrue(policy.shouldRequest(120_001, true, true))
    }

    @Test fun failedRequestsAreVisibleAndRetriesAreBounded() {
        val policy = GpsRecoveryPolicy(0)
        policy.requestStarted(0)
        policy.requestFinished(false)
        assertEquals(GpsStatus.REQUEST_FAILED, policy.status(1, true, true, true))
        assertFalse(policy.shouldRequest(59_999, true, true))
        assertTrue(policy.shouldRequest(60_000, true, true))
    }

    @Test fun hangingRegistrationTimesOutAndCanBeRetried() {
        val policy = GpsRecoveryPolicy(0)
        policy.requestStarted(0)
        assertFalse(policy.shouldRequest(60_000, true, true))
        assertFalse(policy.expirePending(29_999))
        assertTrue(policy.expirePending(30_000))
        assertTrue(policy.shouldRequest(60_000, true, true))
    }

    @Test fun manualRetryDoesNotOverlapAnInFlightRequestOrFloodTheProvider() {
        val policy = GpsRecoveryPolicy(0)
        policy.requestStarted(0)
        assertFalse(policy.shouldRequest(10_000, true, true, true))
        policy.requestFinished(false)
        assertFalse(policy.shouldRequest(4_999, true, true, true))
        assertTrue(policy.shouldRequest(5_000, true, true, true))
    }

    @Test fun missingPermissionAndDisabledLocationPreventRetries() {
        val policy = GpsRecoveryPolicy(0)
        assertFalse(policy.shouldRequest(0, false, true, true))
        assertFalse(policy.shouldRequest(0, true, false, true))
        assertEquals(GpsStatus.PERMISSION_REQUIRED, policy.status(0, false, false, true))
        assertEquals(GpsStatus.LOCATION_DISABLED, policy.status(0, true, true, false))
        assertEquals(GpsStatus.APPROXIMATE_ONLY, policy.status(0, true, false, true))
    }

    @Test fun databaseFailureCannotBeReportedAsRecording() {
        val policy = subscribed()
        policy.savedFix(10_000)
        policy.saveFailed()
        assertEquals(GpsStatus.SAVE_FAILED, policy.status(20_000, true, true, true))
        policy.savedFix(21_000)
        assertEquals(GpsStatus.RECORDING, policy.status(21_000, true, true, true))
    }

    @Test fun aFreshSavedFixProvesDeliveryEvenIfARetryTaskFailed() {
        val policy = subscribed()
        policy.savedFix(10_000)
        policy.requestStarted(130_001)
        policy.requestFinished(false)
        policy.savedFix(135_000)
        assertEquals(GpsStatus.RECORDING, policy.status(135_000, true, true, true))
        assertFalse(policy.shouldRequest(140_000, true, true))
        assertTrue(policy.shouldRequest(190_001, true, true))
    }

    @Test fun serviceDeathIsDetectedEvenIfItsLastFixWasHealthy() {
        val health = GpsHealth(status = GpsStatus.RECORDING, checkedAtElapsedMs = 1_000, lastSavedAtElapsedMs = 1_000)
        assertEquals(GpsStatus.RECORDING, health.effectiveStatus(80_000))
        assertEquals(GpsStatus.SERVICE_UNAVAILABLE, health.effectiveStatus(91_001))
        assertEquals(GpsStatus.STOPPED, GpsHealth().effectiveStatus(1_000_000))
    }

    @Test fun cachedWeakAndInvalidFixesAreRejected() {
        assertTrue(GpsRecoveryPolicy.usableFix(130_000, 129_000, 7.8f, 41.0, -85.0))
        assertFalse(GpsRecoveryPolicy.usableFix(130_000, 9_999, 7.8f, 41.0, -85.0))
        assertFalse(GpsRecoveryPolicy.usableFix(130_000, 130_001, 7.8f, 41.0, -85.0))
        assertFalse(GpsRecoveryPolicy.usableFix(130_000, 129_000, 101f, 41.0, -85.0))
        assertFalse(GpsRecoveryPolicy.usableFix(130_000, 129_000, Float.NaN, 41.0, -85.0))
        assertFalse(GpsRecoveryPolicy.usableFix(130_000, 129_000, 1f, Double.NaN, -85.0))
    }

    private fun subscribed() = GpsRecoveryPolicy(0).apply {
        requestStarted(0)
        requestFinished(true)
    }
}
