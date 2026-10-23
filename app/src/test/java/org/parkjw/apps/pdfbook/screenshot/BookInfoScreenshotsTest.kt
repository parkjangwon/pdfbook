package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookInfoSheet
import org.parkjw.apps.pdfbook.BookState
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class BookInfoScreenshotsTest {
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
    fun bookInfoLightFolded() = capture(dark = false, file = "book-info-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun bookInfoDarkFolded() = capture(dark = true, file = "book-info-dark-folded.png")

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun bookInfoLightFoldedEn() = capture(dark = false, file = "book-info-light-folded-en.png")

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(dark: Boolean, file: String) {
        composeRule.setContent { Content(dark) }
        composeRule.waitForIdle()
        captureScreenRoboImage(file)
    }

    @Composable
    private fun Content(dark: Boolean) {
        ScreenshotTheme(dark) {
            BookInfoSheet(
                book = BookInfo("content://shot/night-garden.pdf", "밤의 정원.pdf", 13_000_000),
                state = BookState(page = 12, pageCount = 120, openedAt = 1_700_000_000_000L),
                onDismiss = {},
                modifier = Modifier,
            )
        }
    }
}
