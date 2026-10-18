package org.parkjw.apps.pdfbook.lock

import android.content.Intent
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    @Test
    fun l1_defaultConfigIsUnlockedMode() {
        assertEquals(AppLockMode.None, LockConfig().mode)
        assertFalse(LockConfig().hasPin)
    }

    @Test
    fun l5_canSelectBiometricRequiresPin() {
        assertFalse(canSelectBiometric(false))
        assertTrue(canSelectBiometric(true))
    }

    @Test
    fun l7_lockoutMillisForDoublesAfterFiveAndCapsAtOneHour() {
        assertEquals(0L, lockoutMillisFor(1))
        assertEquals(0L, lockoutMillisFor(4))
        assertEquals(30_000L, lockoutMillisFor(5))
        assertEquals(60_000L, lockoutMillisFor(6))
        assertEquals(120_000L, lockoutMillisFor(7))
        assertEquals(3_600_000L, lockoutMillisFor(20))
        assertEquals(3_600_000L, lockoutMillisFor(40))
    }

    @Test
    fun l7_clockSkewCannotExtendLockoutPastPolicy() {
        val cap = lockoutMillisFor(5)
        val until = 1_000_000L
        assertEquals(cap, remainingLockoutMs(0L, until, 5))
        assertEquals(0L, remainingLockoutMs(until, until, 5))
        assertEquals(1_000L, remainingLockoutMs(until - 1_000L, until, 5))
    }

    @Test
    fun l2_l3_shouldLockOnStopSkipsConfigChangeAndExempt() {
        assertFalse(shouldLockOnStop(isChangingConfigurations = true, exemptPending = false))
        assertFalse(shouldLockOnStop(isChangingConfigurations = false, exemptPending = true))
        assertTrue(shouldLockOnStop(isChangingConfigurations = false, exemptPending = false))
    }

    @Test
    fun l3_shouldLockOnReturnAfterTenMinutes() {
        assertFalse(shouldLockOnReturn(SYSTEM_RETURN_GRACE_MS))
        assertTrue(shouldLockOnReturn(SYSTEM_RETURN_GRACE_MS + 1))
    }

    @Test
    fun l3_isExemptSystemIntentAllowsDocumentAndInstallOnly() {
        assertTrue(isExemptSystemIntent(Intent.ACTION_OPEN_DOCUMENT_TREE, null))
        assertTrue(isExemptSystemIntent(Intent.ACTION_OPEN_DOCUMENT, "application/pdf"))
        assertTrue(isExemptSystemIntent(Intent.ACTION_INSTALL_PACKAGE, null))
        assertTrue(isExemptSystemIntent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, null))
        assertTrue(isExemptSystemIntent(Intent.ACTION_VIEW, "application/vnd.android.package-archive"))
        assertFalse(isExemptSystemIntent(Intent.ACTION_VIEW, null))
        assertFalse(isExemptSystemIntent(Intent.ACTION_VIEW, "text/html"))
        assertFalse(isExemptSystemIntent(Intent.ACTION_VIEW, "https"))
    }

    @Test
    fun l3_exemptReturnInsideGraceStaysUnlockedAndLateReturnLocks() {
        val state = AppLockState()
        state.unlock()
        state.expectSystemReturn()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 0L)
        assertTrue(state.unlocked)
        state.onStart(SYSTEM_RETURN_GRACE_MS)
        assertTrue(state.unlocked)
        state.expectSystemReturn()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 0L)
        state.onStart(SYSTEM_RETURN_GRACE_MS + 1)
        assertFalse(state.unlocked)
    }

    @Test
    fun l3_configChangeDoesNotLock() {
        val state = AppLockState()
        state.unlock()
        state.onStop(isChangingConfigurations = true, nowElapsedMs = 0L)
        assertTrue(state.unlocked)
        state.onStart(1_000L)
        assertTrue(state.unlocked)
    }

    @Test
    fun l9_recentsScreenshotEnabledFor() {
        val pin = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 4, hideInRecents = true)
        assertFalse(recentsScreenshotEnabledFor(pin))
        assertTrue(recentsScreenshotEnabledFor(pin.copy(hideInRecents = false)))
        assertTrue(recentsScreenshotEnabledFor(pin.copy(mode = AppLockMode.None)))
        var enabled = true
        applyRecentsScreenshot(pin) { enabled = it }
        assertFalse(enabled)
    }

    @Test
    fun l4_usePinShowsKeypad() {
        val start = initialLockInteraction(AppLockMode.Biometric, canAuthenticate = true, preferKeypad = false)
        assertTrue(start.prompt)
        assertFalse(start.keypad)
        val next = onLockUsePin(start)
        assertFalse(next.prompt)
        assertTrue(next.keypad)
        assertTrue(initialLockInteraction(AppLockMode.Pin, true, false).keypad)
    }

    @Test
    fun l8_changesAskForConfirmBeforeApply() {
        val pin = LockConfig(mode = AppLockMode.Pin, pinHash = "h", pinSalt = "s", pinLength = 4)
        assertTrue(phaseFor(LockEdit.None, pin, canAuthenticate = true) is LockEditPhase.Confirm)
        assertTrue(phaseFor(LockEdit.ChangePin, pin, canAuthenticate = true) is LockEditPhase.Confirm)
        assertTrue(phaseFor(LockEdit.HideOff, pin, canAuthenticate = true) is LockEditPhase.Confirm)
        val biometric = phaseFor(LockEdit.Biometric, pin, canAuthenticate = true)
        assertEquals(LockEditPhase.Confirm(LockEdit.Biometric, keypad = false), biometric)
        assertEquals(LockEditPhase.Menu, phaseAfterConfirmed(LockEdit.None, hasPin = true))
        assertEquals(LockEditPhase.Create(LockEdit.ChangePin), phaseAfterConfirmed(LockEdit.ChangePin, hasPin = true))
        assertEquals(
            LockEditPhase.Create(LockEdit.Biometric, needsPinNote = true),
            phaseFor(LockEdit.Biometric, LockConfig(), canAuthenticate = true),
        )
    }
}
