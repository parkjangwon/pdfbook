package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.LocalDisplayMode
import org.parkjw.apps.pdfbook.READER_CROP_PREVIEW_IMAGE_TAG
import org.parkjw.apps.pdfbook.ReaderCropSheet
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class ReaderCropSheetScreenshotsTest {
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
    fun cropSheetLightFolded() = capture("reader-crop-sheet-light-folded.png", dark = false, pages = listOf(1))

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun cropSheetTwoPageDarkFolded() =
        capture(
            "reader-crop-sheet-dark-folded.png",
            dark = true,
            pages = listOf(2, 3),
            displayMode = DisplayMode.Invert,
        )

    @Test
    @Config(qualifiers = "en-w320dp-h640dp-xxhdpi")
    fun cropSheetEnglishNarrow() =
        capture("reader-crop-sheet-en-320.png", dark = false, pages = listOf(1))

    private fun capture(
        file: String,
        dark: Boolean,
        pages: List<Int>,
        displayMode: DisplayMode = DisplayMode.Default,
    ) {
        composeRule.setContent {
            ScreenshotTheme(dark = dark, systemBarsColor = ReaderBackground) {
                CompositionLocalProvider(LocalDisplayMode provides displayMode) {
                    Box(Modifier.fillMaxSize().background(ReaderBackground)) {
                        ReaderCropSheet(
                            renderer = SolidPageBitmaps(
                                pageCount = 4,
                                pageColor = 0xFFF7F2E8.toInt(),
                                accentColor = 0xFF3D5A80.toInt(),
                            ),
                            pages = pages,
                            twoPage = pages.size >= 2,
                            enabled = true,
                            onEnabledChange = {},
                            onDone = {},
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // Detection runs off the main dispatcher, so wait for the preview to finish drawing.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(READER_CROP_PREVIEW_IMAGE_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage(file)
    }
}
