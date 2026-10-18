package org.parkjw.apps.pdfbook.lock

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import org.parkjw.apps.pdfbook.R

@Composable
fun PinCreateScreen(
    onCreated: (CharArray) -> Unit,
    onBack: () -> Unit,
) {
    val session = remember { PinCreateSession() }
    BackHandler {
        if (session.back()) onBack()
    }
    val confirming = session.confirming
    PinPadScreen(
        title = stringResource(if (confirming) R.string.pin_confirm_title else R.string.pin_create_title),
        subtitle = when {
            session.mismatch -> stringResource(R.string.pin_mismatch)
            confirming -> ""
            else -> stringResource(R.string.pin_create_hint)
        },
        subtitleIsError = session.mismatch,
        warning = if (confirming) null else stringResource(R.string.pin_forget_warning),
        note = null,
        slotCount = session.length,
        filled = session.length,
        inputEnabled = true,
        showLockIcon = true,
        left = PinPadLeft.Blank,
        leftEnabled = false,
        onDigit = { digit ->
            val created = session.digit(digit)
            if (created != null) onCreated(created)
        },
        onDelete = { session.delete() },
        onLeft = {},
        actionLabel = if (confirming) null else stringResource(R.string.pin_next),
        actionEnabled = !confirming && session.length in PIN_LENGTH,
        onAction = if (confirming) {
            null
        } else {
            { session.confirmFirst() }
        },
    )
}
