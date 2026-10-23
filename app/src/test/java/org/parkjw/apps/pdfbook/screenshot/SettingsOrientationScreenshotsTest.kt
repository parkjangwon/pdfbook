package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class SettingsOrientationScreenshotsTest {
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
    fun settingsOrientationChoiceLightFolded() =
        captureChoice(dark = false, file = "settings-orientation-choice-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun settingsOrientationChoiceDarkFolded() =
        captureChoice(dark = true, file = "settings-orientation-choice-dark-folded.png")

    private fun captureChoice(dark: Boolean, file: String) {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            ScreenshotTheme(dark = dark) {
                SettingsScreen(
                    settings = sampleSettings().copy(
                        themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
                    ),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    versionCode = 4L,
                    modifier = Modifier.fillMaxSize(),
                    initialSubScreen = SettingsSubScreen.Reading,
                )
            }
        }
        composeRule.onNodeWithText(app.getString(R.string.settings_screen_orientation))
            .performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }
}
