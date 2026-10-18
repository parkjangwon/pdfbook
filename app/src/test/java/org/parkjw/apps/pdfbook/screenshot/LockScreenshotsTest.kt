package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.lock.AppLockMode
import org.parkjw.apps.pdfbook.lock.AppLockSettingsMenu
import org.parkjw.apps.pdfbook.lock.LockConfig
import org.parkjw.apps.pdfbook.lock.LockEdit
import org.parkjw.apps.pdfbook.lock.PinPadLeft
import org.parkjw.apps.pdfbook.lock.PinPadScreen
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class LockScreenshotsTest {
    @Before
    fun onlyWhenRecordingOrVerifying() {
        assumeTrue(
            System.getProperty("roborazzi.test.record") == "true" ||
                System.getProperty("roborazzi.test.verify") == "true" ||
                System.getProperty("roborazzi.test.compare") == "true",
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun lockLightFolded() = captureLock(
        dark = false,
        file = "lock-light-folded.png",
        filled = 2,
        wrong = false,
        wait = false,
        biometric = false,
    )

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun lockDarkFolded() = captureLock(
        dark = true,
        file = "lock-dark-folded.png",
        filled = 2,
        wrong = false,
        wait = false,
        biometric = false,
    )

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun lockLightUnfolded() = captureLock(
        dark = false,
        file = "lock-light-unfolded.png",
        filled = 2,
        wrong = false,
        wait = false,
        biometric = false,
    )

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun lockDarkUnfolded() = captureLock(
        dark = true,
        file = "lock-dark-unfolded.png",
        filled = 2,
        wrong = false,
        wait = false,
        biometric = false,
    )

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun lockWrongDarkFolded() = captureLock(
        dark = true,
        file = "lock-wrong-dark-folded.png",
        filled = 0,
        wrong = true,
        wait = false,
        biometric = false,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun lockWaitLightFolded() = captureLock(
        dark = false,
        file = "lock-wait-light-folded.png",
        filled = 0,
        wrong = false,
        wait = true,
        biometric = false,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun lockBiometricLightFolded() = captureLock(
        dark = false,
        file = "lock-biometric-light-folded.png",
        filled = 0,
        wrong = false,
        wait = false,
        biometric = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun securityNoneLightFolded() = captureSettings(
        dark = false,
        file = "security-none-light-folded.png",
        config = LockConfig(),
        biometricAvailable = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun securityNoneDarkFolded() = captureSettings(
        dark = true,
        file = "security-none-dark-folded.png",
        config = LockConfig(),
        biometricAvailable = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun settingsSecurityLightFolded() = captureSettingsEntry(
        dark = false,
        file = "settings-security-light-folded.png",
        mode = AppLockMode.Biometric,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun settingsSecurityDarkFolded() = captureSettingsEntry(
        dark = true,
        file = "settings-security-dark-folded.png",
        mode = AppLockMode.None,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun securityPinLightFolded() = captureSettings(
        dark = false,
        file = "security-pin-light-folded.png",
        config = LockConfig(
            mode = AppLockMode.Pin,
            pinHash = "hash",
            pinSalt = "salt",
            pinLength = 4,
            hideInRecents = true,
        ),
        biometricAvailable = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun securityBiometricLightFolded() = captureSettings(
        dark = false,
        file = "security-biometric-light-folded.png",
        config = LockConfig(
            mode = AppLockMode.Biometric,
            pinHash = "hash",
            pinSalt = "salt",
            pinLength = 4,
            hideInRecents = true,
        ),
        biometricAvailable = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pinCreateLightFolded() = capturePinPad(
        dark = false,
        file = "pin-create-light-folded.png",
        title = R.string.pin_create_title,
        subtitle = R.string.pin_create_hint,
        warning = R.string.pin_forget_warning,
        slotCount = 3,
        filled = 3,
        actionEnabled = false,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pinCreateNextLightFolded() = capturePinPad(
        dark = false,
        file = "pin-create-next-light-folded.png",
        title = R.string.pin_create_title,
        subtitle = R.string.pin_create_hint,
        warning = R.string.pin_forget_warning,
        slotCount = 6,
        filled = 6,
        actionEnabled = true,
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pinConfirmLightFolded() = capturePinPad(
        dark = false,
        file = "pin-confirm-light-folded.png",
        title = R.string.pin_confirm_title,
        subtitle = null,
        warning = null,
        slotCount = 2,
        filled = 2,
        actionEnabled = false,
        showAction = false,
    )

    private fun captureLock(
        dark: Boolean,
        file: String,
        filled: Int,
        wrong: Boolean,
        wait: Boolean,
        biometric: Boolean,
    ) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                val subtitle = when {
                    wait -> androidx.compose.ui.res.stringResource(R.string.lock_wait, 30)
                    wrong -> androidx.compose.ui.res.stringResource(R.string.lock_wrong_pin)
                    else -> androidx.compose.ui.res.stringResource(R.string.lock_enter_pin)
                }
                PinPadScreen(
                    title = androidx.compose.ui.res.stringResource(R.string.lock_title),
                    subtitle = subtitle,
                    subtitleIsError = wrong || wait,
                    warning = null,
                    note = null,
                    slotCount = filled,
                    filled = filled,
                    inputEnabled = !wait,
                    showLockIcon = true,
                    left = if (biometric) PinPadLeft.Fingerprint else PinPadLeft.Blank,
                    leftEnabled = biometric,
                    onDigit = {},
                    onDelete = {},
                    onLeft = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun capturePinPad(
        dark: Boolean,
        file: String,
        title: Int,
        subtitle: Int?,
        warning: Int?,
        slotCount: Int,
        filled: Int,
        actionEnabled: Boolean,
        showAction: Boolean = true,
    ) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                PinPadScreen(
                    title = androidx.compose.ui.res.stringResource(title),
                    subtitle = subtitle?.let { androidx.compose.ui.res.stringResource(it) }.orEmpty(),
                    subtitleIsError = false,
                    warning = warning?.let { androidx.compose.ui.res.stringResource(it) },
                    note = null,
                    slotCount = slotCount,
                    filled = filled,
                    inputEnabled = true,
                    showLockIcon = true,
                    left = PinPadLeft.Blank,
                    leftEnabled = false,
                    onDigit = {},
                    onDelete = {},
                    onLeft = {},
                    actionLabel = if (showAction) {
                        androidx.compose.ui.res.stringResource(R.string.pin_next)
                    } else {
                        null
                    },
                    actionEnabled = actionEnabled,
                    onAction = if (showAction) ({}) else null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun captureSettingsEntry(dark: Boolean, file: String, mode: AppLockMode) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                org.parkjw.apps.pdfbook.settings.SettingsScreen(
                    settings = org.parkjw.apps.pdfbook.settings.AppSettings(),
                    onSettingsChange = {},
                    appLockMode = mode,
                    versionName = "0.3.1",
                    versionCode = 4L,
                    scrollToEnd = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun captureSettings(
        dark: Boolean,
        file: String,
        config: LockConfig,
        biometricAvailable: Boolean,
    ) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                AppLockSettingsMenu(
                    config = config,
                    biometricAvailable = biometricAvailable,
                    onBack = {},
                    onEdit = { _: LockEdit -> },
                    onHideChange = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
