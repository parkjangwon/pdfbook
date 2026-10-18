package org.parkjw.apps.pdfbook.lock

import android.os.CancellationSignal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.parkjw.apps.pdfbook.hidden.HiddenPeek

class AppLockState {
    var unlocked by mutableStateOf(false)
        private set
    var everUnlocked by mutableStateOf(false)
        private set
    var preferKeypad by mutableStateOf(false)
        private set
    var lockEpoch by mutableIntStateOf(0)
        private set
    var promptNonce by mutableIntStateOf(0)
        private set

    private var exemptPending = false
    private var exemptStopElapsedMs = 0L
    private var promptConsumed = false
    private var promptGeneration = 0
    private var signal: CancellationSignal? = null

    fun unlock() {
        unlocked = true
        everUnlocked = true
    }

    fun expectSystemReturn() {
        exemptPending = true
    }

    fun onStop(isChangingConfigurations: Boolean, nowElapsedMs: Long) {
        cancelPrompt()
        if (exemptPending) exemptStopElapsedMs = nowElapsedMs
        if (shouldLockOnStop(isChangingConfigurations, exemptPending)) lockNow()
    }

    fun onStart(nowElapsedMs: Long) {
        if (!exemptPending) return
        val elapsed = (nowElapsedMs - exemptStopElapsedMs).coerceAtLeast(0L)
        exemptPending = false
        if (shouldLockOnReturn(elapsed)) lockNow()
    }

    fun shouldAutoPrompt(mode: AppLockMode, canAuthenticate: Boolean): Boolean =
        mode == AppLockMode.Biometric && canAuthenticate && !preferKeypad && !promptConsumed

    fun markPromptConsumed() {
        promptConsumed = true
    }

    fun showKeypad() {
        preferKeypad = true
    }

    fun requestPrompt() {
        preferKeypad = false
        promptNonce++
    }

    fun nextPromptGeneration(): Int {
        promptGeneration += 1
        return promptGeneration
    }

    fun isPromptCurrent(generation: Int): Boolean = generation == promptGeneration

    fun bindSignal(cancellation: CancellationSignal) {
        signal = cancellation
    }

    fun cancelPrompt() {
        promptGeneration += 1
        val current = signal
        signal = null
        current?.cancel()
    }

    fun cancelIfCurrent(cancellation: CancellationSignal) {
        if (signal !== cancellation) return
        cancelPrompt()
    }

    fun pendingExempt(): Boolean = exemptPending

    internal fun coldLock() {
        HiddenPeek.reset()
        unlocked = false
        everUnlocked = false
        preferKeypad = false
        promptConsumed = false
    }

    fun resetForTest() {
        coldLock()
        exemptPending = false
        exemptStopElapsedMs = 0L
        promptGeneration = 0
        lockEpoch = 0
        promptNonce = 0
        signal = null
    }

    private fun lockNow() {
        HiddenPeek.reset()
        unlocked = false
        preferKeypad = false
        promptConsumed = false
        lockEpoch++
    }
}

object AppLockProcess {
    val state = AppLockState()
}
