package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntSize

/**
 * Bitmap cache key size for the visible page slot.
 * Tabletop uses the page pane; book single-page uses the half that [BookHalfPage] draws into.
 */
fun pageRenderSlot(
    window: IntSize,
    posture: FoldPosture,
    direction: ReadingDirection,
    twoPage: Boolean,
): IntSize {
    when (posture) {
        is FoldPosture.Tabletop -> {
            val page = tabletopHalves(window, posture.foldBounds).first
            return IntSize(page.width.coerceAtLeast(1), page.height.coerceAtLeast(1))
        }
        is FoldPosture.Book -> {
            if (!twoPage) {
                val half = bookSingleHalf(bookHalves(window, posture.foldBounds), direction)
                return IntSize(half.width.coerceAtLeast(1), half.height.coerceAtLeast(1))
            }
        }
        FoldPosture.Flat -> Unit
    }
    return IntSize(window.width.coerceAtLeast(1), window.height.coerceAtLeast(1))
}

fun isSpreadCached(
    renderer: PageBitmaps,
    spread: List<Int>,
    maxWidth: Int,
    maxHeight: Int,
): Boolean {
    if (spread.isEmpty() || maxWidth <= 0 || maxHeight <= 0) return false
    val width = (maxWidth / spread.size.coerceAtLeast(1)).coerceAtLeast(1)
    return spread.all { page ->
        val bmp = renderer.cached(page, width, maxHeight)
        bmp != null && !bmp.isRecycled
    }
}

fun cachedSpreadBitmaps(
    renderer: PageBitmaps,
    spread: List<Int>,
    maxWidth: Int,
    maxHeight: Int,
): Bitmap? {
    if (spread.size != 1) return null
    val width = maxWidth.coerceAtLeast(1)
    val height = maxHeight.coerceAtLeast(1)
    val bmp = renderer.cached(spread.first(), width, height)
    return bmp?.takeUnless { it.isRecycled }
}
