package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.PageCurlOverlay
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageCurlScreenshotsTest {
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
    fun settingsAnimationCurlLightFolded() {
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                SettingsScreen(
                    settings = AppSettings(pageTurnAnimation = PageTurnAnimation.Curl),
                    onSettingsChange = {},
                    versionName = "0.5.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowAnimationChoice = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onRoot().captureRoboImage("settings-animation-curl-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun settingsAnimationCurlLightFoldedEn() {
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                SettingsScreen(
                    settings = AppSettings(
                        themeMode = ThemeMode.Light,
                        pageTurnAnimation = PageTurnAnimation.Curl,
                    ),
                    onSettingsChange = {},
                    versionName = "0.5.0",
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowAnimationChoice = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onRoot().captureRoboImage("settings-animation-curl-light-folded-en.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageCurlMidLightFolded() {
        val pages = SolidPageBitmaps(2, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val front = runBlocking { pages.render(1, 360, 640)!! }
        val back = runBlocking { pages.render(2, 360, 640)!! }
        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ReaderBackground),
                ) {
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = 0.45f,
                        side = CurlSide.Left,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("reader-page-curl-mid-light-folded.png")
    }
}
