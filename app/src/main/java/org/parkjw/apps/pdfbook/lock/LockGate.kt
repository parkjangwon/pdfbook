package org.parkjw.apps.pdfbook.lock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

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
    Box(Modifier.fillMaxSize()) {
        if (showApp) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (locked) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                app()
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
