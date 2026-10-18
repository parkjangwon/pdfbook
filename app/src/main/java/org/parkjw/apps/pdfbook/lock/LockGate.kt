package org.parkjw.apps.pdfbook.lock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

@Composable
fun LockGate(
    config: LockConfig?,
    session: AppLockState,
    canAuthenticate: Boolean,
    verify: suspend (CharArray, Long) -> PinVerify,
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    onBlockedChange: (Boolean) -> Unit,
    app: @Composable () -> Unit,
) {
    val unlocked = session.unlocked
    val everUnlocked = session.everUnlocked
    val locking = config != null && config.mode != AppLockMode.None
    val locked = locking && !unlocked
    SideEffect { onBlockedChange(config == null || locked) }
    if (config == null) return
    val showApp = !locking || everUnlocked
    val saveable = rememberSaveableStateHolder()
    Box(Modifier.fillMaxSize()) {
        if (showApp && !locked) {
            Box(Modifier.fillMaxSize()) {
                saveable.SaveableStateProvider(APP_STATE_KEY) { app() }
            }
        }
        if (locked) {
            LockScreen(
                config = config,
                session = session,
                canAuthenticate = canAuthenticate,
                verify = verify,
                onUnlocked = onUnlocked,
                onBack = onBack,
            )
        }
    }
}

private const val APP_STATE_KEY = "app"
