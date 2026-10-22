package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntSize

fun pageSlotSize(window: IntSize, pagesInSpread: Int): Pair<Int, Int> {
    val count = pagesInSpread.coerceAtLeast(1)
    return (window.width / count).coerceAtLeast(1) to window.height.coerceAtLeast(1)
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
