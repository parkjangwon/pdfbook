package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.update.UpdateUiState
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class UpdateCheckingScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

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
    fun settingsUpdateCheckingLightFolded() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                SettingsScreen(
                    settings = sampleSettings().copy(themeMode = ThemeMode.Light),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    modifier = Modifier.fillMaxSize(),
                    scrollToEnd = true,
                    updateState = UpdateUiState.Checking,
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.onRoot().captureRoboImage("settings-update-checking-light-folded.png")
    }
}
