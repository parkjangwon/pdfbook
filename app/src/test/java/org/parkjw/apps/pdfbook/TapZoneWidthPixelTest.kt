package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
class TapZoneWidthPixelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun guideBoundariesFollowFortyPercent() {
        val page = ReaderOnOverlay
        composeRule.setContent {
            Box(
                Modifier
                    .size(300.dp, 200.dp)
                    .background(page)
                    .testTag("host"),
            ) {
                TapZoneGuide(direction = ReadingDirection.LTR, sideFraction = tapZoneSideFraction(40))
            }
        }
        val pixels = composeRule.onNodeWithTag("host").captureToImage().toPixelMap()
        val side = ReaderTapZoneSide.compositeOver(page)
        val center = ReaderTapZoneCenter.compositeOver(page)
        assertClose(side, pixels[115, 4])
        assertClose(center, pixels[126, 4])
        assertClose(center, pixels[174, 4])
        assertClose(side, pixels[185, 4])
    }

    @Test
    fun sheetUsesOrangeOnlyOnSliderAndInkDoneButton() {
        composeRule.setContent {
            Box(
                Modifier
                    .width(360.dp)
                    .testTag("host"),
            ) {
                TapZoneWidthSheet(percent = 40, onPercentChange = {}, onDone = {})
            }
        }
        val pixels = composeRule.onNodeWithTag("host").captureToImage().toPixelMap()
        val slider = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).getBoundsInRoot()
        var orangeInSlider = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                if (distance(pixels[x, y], ReaderAccent) < 0.08f) {
                    val inside = x >= slider.left.value && x <= slider.right.value &&
                        y >= slider.top.value && y <= slider.bottom.value
                    assertTrue("orange outside slider at $x,$y", inside)
                    orangeInSlider++
                }
            }
        }
        assertTrue(orangeInSlider > 0)
        val done = composeRule.onNodeWithText("완료").getBoundsInRoot()
        assertClose(ReaderOnOverlay, pixels[(done.left.value + 6).toInt(), ((done.top.value + done.bottom.value) / 2).toInt()])
    }

    private fun assertClose(expected: Color, actual: Color) {
        assertTrue("expected $expected got $actual", distance(expected, actual) < 0.02f)
    }

    private fun distance(a: Color, b: Color): Float =
        maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue))
}
