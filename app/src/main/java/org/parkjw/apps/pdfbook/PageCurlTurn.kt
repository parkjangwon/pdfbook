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
 * A two-page Book with a hinge gap uses the virtual spread (halves butted together), so
 * `spreadRequests` renders each half at half.width and the curl looks them up with the same
 * key; Flat and a flush Book keep the window.
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
            val halves = bookHalves(window, posture.foldBounds)
            if (!twoPage) {
                val half = bookSingleHalf(halves, direction)
                return IntSize(half.width.coerceAtLeast(1), half.height.coerceAtLeast(1))
            }
            val gap = (halves.second.left - halves.first.right).coerceAtLeast(0)
            if (gap > 1) {
                return IntSize(
                    (halves.first.width + halves.second.width).coerceAtLeast(1),
                    window.height.coerceAtLeast(1),
                )
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
data class CurlCanvas(
    val size: IntSize,
    val left: IntRect,
    val right: IntRect,
    val hinge: CurlHinge? = null,
)

/**
 * Foldable hinge that must stay visible while a two-page sheet turns: [gap] px of the window
 * between the two halves. The curl geometry is computed on the virtual spread (the halves
 * butted together) and the overlay shifts everything right of the spine by [gap], so the flap is
 * cut at the hinge instead of painting over it (V071-TWOPAGE.md §4, option A).
 */
data class CurlHinge(val gap: Float)

/**
 * Canvas size and the two page slots for a two-page curl. The Canvas is the page pane:
 * for Tabletop that is the top half (the overlay is placed there), for Flat the whole window.
 * A Book posture with a hinge gap returns the virtual spread — the two halves butted together —
 * plus the [CurlHinge] the overlay cuts at; a flush hinge returns the window halves.
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
            val gap = (halves.second.left - halves.first.right).coerceAtLeast(0)
            // A real hinge gap (> 1px) curls on the virtual spread; one pixel is left to the
            // flush path, where the 1px landing rule already holds.
            if (gap > 1 && halves.first.width > 0 && halves.second.width > 0) {
                val size = IntSize(halves.first.width + halves.second.width, window.height)
                CurlCanvas(
                    size,
                    IntRect(0, 0, halves.first.width, window.height),
                    IntRect(halves.first.width, 0, size.width, window.height),
                    CurlHinge(gap.toFloat()),
                )
            } else {
                CurlCanvas(window, halves.first, halves.second)
            }
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

/**
 * Clip bounds for the two-page fold, in canvas coordinates: the turning sheet and the revealed
 * page plus the opposite pages mirrored about the spine onto the turning half. At progress 1 the
 * revealed polygon covers this union, so its reflection (the flap) covers the opposite pages at
 * their own rects however much the four pages differ in size.
 */
internal fun curlFoldUnion(layout: CurlSheetLayout): Rect {
    fun mirrored(rect: Rect): Rect = Rect(
        2f * layout.spineX - rect.right,
        rect.top,
        2f * layout.spineX - rect.left,
        rect.bottom,
    )
    return layout.sheet
        .expandedTo(layout.landing)
        .expandedTo(mirrored(layout.backFace))
        .expandedTo(mirrored(layout.still))
}

/** True when all four page rects sit within [tolerance] px of the turning sheet. */
internal fun CurlSheetLayout.rectsMatchSheet(tolerance: Float = 1f): Boolean {
    fun Rect.matches(other: Rect): Boolean =
        abs(left - other.left) <= tolerance &&
            abs(top - other.top) <= tolerance &&
            abs(right - other.right) <= tolerance &&
            abs(bottom - other.bottom) <= tolerance
    return landing.matches(sheet) && backFace.matches(sheet) && still.matches(sheet)
}

/**
 * Two-page curl guard. The overlay draws the turning page, the revealed page, the still page
 * and the flap's back face each at its own measured rect, so the first and last frames equal
 * the two spreads pixel for pixel even when a book's pages differ by a few points (a real PDF
 * rarely has identical page sizes). What must hold is that the two slots meet at the spine:
 * each rect's spine-side edge lies on [spineX] and every rect stays inside the canvas. A hinge
 * gap (Book posture) fails here, because its two spine edges are apart, and the turn slides.
 */
fun twoPageCurlMeetsAtSpine(
    sheet: Rect,
    landing: Rect,
    backFace: Rect,
    side: CurlSide,
    still: Rect,
    spineX: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    tolerance: Float = 1f,
): Boolean {
    val rects = listOf(sheet, landing, backFace, still)
    if (rects.any { it.width <= 0f || it.height <= 0f }) return false
    if (rects.any {
            it.left < -tolerance ||
                it.top < -tolerance ||
                it.right > canvasWidth + tolerance ||
                it.bottom > canvasHeight + tolerance
        }
    ) {
        return false
    }
    fun edgeAt(rect: Rect, leftEdge: Boolean): Boolean =
        abs((if (leftEdge) rect.left else rect.right) - spineX) <= tolerance
    return if (side == CurlSide.Right) {
        edgeAt(sheet, leftEdge = true) && edgeAt(landing, leftEdge = true) &&
            edgeAt(backFace, leftEdge = false) && edgeAt(still, leftEdge = false)
    } else {
        edgeAt(sheet, leftEdge = false) && edgeAt(landing, leftEdge = false) &&
            edgeAt(backFace, leftEdge = true) && edgeAt(still, leftEdge = true)
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
