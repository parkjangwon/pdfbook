package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.parkjw.apps.pdfbook.colorFilter
import org.parkjw.apps.pdfbook.curlLook
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderListCurrent
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
                    // RTL forward: the left corner peels.
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = { 0.45f },
                        side = CurlSide.Left,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("reader-page-curl-mid-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageCurlMidLtrLightFolded() {
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
                    // LTR forward: the right corner peels.
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = { 0.45f },
                        side = CurlSide.Right,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("reader-page-curl-mid-ltr-light-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageCurlMidNightFolded() {
        // Night page mode: the page is dark, so the paper back and shadow use the
        // inverted values. The #112 work passes the same colorFilter here.
        val pages = SolidPageBitmaps(2, 0xFF121212.toInt(), 0xFFCFC6B4.toInt())
        val front = runBlocking { pages.render(1, 360, 640)!! }
        val back = runBlocking { pages.render(2, 360, 640)!! }
        composeRule.setContent {
            // The reader is theme-independent; the night look comes from the page
            // mode colors, not the app theme.
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ReaderBackground),
                ) {
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = { 0.45f },
                        side = CurlSide.Left,
                        modifier = Modifier.fillMaxSize(),
                        flapColor = ReaderListCurrent,
                        shadowAlpha = 0.50f,
                        shadowMidAlpha = 0.20f,
                        foldHighlightColor = Color.White.copy(alpha = 0.12f),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("reader-page-curl-mid-night-folded.png")
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageCurlMidSepiaFolded() {
        captureMidCurl("reader-page-curl-mid-sepia-folded.png", DisplayMode.Sepia)
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageCurlMidInvertedFolded() {
        captureMidCurl("reader-page-curl-mid-inverted-folded.png", DisplayMode.Invert)
    }

    private fun captureMidCurl(file: String, mode: DisplayMode) {
        val look = mode.curlLook()
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
                    // Same filter and per-mode paper values the ReaderScreen passes.
                    PageCurlOverlay(
                        front = front,
                        back = back,
                        progress = { 0.45f },
                        side = CurlSide.Left,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = mode.colorFilter(),
                        flapColor = look.flapColor,
                        shadowColor = look.shadowColor,
                        shadowAlpha = look.shadowAlpha,
                        shadowMidAlpha = look.shadowMidAlpha,
                        foldHighlightColor = look.foldHighlightColor,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }
}
