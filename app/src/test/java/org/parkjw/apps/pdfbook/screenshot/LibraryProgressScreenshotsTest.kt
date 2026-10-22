package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.LibraryScreen
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class LibraryProgressScreenshotsTest {
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
    fun bookMenuLightFolded() = capture(dark = false, file = "library-book-menu-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun bookMenuDarkFolded() = capture(dark = true, file = "library-book-menu-dark-folded.png")

    private fun capture(dark: Boolean, file: String) {
        val listing = sampleListing()
        composeRule.setContent {
            ScreenshotTheme(dark) {
                LibraryScreen(
                    listing = listing,
                    states = sampleStates(listing),
                    onOpen = {},
                    onSettings = {},
                    covers = sampleCovers(listing),
                    resumeBooks = emptyList(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onAllNodesWithTag("library_book")[0].performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(file)
    }
}
