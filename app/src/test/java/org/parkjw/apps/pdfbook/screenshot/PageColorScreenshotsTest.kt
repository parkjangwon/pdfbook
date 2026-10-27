package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ReaderScreen
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.SettingsScreen
import org.parkjw.apps.pdfbook.settings.SettingsSubScreen
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.spreads
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageColorScreenshotsTest {
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
    fun pageColorRowLightFolded() {
        captureSettings("settings-page-color-light-folded.png", dark = false)
    }

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun pageColorRowDarkFolded() {
        captureSettings("settings-page-color-dark-folded.png", dark = true)
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun pageColorRowLightFoldedEn() {
        captureSettings("settings-page-color-light-folded-en.png", dark = false)
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun pageColorDialogLightFolded() {
        captureSettings("settings-page-color-dialog-light-folded.png", dark = false, dialog = true)
    }

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun pageColorDialogDarkFolded() {
        captureSettings("settings-page-color-dialog-dark-folded.png", dark = true, dialog = true)
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun pageColorDialogLightFoldedEn() {
        captureSettings("settings-page-color-dialog-light-folded-en.png", dark = false, dialog = true)
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun readerInvertedLightFolded() {
        captureReader("reader-page-color-inverted-light-folded.png", DisplayMode.Invert)
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun readerSepiaLightFolded() {
        captureReader("reader-page-color-sepia-light-folded.png", DisplayMode.Sepia)
    }

    private fun captureSettings(file: String, dark: Boolean, dialog: Boolean = false) {
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                SettingsScreen(
                    settings = AppSettings(
                        themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light,
                        displayMode = DisplayMode.Invert,
                    ),
                    onSettingsChange = {},
                    versionName = "0.3.0",
                    versionCode = 4L,
                    modifier = Modifier.fillMaxSize(),
                    initialSubScreen = SettingsSubScreen.Reading,
                    initiallyShowDisplayChoice = dialog,
                )
            }
        }
    }

    private fun captureReader(file: String, displayMode: DisplayMode) {
        val pageCount = 12
        val pageSpreads = spreads(pageCount, twoPage = false, coverAlone = true)
        captureRoboImage(file) {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                ReaderScreen(
                    renderer = SolidPageBitmaps(
                        pageCount = pageCount,
                        pageColor = 0xFFF7F2E8.toInt(),
                        accentColor = 0xFF3D5A80.toInt(),
                    ),
                    pagerState = rememberPagerState(pageCount = { pageSpreads.size }),
                    spreads = pageSpreads,
                    title = "샘플 책",
                    pageCount = pageCount,
                    currentPage = 1,
                    twoPage = false,
                    onBack = {},
                    onDirectionChange = {},
                    overlayVisible = true,
                    showTapZoneGuide = false,
                    displayMode = displayMode,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
