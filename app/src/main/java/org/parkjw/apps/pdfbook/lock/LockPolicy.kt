package org.parkjw.apps.pdfbook.lock

import android.content.Intent
import android.provider.Settings

const val SYSTEM_RETURN_GRACE_MS = 10 * 60 * 1000L
private const val LOCKOUT_BASE_MS = 30_000L
private const val LOCKOUT_CAP_MS = 3_600_000L

fun isValidPin(pin: CharArray): Boolean = pin.size in PIN_LENGTH && pin.all { it in '0'..'9' }

fun lockoutMillisFor(failedAttempts: Int): Long {
    if (failedAttempts < 5) return 0L
    var wait = LOCKOUT_BASE_MS
    repeat(failedAttempts - 5) {
        if (wait >= LOCKOUT_CAP_MS) return LOCKOUT_CAP_MS
        wait = minOf(wait * 2, LOCKOUT_CAP_MS)
    }
    return wait
}

fun remainingLockoutMs(nowEpochMs: Long, untilEpochMs: Long, failedAttempts: Int): Long {
    val scheduled = (untilEpochMs - nowEpochMs).coerceAtLeast(0L)
    return minOf(scheduled, lockoutMillisFor(failedAttempts))
}

fun canSelectBiometric(hasPin: Boolean): Boolean = hasPin

fun shouldLockOnStop(isChangingConfigurations: Boolean, exemptPending: Boolean): Boolean =
    !isChangingConfigurations && !exemptPending

fun shouldLockOnReturn(elapsedMs: Long): Boolean = elapsedMs > SYSTEM_RETURN_GRACE_MS

fun isExemptSystemIntent(action: String?, type: String?): Boolean = when (action) {
    Intent.ACTION_OPEN_DOCUMENT_TREE,
    Intent.ACTION_OPEN_DOCUMENT,
    Intent.ACTION_INSTALL_PACKAGE,
    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
    -> true
    Intent.ACTION_VIEW -> type == "application/vnd.android.package-archive"
    else -> false
}

fun recentsScreenshotEnabledFor(config: LockConfig): Boolean =
    config.mode == AppLockMode.None || !config.hideInRecents

fun applyRecentsScreenshot(config: LockConfig, setEnabled: (Boolean) -> Unit) {
    setEnabled(recentsScreenshotEnabledFor(config))
}

data class LockInteraction(val prompt: Boolean, val keypad: Boolean)

fun initialLockInteraction(
    mode: AppLockMode,
    canAuthenticate: Boolean,
    preferKeypad: Boolean,
): LockInteraction {
    val prompt = mode == AppLockMode.Biometric && canAuthenticate && !preferKeypad
    return LockInteraction(prompt = prompt, keypad = !prompt)
}

fun onLockUsePin(state: LockInteraction): LockInteraction = LockInteraction(prompt = false, keypad = true)

enum class LockEdit {
    None,
    Pin,
    Biometric,
    ChangePin,
    HideOff,
}

sealed interface LockEditPhase {
    data object Menu : LockEditPhase

    data class Confirm(val edit: LockEdit, val keypad: Boolean) : LockEditPhase

    data class Create(val edit: LockEdit, val needsPinNote: Boolean = false) : LockEditPhase
}

fun phaseFor(edit: LockEdit, config: LockConfig, canAuthenticate: Boolean): LockEditPhase {
    val keypad = config.mode != AppLockMode.Biometric || !canAuthenticate
    return when (edit) {
        LockEdit.None -> if (config.mode == AppLockMode.None) LockEditPhase.Menu else LockEditPhase.Confirm(edit, keypad)
        LockEdit.Pin -> when {
            config.mode == AppLockMode.Pin -> LockEditPhase.Menu
            !config.hasPin -> LockEditPhase.Create(edit)
            else -> LockEditPhase.Confirm(edit, keypad)
        }
        LockEdit.Biometric -> when {
            !canSelectBiometric(config.hasPin) -> LockEditPhase.Create(edit, needsPinNote = true)
            config.mode == AppLockMode.Biometric -> LockEditPhase.Menu
            else -> LockEditPhase.Confirm(edit, keypad = !canAuthenticate)
        }
        LockEdit.ChangePin -> when {
            !config.hasPin -> LockEditPhase.Menu
            else -> LockEditPhase.Confirm(edit, keypad)
        }
        LockEdit.HideOff -> if (config.mode == AppLockMode.None) LockEditPhase.Menu else LockEditPhase.Confirm(edit, keypad)
    }
}

fun phaseAfterConfirmed(edit: LockEdit, hasPin: Boolean): LockEditPhase = when (edit) {
    LockEdit.ChangePin -> LockEditPhase.Create(edit)
    LockEdit.Pin -> if (hasPin) LockEditPhase.Menu else LockEditPhase.Create(edit)
    LockEdit.Biometric -> if (hasPin) LockEditPhase.Menu else LockEditPhase.Create(edit, needsPinNote = true)
    LockEdit.None, LockEdit.HideOff -> LockEditPhase.Menu
}

fun modeForNewPin(edit: LockEdit, current: AppLockMode): AppLockMode = when (edit) {
    LockEdit.Biometric -> AppLockMode.Biometric
    LockEdit.ChangePin -> if (current == AppLockMode.None) AppLockMode.Pin else current
    else -> AppLockMode.Pin
}
