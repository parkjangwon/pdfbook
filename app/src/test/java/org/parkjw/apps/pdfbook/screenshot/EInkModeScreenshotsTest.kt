package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class EInkModeScreenshotsTest {

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
    fun settingsReadingEInkLightFolded() {
        captureSettings(dark = false, file = "settings-reading-e-ink-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun settingsReadingEInkDarkFolded() {
        captureSettings(dark = true, file = "settings-reading-e-ink-dark-folded.png")
    }

    private fun captureSettings(dark: Boolean, file: String) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                SettingsScreen(
                    settings = sampleSettings().copy(
                        themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
                        eInkMode = true,
                    ),
                    onSettingsChange = {},
                    versionName = "0.5.0",
                    versionCode = 5L,
                    modifier = Modifier.fillMaxSize(),
                    initialSubScreen = SettingsSubScreen.Reading,
                )
            }
        }
    }
}
