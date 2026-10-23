package org.parkjw.apps.pdfbook.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.GoToPageDialog
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderScrim
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class GoToPageScreenshotsTest {
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
    fun goToPageLightFolded() {
        capture("reader-go-to-page-light-folded.png", dark = false)
    }

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun goToPageLightFoldedEn() {
        capture("reader-go-to-page-light-folded-en.png", dark = false)
    }

    private fun capture(file: String, dark: Boolean) {
        captureRoboImage(file) {
            ScreenshotTheme(dark, ReaderBackground) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ReaderBackground)
                        .background(ReaderScrim),
                    contentAlignment = Alignment.Center,
                ) {
                    GoToPageDialog(pageCount = 120, onDismiss = {}, onGo = {})
                }
            }
        }
    }
}
