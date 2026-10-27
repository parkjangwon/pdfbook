package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** Rect the curl overlay must cover; null means the full page pane (flat, tabletop, spreads). */
fun curlOverlayBounds(
    window: IntSize,
    posture: FoldPosture,
    direction: ReadingDirection,
    twoPage: Boolean,
): IntRect? {
    if (posture !is FoldPosture.Book || twoPage) return null
    val half = bookSingleHalf(bookHalves(window, posture.foldBounds), direction)
    return half.takeIf { it.width > 0 && it.height > 0 }
}

/**
 * Rects for a two-page curl: [sheet] is the turning page, [spread] is what it may paint
 * (the opposite page included) and [gap] is the Book-posture hinge that the flap must
 * not cover. A one-page spread has sheet == spread and no gap.
 */
data class CurlSpreadRects(
    val sheet: IntRect,
    val spread: IntRect,
    val gap: IntRect?,
)

/**
 * Sheet / spread rects for a curl over [spreadPages] pages in [posture]. Which page turns
 * is [side] (mirrored by [direction] in the one-page Book case, where the single page sits
 * on one half). Null when there is no page to turn.
 *
 * Separate from [curlOverlayBounds], whose null two-page result current callers rely on.
 */
fun curlSpreadRects(
    window: IntSize,
    posture: FoldPosture,
    direction: ReadingDirection,
    side: CurlSide,
    spreadPages: Int,
): CurlSpreadRects? {
    if (spreadPages < 1 || window.width <= 0 || window.height <= 0) return null
    val slot = when (posture) {
        is FoldPosture.Book -> IntRect(0, 0, window.width, window.height)
        is FoldPosture.Tabletop -> tabletopHalves(window, posture.foldBounds).first
        FoldPosture.Flat -> IntRect(0, 0, window.width, window.height)
    }
    val sheet = when {
        posture is FoldPosture.Book -> {
            val halves = bookHalves(window, posture.foldBounds)
            when {
                spreadPages == 1 -> bookSingleHalf(halves, direction)
                side == CurlSide.Left -> halves.first
                else -> halves.second
            }
        }
        spreadPages > 1 -> if (side == CurlSide.Left) slot.leftHalf() else slot.rightHalf()
        else -> slot
    }
    val spread = if (spreadPages > 1) slot else sheet
    val gap = if (spreadPages > 1 && posture is FoldPosture.Book) {
        posture.foldBounds
            .clipTo(spread)
            .takeIf { it.width > 0 && it.height > 0 }
    } else {
        null
    }
    return CurlSpreadRects(sheet, spread, gap).takeIf { sheet.width > 0 && sheet.height > 0 }
}

private fun IntRect.leftHalf(): IntRect =
    IntRect(left, top, (left + right) / 2, bottom)

private fun IntRect.rightHalf(): IntRect =
    IntRect((left + right) / 2, top, right, bottom)

private fun IntRect.clipTo(bounds: IntRect): IntRect =
    IntRect(
        left.coerceIn(bounds.left, bounds.right),
        top.coerceIn(bounds.top, bounds.bottom),
        right.coerceIn(bounds.left, bounds.right),
        bottom.coerceIn(bounds.top, bounds.bottom),
    )

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
