package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class ImageAdjustReaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pageDrawsWithTheBookAdjustment() {
        val (r, g, b) = corner(DisplayMode.Default, ImageAdjust(grayscale = 100), page = 0xFFFF0000.toInt())
        assertTrue("r=$r g=$g b=$b", abs(r - 76) <= 4 && abs(g - 76) <= 4 && abs(b - 76) <= 4)
    }

    @Test
    fun defaultAdjustmentLeavesThePageUntouched() {
        val (r, g, b) = corner(DisplayMode.Default, ImageAdjust.Default, page = 0xFFFF0000.toInt())
        assertTrue("r=$r g=$g b=$b", r > 247 && g < 8 && b < 8)
    }

    @Test
    fun pageColorIsAppliedAfterTheAdjustment() {
        // White page, brightness -50 first (191), then inverted (64).
        val (r, g, b) = corner(DisplayMode.Invert, ImageAdjust(brightness = -50), page = 0xFFFFFFFF.toInt())
        assertTrue("r=$r g=$g b=$b", abs(r - 64) <= 5 && abs(g - 64) <= 5 && abs(b - 64) <= 5)
    }

    private fun corner(mode: DisplayMode, adjust: ImageAdjust, page: Int): Triple<Int, Int, Int> {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                CompositionLocalProvider(
                    LocalDisplayMode provides mode,
                    LocalImageAdjust provides adjust,
                ) {
                    Box(Modifier.size(200.dp).testTag("page_host")) {
                        PageBitmap(
                            renderer = SolidPageBitmaps(1, page, 0xFF000000.toInt()),
                            page = 1,
                            maxWidth = 200,
                            maxHeight = 200,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val px = composeRule.onNodeWithTag("page_host").captureToImage().toPixelMap()
        val c = px[2, px.height - 2]
        fun ch(v: Float) = (v * 255f + 0.5f).toInt()
        return Triple(ch(c.red), ch(c.green), ch(c.blue))
    }
}
