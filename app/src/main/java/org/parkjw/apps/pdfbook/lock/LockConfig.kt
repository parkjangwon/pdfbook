package org.parkjw.apps.pdfbook.lock

data class LockConfig(
    val mode: AppLockMode = AppLockMode.None,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val pinIterations: Int = PinHasher.ITERATIONS,
    val pinLength: Int = 0,
    val hideInRecents: Boolean = true,
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMs: Long = 0L,
) {
    val hasPin: Boolean
        get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank() && pinLength in PIN_LENGTH
}

internal val PIN_LENGTH = 4..8
