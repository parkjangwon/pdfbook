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
import org.parkjw.apps.pdfbook.lock.PinCreateScreen
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
    fun lockLightFolded() = captureLock(dark = false, file = "lock-light-folded.png", filled = 2, wrong = false, wait = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun lockDarkFolded() = captureLock(dark = true, file = "lock-dark-folded.png", filled = 2, wrong = false, wait = false)

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun lockLightUnfolded() = captureLock(dark = false, file = "lock-light-unfolded.png", filled = 2, wrong = false, wait = false)

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun lockDarkUnfolded() = captureLock(dark = true, file = "lock-dark-unfolded.png", filled = 2, wrong = false, wait = false)

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun lockWrongDarkFolded() = captureLock(dark = true, file = "lock-wrong-dark-folded.png", filled = 4, wrong = true, wait = false)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun lockWaitLightFolded() = captureLock(dark = false, file = "lock-wait-light-folded.png", filled = 0, wrong = false, wait = true)

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pinCreateLightFolded() = capturePinCreate(dark = false, file = "pin-create-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun pinCreateDarkFolded() = capturePinCreate(dark = true, file = "pin-create-dark-folded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun appLockSettingsLightFolded() = captureSettings(dark = false, file = "app-lock-settings-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun appLockSettingsDarkFolded() = captureSettings(dark = true, file = "app-lock-settings-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun appLockSettingsLightUnfolded() = captureSettings(dark = false, file = "app-lock-settings-light-unfolded.png")

    @Test
    @Config(qualifiers = UNFOLDED_NIGHT_QUALIFIERS)
    fun appLockSettingsDarkUnfolded() = captureSettings(dark = true, file = "app-lock-settings-dark-unfolded.png")

    private fun captureLock(dark: Boolean, file: String, filled: Int, wrong: Boolean, wait: Boolean) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                val subtitle = when {
                    wait -> androidx.compose.ui.res.stringResource(R.string.lock_wait, 30)
                    wrong -> androidx.compose.ui.res.stringResource(R.string.lock_wrong_pin)
                    else -> androidx.compose.ui.res.stringResource(R.string.lock_title)
                }
                PinPadScreen(
                    title = androidx.compose.ui.res.stringResource(R.string.lock_enter_pin),
                    subtitle = subtitle,
                    subtitleIsError = wrong,
                    warning = null,
                    note = null,
                    slotCount = 4,
                    filled = filled,
                    inputEnabled = !wait,
                    showLockIcon = true,
                    left = PinPadLeft.Blank,
                    leftEnabled = false,
                    onDigit = {},
                    onDelete = {},
                    onLeft = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun capturePinCreate(dark: Boolean, file: String) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                PinCreateScreen(needsPinNote = false, onCreated = {}, onBack = {})
            }
        }
    }

    private fun captureSettings(dark: Boolean, file: String) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                AppLockSettingsMenu(
                    config = LockConfig(
                        mode = AppLockMode.Pin,
                        pinHash = "hash",
                        pinSalt = "salt",
                        pinLength = 4,
                        hideInRecents = true,
                    ),
                    biometricAvailable = true,
                    onBack = {},
                    onEdit = { _: LockEdit -> },
                    onHideChange = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
