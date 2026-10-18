package org.parkjw.apps.pdfbook.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.lock.AppLockMode
import org.parkjw.apps.pdfbook.lock.SecurityValueRow

@Composable
internal fun SecuritySettingsSection(
    appLockMode: AppLockMode = AppLockMode.None,
    onOpenAppLock: () -> Unit = {},
) {
    SettingsSection(stringResource(R.string.settings_section_security))
    SettingsGroup {
        SecurityValueRow(
            label = stringResource(R.string.app_lock),
            value = stringResource(appLockModeLabel(appLockMode)),
            onClick = onOpenAppLock,
        )
    }
}

internal fun appLockModeLabel(mode: AppLockMode): Int = when (mode) {
    AppLockMode.None -> R.string.app_lock_none
    AppLockMode.Pin -> R.string.app_lock_pin
    AppLockMode.Biometric -> R.string.app_lock_biometric
}
