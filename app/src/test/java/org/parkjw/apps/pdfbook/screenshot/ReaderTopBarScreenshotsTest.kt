package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ReaderOverlayTop
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderTopBarScreenshotsTest {
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
    @Config(qualifiers = "ko-w320dp-h920dp-xxhdpi")
    fun light320() = capture("reader-topbar-light-320dp.png", 320, dark = false)

    @Test
    @Config(qualifiers = "ko-w320dp-h920dp-night-xxhdpi")
    fun dark320() = capture("reader-topbar-dark-320dp.png", 320, dark = true)

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp-xxhdpi")
    fun light360() = capture("reader-topbar-light-360dp.png", 360, dark = false)

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp-night-xxhdpi")
    fun dark360() = capture("reader-topbar-dark-360dp.png", 360, dark = true)

    @Test
    @Config(qualifiers = "ko-w412dp-h920dp-xxhdpi")
    fun light412() = capture("reader-topbar-light-412dp.png", 412, dark = false)

    @Test
    @Config(qualifiers = "ko-w412dp-h920dp-night-xxhdpi")
    fun dark412() = capture("reader-topbar-dark-412dp.png", 412, dark = true)

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp-xxhdpi")
    fun menuLight360() = capture("reader-topbar-menu-light-360dp.png", 360, dark = false, menuOpen = true)

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp-night-xxhdpi")
    fun menuDark360() = capture("reader-topbar-menu-dark-360dp.png", 360, dark = true, menuOpen = true)

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp-xxhdpi")
    fun fontScale2Light360() =
        capture("reader-topbar-fontscale2-light-360dp.png", 360, dark = false, fontScale = 2f)

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(
        file: String,
        widthDp: Int,
        dark: Boolean,
        menuOpen: Boolean = false,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            ScreenshotTheme(dark, ReaderBackground) {
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = base.density, fontScale = fontScale),
                ) {
                    Box(Modifier.fillMaxSize().background(ReaderBackground)) {
                        ReaderOverlayTop(
                            title = "바닷가 마을 이야기 3권",
                            twoPage = false,
                            direction = ReadingDirection.RTL,
                            onBack = {},
                            onDirectionChange = {},
                            showPageMode = true,
                            modifier = Modifier.align(Alignment.TopStart),
                            windowWidthDp = widthDp,
                            onOpenBookList = {},
                            onOpenPageNav = {},
                            bookmarked = false,
                            onToggleBookmark = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        if (menuOpen) {
            composeRule.onNodeWithTag("reader_more_button").performClick()
            composeRule.waitForIdle()
        }
        composeRule.mainClock.autoAdvance = false
        captureScreenRoboImage(file)
    }
}
