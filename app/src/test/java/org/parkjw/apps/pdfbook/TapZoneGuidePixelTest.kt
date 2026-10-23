package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.DarkPrimary
import org.parkjw.apps.pdfbook.ui.theme.LightPrimary
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneCenter
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneSide
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class TapZoneGuidePixelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val page = ReaderOnOverlay
    private val sideColor = ReaderTapZoneSide.compositeOver(page)
    private val centerColor = ReaderTapZoneCenter.compositeOver(page)

    @Test
    fun sidesAreDarkerThanMenuAndMatchTokens() {
        val pixels = capture(ReadingDirection.LTR)
        val y = 4
        assertClose(sideColor, pixels[30, y])
        assertClose(centerColor, pixels[150, y])
        assertClose(sideColor, pixels[270, y])
        assertTrue(luminance(pixels[30, y]) < luminance(pixels[150, y]))
    }

    @Test
    fun boundariesSitAtThirds() {
        val pixels = capture(ReadingDirection.RTL)
        val y = 4
        assertClose(sideColor, pixels[95, y])
        assertClose(centerColor, pixels[106, y])
        assertClose(centerColor, pixels[194, y])
        assertClose(sideColor, pixels[205, y])
    }

    @Test
    fun ltrGuideHasNoOrange() = assertNoOrange(capture(ReadingDirection.LTR))

    @Test
    fun rtlGuideHasNoOrange() = assertNoOrange(capture(ReadingDirection.RTL))

    private fun assertNoOrange(pixels: PixelMap) {
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val c = pixels[x, y]
                for (orange in listOf(ReaderAccent, LightPrimary, DarkPrimary)) {
                    assertTrue("orange at $x,$y", distance(c, orange) > 0.25f)
                }
            }
        }
    }

    private fun capture(direction: ReadingDirection): PixelMap {
        composeRule.setContent {
            Box(
                Modifier
                    .size(300.dp, 200.dp)
                    .background(page)
                    .testTag("guide_host"),
            ) {
                TapZoneGuide(direction = direction)
            }
        }
        composeRule.waitForIdle()
        val image = composeRule.onNodeWithTag("guide_host").captureToImage()
        assertEquals(300, image.width)
        return image.toPixelMap()
    }

    private fun assertClose(expected: Color, actual: Color) {
        assertTrue("expected $expected got $actual", distance(expected, actual) < 0.02f)
    }

    private fun distance(a: Color, b: Color): Float =
        maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue))

    private fun luminance(c: Color): Float = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
}
