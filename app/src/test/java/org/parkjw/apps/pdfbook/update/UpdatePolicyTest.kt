package org.parkjw.apps.pdfbook.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private val v031 = AppVersion(0, 3, 1)

    @Test
    fun autoCheckRunsAtMostOncePerDayAndNeverWhenOff() {
        assertTrue(UpdatePolicy.shouldAutoCheck(enabled = true, lastAutoCheckDay = null, today = 100))
        assertTrue(UpdatePolicy.shouldAutoCheck(enabled = true, lastAutoCheckDay = 99, today = 100))
        assertFalse(UpdatePolicy.shouldAutoCheck(enabled = true, lastAutoCheckDay = 100, today = 100))
        assertTrue(UpdatePolicy.shouldAutoCheck(enabled = true, lastAutoCheckDay = 101, today = 100))
        assertFalse(UpdatePolicy.shouldAutoCheck(enabled = false, lastAutoCheckDay = null, today = 100))
        assertFalse(UpdatePolicy.shouldAutoCheck(enabled = false, lastAutoCheckDay = 1, today = 100))
    }

    @Test
    fun laterHidesSameVersionUntilNextDay() {
        assertTrue(UpdatePolicy.shouldPromptAfterAutoCheck(v031, null, 100))
        assertFalse(UpdatePolicy.shouldPromptAfterAutoCheck(v031, UpdateSnooze("0.3.1", 100), 100))
        assertTrue(UpdatePolicy.shouldPromptAfterAutoCheck(v031, UpdateSnooze("0.3.1", 99), 100))
        assertTrue(UpdatePolicy.shouldPromptAfterAutoCheck(v031, UpdateSnooze("0.3.0", 100), 100))
    }

    @Test
    fun onlyManualChecksSurfaceFailures() {
        assertTrue(UpdatePolicy.showsFailure(manual = true))
        assertFalse(UpdatePolicy.showsFailure(manual = false))
    }
}
