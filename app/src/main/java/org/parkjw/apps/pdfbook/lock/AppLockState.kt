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
    private var exemptStopped = false
    private var exemptStopElapsedMs = 0L
    private var handledPromptNonce = 0
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

    fun cancelSystemReturn() {
        exemptPending = false
        exemptStopped = false
    }

    fun onResume() {
        if (exemptPending && !exemptStopped) cancelSystemReturn()
    }

    fun onStop(isChangingConfigurations: Boolean, nowElapsedMs: Long) {
        cancelPrompt()
        if (exemptPending && !isChangingConfigurations) {
            exemptStopped = true
            exemptStopElapsedMs = nowElapsedMs
        }
        if (shouldLockOnStop(isChangingConfigurations, exemptPending)) lockNow()
    }

    fun onStart(nowElapsedMs: Long) {
        if (!exemptPending || !exemptStopped) return
        val elapsed = (nowElapsedMs - exemptStopElapsedMs).coerceAtLeast(0L)
        cancelSystemReturn()
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

    fun consumePromptRequest(nonce: Int): Boolean {
        if (nonce == 0 || nonce <= handledPromptNonce) return false
        handledPromptNonce = nonce
        return true
    }

    fun nextPromptGeneration(): Int {
        promptGeneration += 1
        return promptGeneration
    }

    fun isPromptCurrent(generation: Int): Boolean = generation == promptGeneration

    fun bindSignal(cancellation: CancellationSignal) {
        val previous = signal
        signal = cancellation
        if (previous != null && previous !== cancellation) previous.cancel()
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
        cancelPrompt()
        cancelSystemReturn()
        unlocked = false
        everUnlocked = false
        preferKeypad = false
        promptConsumed = false
    }

    fun resetForTest() {
        coldLock()
        exemptStopElapsedMs = 0L
        handledPromptNonce = 0
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
