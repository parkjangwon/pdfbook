package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.math.roundToInt
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.CropMargins
import org.parkjw.apps.pdfbook.CurlSheetLayout
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.FoldPosture
import org.parkjw.apps.pdfbook.PageCurlOverlay
import org.parkjw.apps.pdfbook.SlotAlignment
import org.parkjw.apps.pdfbook.curlCanvas
import org.parkjw.apps.pdfbook.expandedTo
import org.parkjw.apps.pdfbook.pageDrawRect
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi captures for the two-page curl (#145 PR2). Pages are generated synthetic
 * colour pages; no real book, screenshot or private file is used. All captures live in one
 * test method: in this Robolectric setup a second capture-bearing method renders blank.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class PageCurlTwoPageScreenshotsTest {
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
    fun twoPageCurlLtrAndRtl() {
        val currentLeft = syntheticPage(0xFFF7F2E8.toInt(), 0xFFB07A2A.toInt())
        val currentRight = syntheticPage(0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt())
        val targetLeft = syntheticPage(0xFFF7F2E8.toInt(), 0xFF2E7D32.toInt())
        val targetRight = syntheticPage(0xFFF7F2E8.toInt(), 0xFFD9534F.toInt())

        var progress by mutableStateOf(0.25f)
        var side by mutableStateOf(CurlSide.Right)

        composeRule.setContent {
            ScreenshotTheme(dark = false, systemBarsColor = ReaderBackground) {
                TwoPageCurlPreview(
                    side = side,
                    progress = progress,
                    currentLeft = currentLeft,
                    currentRight = currentRight,
                    targetLeft = targetLeft,
                    targetRight = targetRight,
                )
            }
        }
        composeRule.waitForIdle()

        // LTR forward: the right sheet turns; the flap back face is the target's left page.
        for (p in listOf(0.25f, 0.5f, 0.75f)) {
            composeRule.runOnIdle {
                side = CurlSide.Right
                progress = p
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-two-ltr-p${percent(p)}.png")
        }

        // RTL forward: mirrored, the left sheet turns; the flap back face is the target's right page.
        for (p in listOf(0.25f, 0.5f, 0.75f)) {
            composeRule.runOnIdle {
                side = CurlSide.Left
                progress = p
            }
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("reader-page-curl-two-rtl-p${percent(p)}.png")
        }
    }

    @Composable
    private fun TwoPageCurlPreview(
        side: CurlSide,
        progress: Float,
        currentLeft: Bitmap,
        currentRight: Bitmap,
        targetLeft: Bitmap,
        targetRight: Bitmap,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(ReaderBackground)) {
            val window = IntSize(constraints.maxWidth, constraints.maxHeight)
            val canvas = curlCanvas(window, FoldPosture.Flat) ?: return@BoxWithConstraints
            val slots = listOf(canvas.left, canvas.right)
            val none = CropMargins()
            val leftRect = pageDrawRect(currentLeft.width, currentLeft.height, none, slots[0], SlotAlignment.End, null)
            val rightRect = pageDrawRect(currentRight.width, currentRight.height, none, slots[1], SlotAlignment.Start, null)
            val rects = listOf(leftRect, rightRect)
            val turning = if (side == CurlSide.Right) 1 else 0

            ComposeCanvas(Modifier.matchParentSize()) {
                drawPage(currentLeft, rects[0])
                drawPage(currentRight, rects[1])
            }
            val sheet = rects[turning]
            val backFace = rects[1 - turning]
            PageCurlOverlay(
                front = if (turning == 1) currentRight else currentLeft,
                back = if (turning == 1) targetRight else targetLeft,
                progress = { progress },
                side = side,
                modifier = Modifier.fillMaxSize(),
                flapBack = if (turning == 1) targetLeft else targetRight,
                layout = CurlSheetLayout(
                    sheet = sheet,
                    spread = sheet.expandedTo(backFace),
                    backFace = backFace,
                    spineX = if (side == CurlSide.Right) sheet.left else sheet.right,
                ),
            )
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPage(bitmap: Bitmap, rect: Rect) {
        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
            dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
        )
    }

    private fun percent(p: Float): Int = (p * 100).roundToInt()

    private fun syntheticPage(fill: Int, accent: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(fill)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRect(
            PAGE_WIDTH * 0.1f,
            PAGE_HEIGHT * 0.08f,
            PAGE_WIDTH * 0.9f,
            PAGE_HEIGHT * 0.32f,
            paint,
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawCircle(PAGE_WIDTH / 2f, PAGE_HEIGHT * 0.66f, PAGE_WIDTH * 0.22f, paint)
        return bitmap
    }

    private companion object {
        const val PAGE_WIDTH = 600
        const val PAGE_HEIGHT = 849
    }
}
