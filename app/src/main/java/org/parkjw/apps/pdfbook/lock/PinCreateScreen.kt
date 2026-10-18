package org.parkjw.apps.pdfbook.lock

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import org.parkjw.apps.pdfbook.R

@Composable
fun PinCreateScreen(
    needsPinNote: Boolean,
    onCreated: (CharArray) -> Unit,
    onBack: () -> Unit,
) {
    val session = remember { PinCreateSession() }
    BackHandler {
        if (session.back()) onBack()
    }
    PinPadScreen(
        title = stringResource(if (session.confirming) R.string.pin_confirm_title else R.string.pin_create_title),
        subtitle = stringResource(if (session.mismatch) R.string.pin_mismatch else R.string.pin_create_hint),
        subtitleIsError = session.mismatch,
        warning = if (session.confirming) null else stringResource(R.string.pin_forget_warning),
        note = if (!session.confirming && needsPinNote) stringResource(R.string.app_lock_biometric_needs_pin) else null,
        slotCount = if (session.confirming) session.firstLength else maxOf(4, session.length).coerceAtMost(8),
        filled = session.length,
        inputEnabled = true,
        showLockIcon = false,
        left = if (!session.confirming && session.length in PIN_LENGTH) PinPadLeft.Confirm else PinPadLeft.Blank,
        leftEnabled = !session.confirming && session.length in PIN_LENGTH,
        onDigit = { digit ->
            val created = session.digit(digit)
            if (created != null) onCreated(created)
        },
        onDelete = { session.delete() },
        onLeft = { session.confirmFirst() },
    )
}
