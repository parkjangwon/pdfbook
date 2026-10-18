package org.parkjw.apps.pdfbook.lock

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlin.math.ceil
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.R

class PinBuffer {
    private val chars = CharArray(8)
    var length by mutableIntStateOf(0)
        private set

    fun push(digit: Char) {
        if (length >= chars.size || digit !in '0'..'9') return
        chars[length] = digit
        length += 1
    }

    fun pop() {
        if (length == 0) return
        length -= 1
        chars[length] = '\u0000'
    }

    fun clear() {
        chars.fill('\u0000')
        length = 0
    }

    fun copyOut(): CharArray = chars.copyOf(length)
}

@Composable
fun LockScreen(
    config: LockConfig,
    session: AppLockState,
    canAuthenticate: Boolean,
    verify: suspend (CharArray, Long) -> PinVerify,
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    now: () -> Long = { System.currentTimeMillis() },
) {
    androidx.activity.compose.BackHandler { onBack() }
    val mode = config.mode
    val activity = LocalActivity.current as? ComponentActivity
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val usePin = stringResource(R.string.lock_use_pin)
    val epoch = session.lockEpoch
    val nonce = session.promptNonce
    LaunchedEffect(epoch) {
        if (activity == null || !session.shouldAutoPrompt(mode, canAuthenticate)) return@LaunchedEffect
        session.markPromptConsumed()
        awaitBiometricPrompt(activity, session, promptTitle, usePin, onUnlocked) { session.showKeypad() }
    }
    LaunchedEffect(nonce) {
        if (activity == null || !session.consumePromptRequest(nonce)) return@LaunchedEffect
        awaitBiometricPrompt(activity, session, promptTitle, usePin, onUnlocked) { session.showKeypad() }
    }
    PinEntry(
        title = stringResource(R.string.lock_title),
        idleSubtitle = stringResource(R.string.lock_enter_pin),
        pinLength = config.pinLength,
        failedAttempts = config.failedAttempts,
        lockoutUntilEpochMs = config.lockoutUntilEpochMs,
        showLockIcon = true,
        showFingerprint = mode == AppLockMode.Biometric && canAuthenticate,
        onFingerprint = { session.requestPrompt() },
        verify = verify,
        onSuccess = onUnlocked,
        now = now,
    )
}

@Composable
fun ConfirmPinScreen(
    config: LockConfig,
    session: AppLockState,
    canAuthenticate: Boolean,
    forceKeypad: Boolean,
    verify: suspend (CharArray, Long) -> PinVerify,
    onSuccess: () -> Unit,
    onUsePin: () -> Unit,
    onRequestBiometric: () -> Unit = onUsePin,
    now: () -> Long = { System.currentTimeMillis() },
) {
    val prompt = config.mode == AppLockMode.Biometric && canAuthenticate && !forceKeypad
    val activity = LocalActivity.current as? ComponentActivity
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val usePin = stringResource(R.string.lock_use_pin)
    LaunchedEffect(prompt) {
        if (!prompt || activity == null) return@LaunchedEffect
        awaitBiometricPrompt(activity, session, promptTitle, usePin, onSuccess, onUsePin)
    }
    if (!prompt) {
        PinEntry(
            title = stringResource(R.string.confirm_identity),
            idleSubtitle = stringResource(R.string.lock_enter_pin),
            pinLength = config.pinLength,
            failedAttempts = config.failedAttempts,
            lockoutUntilEpochMs = config.lockoutUntilEpochMs,
            showLockIcon = true,
            showFingerprint = config.mode == AppLockMode.Biometric && canAuthenticate,
            onFingerprint = onRequestBiometric,
            verify = verify,
            onSuccess = onSuccess,
            now = now,
        )
    }
}

@Composable
private fun PinEntry(
    title: String,
    idleSubtitle: String,
    pinLength: Int,
    failedAttempts: Int,
    lockoutUntilEpochMs: Long,
    showLockIcon: Boolean,
    showFingerprint: Boolean,
    onFingerprint: () -> Unit,
    verify: suspend (CharArray, Long) -> PinVerify,
    onSuccess: () -> Unit,
    now: () -> Long,
) {
    val buffer = remember { PinBuffer() }
    val scope = rememberCoroutineScope()
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var clock by remember { mutableLongStateOf(now()) }
    val remaining = remainingLockoutMs(clock, lockoutUntilEpochMs, failedAttempts)
    androidx.compose.runtime.LaunchedEffect(lockoutUntilEpochMs, failedAttempts) {
        while (remainingLockoutMs(now(), lockoutUntilEpochMs, failedAttempts) > 0L) {
            clock = now()
            kotlinx.coroutines.delay(200)
        }
        clock = now()
    }
    val waiting = remaining > 0L
    val seconds = ceil(remaining / 1000.0).toInt().coerceAtLeast(if (waiting) 1 else 0)
    val subtitle = when {
        waiting -> stringResource(R.string.lock_wait, seconds)
        wrong -> stringResource(R.string.lock_wrong_pin)
        else -> idleSubtitle
    }
    fun submit() {
        if (busy || waiting || buffer.length != pinLength || pinLength !in PIN_LENGTH) return
        val pin = buffer.copyOut()
        buffer.clear()
        busy = true
        scope.launch {
            try {
                when (verify(pin, now())) {
                    PinVerify.Success -> onSuccess()
                    PinVerify.Wrong -> {
                        wrong = true
                        clock = now()
                    }
                    is PinVerify.Waiting -> {
                        wrong = false
                        clock = now()
                    }
                }
            } finally {
                pin.fill('\u0000')
                busy = false
            }
        }
    }
    PinPadScreen(
        title = title,
        subtitle = subtitle,
        subtitleIsError = waiting || wrong,
        warning = null,
        note = null,
        slotCount = buffer.length,
        filled = buffer.length,
        inputEnabled = !waiting && !busy,
        showLockIcon = showLockIcon,
        left = if (showFingerprint) PinPadLeft.Fingerprint else PinPadLeft.Blank,
        leftEnabled = showFingerprint,
        onDigit = { digit ->
            if (waiting || busy || buffer.length >= pinLength) return@PinPadScreen
            wrong = false
            buffer.push(digit)
            if (buffer.length == pinLength) submit()
        },
        onDelete = {
            if (waiting || busy) return@PinPadScreen
            buffer.pop()
        },
        onLeft = onFingerprint,
    )
}
