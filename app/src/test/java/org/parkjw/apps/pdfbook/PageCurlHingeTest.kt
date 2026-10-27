package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Hinge cut for #172: a two-page sheet turning on a Book posture with a real hinge gap.
 *
 * The layout is in virtual-spread coordinates (the halves butted together); the overlay paints
 * the frame clipped on each side of the gap. The root behind the overlay is a sentinel colour, so
 * any pixel the gap shows means the overlay painted over the hinge — the flap, the background,
 * anything. Synthetic solid pages only.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageCurlHingeTest {
    @get:Rule
    val composeRule = createComposeRule()

    // mdpi 400x800: the hinge is 16px wide, the two halves are 192px each.
    private val windowWidth = 400
    private val gap = 16f
    private val spine = 192f
    private val pageRect = Rect(spine, 100f, spine * 2f, 700f)

    @Test
    fun gapStaysUntouchedForBothSidesAtHalfAndAlmostDone() {
        var progress by mutableStateOf(0.5f)
        var turning by mutableStateOf(CurlSide.Right)
        val front = solid(0xFFD9534F.toInt())
        val back = solid(0xFF2E7D32.toInt())
        val flapBack = solid(0xFFF0C040.toInt())
        val still = solid(0xFF3D5A80.toInt())
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Magenta)) {
                PageCurlOverlay(
                    front = front,
                    back = back,
                    flapBack = flapBack,
                    still = still,
                    progress = { progress },
                    side = turning,
                    modifier = Modifier.fillMaxSize(),
                    layout = layoutFor(turning),
                )
            }
        }
        for (side in listOf(CurlSide.Right, CurlSide.Left)) {
            for (p in listOf(0.5f, 0.95f)) {
                composeRule.runOnIdle {
                    turning = side
                    progress = p
                }
                composeRule.waitForIdle()
                val pixels = composeRule.onRoot().captureToImage().toPixelMap()
                assertGapUntouched(side, p, pixels)
                assertHalvesPainted(side, p, pixels)
            }
        }
    }

    /**
     * The gap columns are exactly the sentinel: no ReaderBackground wash, no flap paper, no page.
     * The overlay's two passes must stop at the spine on the left and at spine + gap on the right.
     */
    private fun assertGapUntouched(side: CurlSide, p: Float, pixels: PixelMap) {
        val sentinel = Color.Magenta.value
        val left = spine.toInt()
        val right = (spine + gap).toInt()
        for (y in 0 until pixels.height) {
            for (x in left until right) {
                assertEquals(
                    "$side p=$p: overlay painted the hinge gap at ($x, $y)",
                    sentinel,
                    pixels[x, y].value,
                )
            }
        }
    }

    /** Both drawn halves keep their page (the pages fill their rects) and are not the sentinel. */
    private fun assertHalvesPainted(side: CurlSide, p: Float, pixels: PixelMap) {
        val sentinel = Color.Magenta.value
        val y = 400
        val points = listOf(40, (spine / 2).toInt(), (spine + gap + 40).toInt(), windowWidth - 40)
        points.forEach { x ->
            assertTrue(
                "$side p=$p: nothing painted at ($x, $y)",
                pixels[x, y].value != sentinel,
            )
        }
    }

    /**
     * Virtual-spread layout for the given turning side: the rects are butted at [spine] and the
     * overlay shifts everything right of it by the gap.
     */
    private fun layoutFor(side: CurlSide): CurlSheetLayout {
        val left = Rect(0f, pageRect.top, spine, pageRect.bottom)
        val right = pageRect
        val sheet = if (side == CurlSide.Right) right else left
        val landing = sheet
        val backFace = if (side == CurlSide.Right) left else right
        return CurlSheetLayout(
            sheet = sheet,
            spread = left.expandedTo(right),
            backFace = backFace,
            spineX = spine,
            landing = landing,
            still = backFace,
            hinge = CurlHinge(gap),
        )
    }

    private fun solid(color: Int): Bitmap {
        val width = spine.toInt()
        val height = (pageRect.bottom - pageRect.top).toInt()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
    }
}
