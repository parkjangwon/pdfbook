package org.parkjw.apps.pdfbook.lock

import android.os.CancellationSignal
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class AppLockStateTest {
    private val state = AppLockState()

    @Test
    fun startsLockedAndUnlockMarksEverUnlocked() {
        assertFalse(state.unlocked)
        assertFalse(state.everUnlocked)
        state.unlock()
        assertTrue(state.unlocked)
        assertTrue(state.everUnlocked)
    }

    @Test
    fun backgroundingLocksAndBumpsEpochButKeepsEverUnlocked() {
        state.unlock()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 0L)
        assertFalse(state.unlocked)
        assertTrue(state.everUnlocked)
        assertEquals(1, state.lockEpoch)
    }

    @Test
    fun configChangeStopDoesNotBumpEpoch() {
        state.unlock()
        state.onStop(isChangingConfigurations = true, nowElapsedMs = 0L)
        assertEquals(0, state.lockEpoch)
    }

    @Test
    fun onStartWithoutExemptStopNeverLocks() {
        state.unlock()
        state.onStart(Long.MAX_VALUE)
        assertTrue(state.unlocked)
        state.expectSystemReturn()
        state.onStart(Long.MAX_VALUE)
        assertTrue(state.unlocked)
        assertTrue(state.pendingExempt())
    }

    @Test
    fun clockGoingBackwardsDuringExemptReturnDoesNotLock() {
        state.unlock()
        state.expectSystemReturn()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 5_000L)
        state.onStart(1_000L)
        assertTrue(state.unlocked)
    }

    @Test
    fun resumeAfterExemptStopKeepsExemptForOnStart() {
        state.unlock()
        state.expectSystemReturn()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 0L)
        state.onResume()
        assertTrue(state.pendingExempt())
    }

    @Test
    fun lateExemptReturnBumpsEpochAndClearsExempt() {
        state.unlock()
        state.expectSystemReturn()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 0L)
        state.onStart(SYSTEM_RETURN_GRACE_MS + 1)
        assertFalse(state.unlocked)
        assertFalse(state.pendingExempt())
        assertEquals(1, state.lockEpoch)
    }

    @Test
    fun autoPromptOnlyForBiometricWithCapabilityAndNoKeypadOrConsumption() {
        assertTrue(state.shouldAutoPrompt(AppLockMode.Biometric, true))
        assertFalse(state.shouldAutoPrompt(AppLockMode.Pin, true))
        assertFalse(state.shouldAutoPrompt(AppLockMode.Biometric, false))
        state.showKeypad()
        assertFalse(state.shouldAutoPrompt(AppLockMode.Biometric, true))
        state.requestPrompt()
        assertTrue(state.shouldAutoPrompt(AppLockMode.Biometric, true))
        state.markPromptConsumed()
        assertFalse(state.shouldAutoPrompt(AppLockMode.Biometric, true))
    }

    @Test
    fun lockingResetsKeypadPreferenceAndPromptConsumption() {
        state.unlock()
        state.showKeypad()
        state.markPromptConsumed()
        state.onStop(isChangingConfigurations = false, nowElapsedMs = 0L)
        assertFalse(state.preferKeypad)
        assertTrue(state.shouldAutoPrompt(AppLockMode.Biometric, true))
    }

    @Test
    fun promptRequestNonceRules() {
        assertFalse(state.consumePromptRequest(0))
        state.requestPrompt()
        state.requestPrompt()
        assertEquals(2, state.promptNonce)
        assertTrue(state.consumePromptRequest(2))
        assertFalse(state.consumePromptRequest(1))
        assertFalse(state.consumePromptRequest(2))
    }

    @Test
    fun promptGenerationInvalidatesOlderOnes() {
        val first = state.nextPromptGeneration()
        assertTrue(state.isPromptCurrent(first))
        val second = state.nextPromptGeneration()
        assertFalse(state.isPromptCurrent(first))
        assertTrue(state.isPromptCurrent(second))
        state.cancelPrompt()
        assertFalse(state.isPromptCurrent(second))
    }

    @Test
    fun bindingNewSignalCancelsPreviousOne() {
        val a = CancellationSignal()
        val b = CancellationSignal()
        state.bindSignal(a)
        state.bindSignal(a)
        assertFalse(a.isCanceled)
        state.bindSignal(b)
        assertTrue(a.isCanceled)
        assertFalse(b.isCanceled)
    }

    @Test
    fun cancelIfCurrentOnlyCancelsBoundSignal() {
        val a = CancellationSignal()
        val b = CancellationSignal()
        state.bindSignal(a)
        state.cancelIfCurrent(b)
        assertFalse(a.isCanceled)
        state.cancelIfCurrent(a)
        assertTrue(a.isCanceled)
    }

    @Test
    fun stopCancelsBoundPrompt() {
        val signal = CancellationSignal()
        state.bindSignal(signal)
        state.onStop(isChangingConfigurations = true, nowElapsedMs = 0L)
        assertTrue(signal.isCanceled)
    }

    @Test
    fun coldLockClearsUnlockStateAndExempt() {
        state.unlock()
        state.expectSystemReturn()
        state.coldLock()
        assertFalse(state.unlocked)
        assertFalse(state.everUnlocked)
        assertFalse(state.pendingExempt())
    }
}
