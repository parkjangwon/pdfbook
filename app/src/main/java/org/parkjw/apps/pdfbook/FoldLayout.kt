package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** Clearance around the fold line where no button/handle may sit (dp → px at call site). */
const val FOLD_EXCLUSION_DP = 12

data class FoldHalves(val first: IntRect, val second: IntRect)

/** Left / right of a vertical hinge (book posture). */
fun bookHalves(window: IntSize, fold: IntRect): FoldHalves {
    val left = IntRect(0, 0, fold.left.coerceIn(0, window.width), window.height)
    val right = IntRect(fold.right.coerceIn(0, window.width), 0, window.width, window.height)
    return FoldHalves(left, right)
}

/** Top / bottom of a horizontal hinge (tabletop posture). */
fun tabletopHalves(window: IntSize, fold: IntRect): FoldHalves {
    val top = IntRect(0, 0, window.width, fold.top.coerceIn(0, window.height))
    val bottom = IntRect(0, fold.bottom.coerceIn(0, window.height), window.width, window.height)
    return FoldHalves(top, bottom)
}

fun foldExclusionZone(fold: IntRect, marginPx: Int, verticalHinge: Boolean): IntRect =
    if (verticalHinge) {
        IntRect(
            left = (fold.left - marginPx).coerceAtLeast(0),
            top = fold.top,
            right = fold.right + marginPx,
            bottom = fold.bottom,
        )
    } else {
        IntRect(
            left = fold.left,
            top = (fold.top - marginPx).coerceAtLeast(0),
            right = fold.right,
            bottom = fold.bottom + marginPx,
        )
    }

fun rectsOverlap(a: IntRect, b: IntRect): Boolean =
    a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

/**
 * Side / menu / side pad widths. Menu is [menuRatio] × side; [gap] between pads.
 * `2 * side + menu + 2 * gap == available`.
 */
fun tabletopPadWidths(available: Float, gap: Float, menuRatio: Float = 0.6f): Triple<Float, Float, Float> {
    val side = ((available - 2f * gap) / (2f + menuRatio)).coerceAtLeast(0f)
    val menu = side * menuRatio
    return Triple(side, menu, side)
}

/** Which half gets the single page in book posture AlwaysSingle. */
fun bookSingleHalf(halves: FoldHalves, direction: ReadingDirection): IntRect =
    if (direction == ReadingDirection.LTR) halves.first else halves.second

fun bookSpreadGap(fold: IntRect): IntRect =
    IntRect(fold.left, fold.top, fold.right, fold.bottom)
