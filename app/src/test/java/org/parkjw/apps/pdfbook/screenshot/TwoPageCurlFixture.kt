package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import org.parkjw.apps.pdfbook.CropMargins
import org.parkjw.apps.pdfbook.CurlSheetLayout
import org.parkjw.apps.pdfbook.CurlSide
import org.parkjw.apps.pdfbook.FoldPosture
import org.parkjw.apps.pdfbook.PageCurlOverlay
import org.parkjw.apps.pdfbook.SlotAlignment
import org.parkjw.apps.pdfbook.colorFilter
import org.parkjw.apps.pdfbook.curlCanvas
import org.parkjw.apps.pdfbook.curlLook
import org.parkjw.apps.pdfbook.expandedTo
import org.parkjw.apps.pdfbook.pageDrawRect
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground

/**
 * Two-page curl preview shared by the #167 captures. Pages are generated synthetic
 * colour pages; no real book, screenshot or private file is used. [mode] applies the same
 * page color filter and curl look the reader passes for that display mode.
 */
@Composable
internal fun TwoPageCurlPreview(
    side: CurlSide,
    progress: Float,
    currentLeft: Bitmap,
    currentRight: Bitmap,
    targetLeft: Bitmap,
    targetRight: Bitmap,
    mode: DisplayMode = DisplayMode.Default,
) {
    val filter = mode.colorFilter()
    val look = mode.curlLook()
    BoxWithConstraints(Modifier.fillMaxSize().background(ReaderBackground)) {
        val window = IntSize(constraints.maxWidth, constraints.maxHeight)
        val canvas = curlCanvas(window, FoldPosture.Flat) ?: return@BoxWithConstraints
        val slots = listOf(canvas.left, canvas.right)
        val none = CropMargins()
        fun rects(left: Bitmap, right: Bitmap) = listOf(
            pageDrawRect(left.width, left.height, none, slots[0], SlotAlignment.End, null),
            pageDrawRect(right.width, right.height, none, slots[1], SlotAlignment.Start, null),
        )
        val currentRects = rects(currentLeft, currentRight)
        val targetRects = rects(targetLeft, targetRight)
        // The real stack: the reader has already scrolled its pager to the target spread
        // when the turn animates, so the pager below sits on the target spread. Page 0 is
        // the current spread; the capture would expose the swap if the still page were
        // taken from the pager.
        val pagerState = rememberPagerState(initialPage = 1) { 2 }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
        ) { index ->
            ComposeCanvas(Modifier.fillMaxSize()) {
                if (index == 0) {
                    drawPage(currentLeft, currentRects[0], filter)
                    drawPage(currentRight, currentRects[1], filter)
                } else {
                    drawPage(targetLeft, targetRects[0], filter)
                    drawPage(targetRight, targetRects[1], filter)
                }
            }
        }
        val turning = if (side == CurlSide.Right) 1 else 0
        val sheet = currentRects[turning]
        val backFace = targetRects[1 - turning]
        PageCurlOverlay(
            front = if (turning == 1) currentRight else currentLeft,
            back = if (turning == 1) targetRight else targetLeft,
            still = if (turning == 1) currentLeft else currentRight,
            progress = { progress },
            side = side,
            modifier = Modifier.fillMaxSize(),
            colorFilter = filter,
            flapBack = if (turning == 1) targetLeft else targetRight,
            layout = CurlSheetLayout(
                sheet = sheet,
                spread = sheet.expandedTo(backFace),
                backFace = backFace,
                spineX = if (side == CurlSide.Right) sheet.left else sheet.right,
                landing = targetRects[turning],
                still = currentRects[1 - turning],
            ),
            flapColor = look.flapColor,
            shadowColor = look.shadowColor,
            shadowAlpha = look.shadowAlpha,
            shadowMidAlpha = look.shadowMidAlpha,
            foldHighlightColor = look.foldHighlightColor,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPage(
    bitmap: Bitmap,
    rect: Rect,
    colorFilter: ColorFilter?,
) {
    drawImage(
        image = bitmap.asImageBitmap(),
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
        colorFilter = colorFilter,
    )
}

/**
 * Left/right asymmetric page: a band down the left edge and a block in the top-left
 * corner. A mirrored back face puts both on the right, so the capture can tell it apart.
 */
internal fun syntheticTwoPageCurlPage(
    fill: Int,
    accent: Int,
    height: Int = TWO_PAGE_HEIGHT,
): Bitmap {
    val bitmap = Bitmap.createBitmap(TWO_PAGE_WIDTH, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(fill)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    canvas.drawRect(0f, 0f, TWO_PAGE_WIDTH * 0.12f, height.toFloat(), paint)
    paint.color = TWO_PAGE_INK
    canvas.drawRect(
        TWO_PAGE_WIDTH * 0.22f,
        height * 0.1f,
        TWO_PAGE_WIDTH * 0.46f,
        height * 0.22f,
        paint,
    )
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(TWO_PAGE_WIDTH * 0.34f, height * 0.62f, TWO_PAGE_WIDTH * 0.16f, paint)
    return bitmap
}

private const val TWO_PAGE_WIDTH = 600
private const val TWO_PAGE_HEIGHT = 849
private const val TWO_PAGE_INK = 0xFF1B1B1B.toInt()
