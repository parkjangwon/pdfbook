package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

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

/** Visual left/right slots of a two-page spread inside the curl Canvas. */
data class CurlCanvas(val size: IntSize, val left: IntRect, val right: IntRect)

/**
 * Canvas size and the two page slots for a two-page curl. The Canvas is the page pane:
 * for Tabletop that is the top half (the overlay is placed there), for Flat and Book the
 * whole window. Book slots are the halves either side of the hinge, so a hinge gap shows
 * up as non-adjacent slots (and the turn falls back to Slide).
 */
fun curlCanvas(window: IntSize, posture: FoldPosture): CurlCanvas? {
    if (window.width <= 0 || window.height <= 0) return null
    return when (posture) {
        FoldPosture.Flat -> CurlCanvas(
            window,
            IntRect(0, 0, window.width / 2, window.height),
            IntRect(window.width / 2, 0, window.width, window.height),
        )
        is FoldPosture.Tabletop -> {
            val page = tabletopHalves(window, posture.foldBounds).first
            if (page.width <= 0 || page.height <= 0) {
                null
            } else {
                CurlCanvas(
                    IntSize(page.width, page.height),
                    IntRect(0, 0, page.width / 2, page.height),
                    IntRect(page.width / 2, 0, page.width, page.height),
                )
            }
        }
        is FoldPosture.Book -> {
            val halves = bookHalves(window, posture.foldBounds)
            CurlCanvas(window, halves.first, halves.second)
        }
    }
}

enum class SlotAlignment { Start, Center, End }

/**
 * Cropped content rect of a page drawn into [slot], mirroring PageBitmap/CroppedPage:
 * the shared spread scale (or the page's own fit) is clamped to the slot, aligned
 * CenterEnd on the left slot and CenterStart on the right.
 */
fun pageDrawRect(
    bitmapWidth: Int,
    bitmapHeight: Int,
    crop: CropMargins,
    slot: IntRect,
    alignment: SlotAlignment,
    sharedScale: Float?,
): Rect {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || slot.width <= 0 || slot.height <= 0) return Rect.Zero
    val left = (crop.left * bitmapWidth).roundToInt().coerceIn(0, bitmapWidth - 1)
    val top = (crop.top * bitmapHeight).roundToInt().coerceIn(0, bitmapHeight - 1)
    val right = (crop.right * bitmapWidth).roundToInt().coerceIn(0, bitmapWidth - 1 - left)
    val bottom = (crop.bottom * bitmapHeight).roundToInt().coerceIn(0, bitmapHeight - 1 - top)
    val sourceWidth = (bitmapWidth - left - right).coerceAtLeast(1)
    val sourceHeight = (bitmapHeight - top - bottom).coerceAtLeast(1)
    val fitScale = min(slot.width.toFloat() / sourceWidth, slot.height.toFloat() / sourceHeight)
    val drawScale = (sharedScale ?: fitScale).coerceAtMost(fitScale)
    val drawWidth = (sourceWidth * drawScale).roundToInt().coerceAtLeast(1)
    val drawHeight = (sourceHeight * drawScale).roundToInt().coerceAtLeast(1)
    val offsetX = when (alignment) {
        SlotAlignment.Start -> 0
        SlotAlignment.End -> slot.width - drawWidth
        SlotAlignment.Center -> (slot.width - drawWidth) / 2
    }
    val offsetY = (slot.height - drawHeight) / 2
    return Rect(
        (slot.left + offsetX).toFloat(),
        (slot.top + offsetY).toFloat(),
        (slot.left + offsetX + drawWidth).toFloat(),
        (slot.top + offsetY + drawHeight).toFloat(),
    )
}

fun Rect.expandedTo(other: Rect): Rect = Rect(
    min(left, other.left),
    min(top, other.top),
    maxOf(right, other.right),
    maxOf(bottom, other.bottom),
)

/** The same drawn rect within [tolerance] px on every edge. */
fun rectsMatch(a: Rect, b: Rect, tolerance: Float = 1f): Boolean =
    abs(a.left - b.left) <= tolerance &&
        abs(a.top - b.top) <= tolerance &&
        abs(a.right - b.right) <= tolerance &&
        abs(a.bottom - b.bottom) <= tolerance

/**
 * 오대리 rule: the turning sheet must land exactly on the opposite page, so the sheet and
 * the pages it turns onto have to agree within 1px. Otherwise the end frame jumps and the
 * turn falls back to Slide. [still] is the current spread's other page: it is painted under
 * the turn, so it must occupy the same rect as [backFace].
 */
fun sheetLandsOnSpread(
    sheet: Rect,
    targetSheet: Rect,
    backFace: Rect,
    side: CurlSide,
    still: Rect,
    tolerance: Float = 1f,
): Boolean {
    if (!rectsMatch(sheet, targetSheet, tolerance)) return false
    if (!rectsMatch(still, backFace, tolerance)) return false
    if (abs(sheet.width - backFace.width) > tolerance) return false
    if (abs(sheet.height - backFace.height) > tolerance) return false
    if (abs(sheet.top - backFace.top) > tolerance) return false
    val spine = if (side == CurlSide.Right) sheet.left else sheet.right
    return if (side == CurlSide.Right) {
        abs(backFace.right - spine) <= tolerance
    } else {
        abs(backFace.left - spine) <= tolerance
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
