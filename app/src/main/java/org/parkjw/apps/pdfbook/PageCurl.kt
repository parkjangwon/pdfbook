package org.parkjw.apps.pdfbook

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation

/** Which bottom corner peels during a curl. */
enum class CurlSide {
    Left,
    Right,
}

enum class ResolvedPageTurn {
    Curl,
    Slide,
    Instant,
}

/** Distance rule for slow drags (also used when a turn is interrupted). */
const val CURL_RELEASE_THRESHOLD = 0.33f

/** Curl release rule from the v0.7.0 design mockup. */
const val CURL_RELEASE_PROGRESS = 0.25f

/** Release speed along the turn direction that completes the turn, in dp/s. */
const val CURL_FLING_DP_PER_SECOND = 1000f

const val CURL_BASE_DURATION_MS = 300
const val CURL_MIN_SETTLE_DURATION_MS = 150

/** Overlay is removed once the front page is within a pixel of the far edge. */
const val CURL_CLEAR_PROGRESS = 0.999f

/** Progress at which a second pointer settles the in-flight curl (nearest state). */
const val CURL_SETTLE_MIDPOINT = 0.5f

/** Maximum lift of the peeled corner at mid-turn, as a fraction of the height. */
const val CURL_TIP_LIFT = 0.18f

/** Flap and shadow values from the v0.7.0 mockup (normal mode defaults). */
const val CURL_MIRROR_ALPHA = 0.08f
const val CURL_SHADE_WIDTH_DP = 24f
const val CURL_SHADE_ALPHA = 0.10f
const val CURL_SHADOW_WIDTH_DP = 20f
const val CURL_SHADOW_ALPHA = 0.24f
const val CURL_SHADOW_MID_ALPHA = 0.10f
const val CURL_FOLD_HIGHLIGHT_DP = 1f
const val CURL_FOLD_HIGHLIGHT_ALPHA = 0.5f

val CurlEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

fun curlSide(direction: ReadingDirection, forward: Boolean): CurlSide =
    when (direction) {
        ReadingDirection.RTL -> if (forward) CurlSide.Left else CurlSide.Right
        ReadingDirection.LTR -> if (forward) CurlSide.Right else CurlSide.Left
    }

fun shouldCompleteCurl(progress: Float, threshold: Float = CURL_RELEASE_THRESHOLD): Boolean =
    progress >= threshold

/**
 * Complete-or-revert decision at release: a quarter turn or a fling at
 * [flingVelocityPx] along the turn direction completes, otherwise it reverts.
 */
fun curlReleaseCompletes(
    progress: Float,
    alongVelocityPx: Float,
    flingVelocityPx: Float,
    threshold: Float = CURL_RELEASE_PROGRESS,
): Boolean = progress >= threshold || alongVelocityPx >= flingVelocityPx

/** Release speed along the turn direction for a horizontal fling. */
fun curlAlongVelocity(velocityX: Float, side: CurlSide): Float =
    if (side == CurlSide.Left) velocityX else -velocityX

/** A second pointer settles to the nearest state instead of leaving the curl half-turned. */
fun curlSettleCompletes(progress: Float, midpoint: Float = CURL_SETTLE_MIDPOINT): Boolean =
    progress >= midpoint

/**
 * Maps a horizontal drag to curl progress 0..1.
 * Left-side curl advances when the finger moves right; right-side when it moves left.
 */
fun curlProgressFromDrag(dx: Float, width: Float, side: CurlSide): Float {
    if (width <= 0f) return 0f
    val along = when (side) {
        CurlSide.Left -> dx
        CurlSide.Right -> -dx
    }
    return (along / width).coerceIn(0f, 1f)
}

/**
 * Decides how a turn runs. Two-page view curls a full spread of two pages (and a lone
 * cover / last odd page onto its neighbour). It falls back to Slide when zoomed, when the
 * target spread is not cached, and when a two-page spread turns onto a one-page spread:
 * there is no facing page to paint on the flap's back face.
 *
 * [fromSpreadPages] and [toSpreadPages] are the real page counts of the spreads
 * (`currentSpreads[from].size` / `[to].size`), not positions. The single-page callers keep
 * the defaults and behave exactly as in v0.7.0.
 */
fun resolvePageTurn(
    preference: PageTurnAnimation,
    twoPage: Boolean,
    zoomed: Boolean,
    nextPageCached: Boolean,
    animationsOff: Boolean,
    endNoticeOnly: Boolean,
    fromSpreadPages: Int = 1,
    toSpreadPages: Int = 1,
): ResolvedPageTurn = when {
    endNoticeOnly -> ResolvedPageTurn.Instant
    animationsOff -> ResolvedPageTurn.Instant
    preference == PageTurnAnimation.None -> ResolvedPageTurn.Instant
    preference == PageTurnAnimation.Slide -> ResolvedPageTurn.Slide
    zoomed || !nextPageCached -> ResolvedPageTurn.Slide
    twoPage && fromSpreadPages >= 2 && toSpreadPages <= 1 -> ResolvedPageTurn.Slide
    else -> ResolvedPageTurn.Curl
}

data class CurlEffectState(
    val fromIndex: Int,
    val toIndex: Int,
    val side: CurlSide,
    val progress: Float,
    val dragging: Boolean,
    /** False while springing back to [fromIndex]. */
    val completing: Boolean = true,
)

/** Page the in-flight effect must settle on before a new turn starts. */
fun curlFinishIndex(current: CurlEffectState, liveProgress: Float = current.progress): Int = when {
    current.dragging -> if (shouldCompleteCurl(liveProgress)) current.toIndex else current.fromIndex
    current.completing -> current.toIndex
    else -> current.fromIndex
}

data class CurlGeometry(
    val tip: Offset,
    val origin: Offset,
    val foldTop: Offset,
    val foldBottom: Offset,
)

fun curlOrigin(width: Float, height: Float, side: CurlSide): Offset =
    when (side) {
        CurlSide.Left -> Offset(0f, height)
        CurlSide.Right -> Offset(width, height)
    }

fun curlTipX(width: Float, progress: Float, side: CurlSide): Float {
    val p = progress.coerceIn(0f, 1f)
    return when (side) {
        CurlSide.Left -> width * 2f * p
        CurlSide.Right -> width * (1f - 2f * p)
    }
}

fun curlTipY(height: Float, progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return height - CURL_TIP_LIFT * height * sin(PI.toFloat() * p)
}

/** Peeled corner; at progress 1 it is one page width past the far edge. */
fun curlTip(width: Float, height: Float, progress: Float, side: CurlSide): Offset =
    Offset(curlTipX(width, progress, side), curlTipY(height, progress))

/**
 * Reusable curl vertices: the front page, the revealed next page and the flap
 * (the reflected peeled part), plus the drawn fold line. Filled in place so the
 * overlay allocates nothing per frame.
 */
class CurlShape {
    var tipX = 0f
    var tipY = 0f
    var originX = 0f
    var originY = 0f
    var midX = 0f
    var midY = 0f

    /** Unit normal of the fold line, pointing from the origin toward the far edge. */
    var normalX = 0f
    var normalY = 0f
    var foldTopX = 0f
    var foldTopY = 0f
    var foldBottomX = 0f
    var foldBottomY = 0f
    var foldValid = false

    val front = FloatArray(CURL_MAX_VERTS * 2)
    var frontCount = 0
    val revealed = FloatArray(CURL_MAX_VERTS * 2)
    var revealedCount = 0
    val flap = FloatArray(CURL_MAX_VERTS * 2)
    var flapCount = 0

    internal val scratchA = FloatArray(CURL_MAX_VERTS * 2)
    internal val scratchB = FloatArray(CURL_MAX_VERTS * 2)
    internal val pageRect = FloatArray(8)
}

const val CURL_MAX_VERTS = 12

/**
 * Fills [out] with the curl polygons for [progress]; no allocation.
 *
 * [spreadFlap] widens only the flap clip from the sheet rect to the whole spread rect
 * (sheet and the page across the spine). The fold and tip math is unchanged, so at
 * progress 1 the flap lands exactly on the opposite page. The default keeps the
 * single-sheet output byte-identical.
 *
 * [bounds] is the clip rect `[left, top, right, bottom]` in the page's local coordinates and
 * defaults to the page rect. A two-page turn passes the union of its four page rects, so the
 * fold line reaches the rows of a taller landing page and the flap reaches the rows of a
 * taller back face. The tip and fold math still use [width] and [height], the turning sheet.
 */
fun updateCurlShape(
    width: Float,
    height: Float,
    progress: Float,
    side: CurlSide,
    out: CurlShape,
    spreadFlap: Boolean = false,
    bounds: FloatArray? = null,
) {
    out.frontCount = 0
    out.revealedCount = 0
    out.flapCount = 0
    out.foldValid = false
    if (width <= 0f || height <= 0f) return
    val p = clampCurlProgress(progress)
    val boundLeft: Float
    val boundTop: Float
    val boundRight: Float
    val boundBottom: Float
    if (bounds != null) {
        boundLeft = bounds[0]
        boundTop = bounds[1]
        boundRight = bounds[2]
        boundBottom = bounds[3]
    } else {
        boundLeft = 0f
        boundTop = 0f
        boundRight = width
        boundBottom = height
    }

    val originX = if (side == CurlSide.Left) 0f else width
    val originY = height
    val tipX = curlTipX(width, p, side)
    val tipY = curlTipY(height, p)
    out.originX = originX
    out.originY = originY
    out.tipX = tipX
    out.tipY = tipY

    val dx = tipX - originX
    val dy = tipY - originY
    val len = hypot(dx, dy)
    if (len < 1f) {
        fillPageRect(boundLeft, boundTop, boundRight, boundBottom, out.front)
        out.frontCount = 4
        return
    }

    val midX = (originX + tipX) / 2f
    val midY = (originY + tipY) / 2f
    out.midX = midX
    out.midY = midY
    out.normalX = dx / len
    out.normalY = dy / len

    val ends = collectFoldEnds(
        midX,
        midY,
        -dy / len,
        dx / len,
        boundLeft,
        boundTop,
        boundRight,
        boundBottom,
        out.pageRect,
    )
    if (ends >= 2) {
        val aY = out.pageRect[1]
        val bY = out.pageRect[3]
        val topIsA = aY <= bY
        out.foldTopX = out.pageRect[if (topIsA) 0 else 2]
        out.foldTopY = aY.coerceAtMost(bY)
        out.foldBottomX = out.pageRect[if (topIsA) 2 else 0]
        out.foldBottomY = aY.coerceAtLeast(bY)
        out.foldValid = true
    }

    fillPageRect(boundLeft, boundTop, boundRight, boundBottom, out.scratchA)
    // Front page: the part on the far side of the fold from the origin.
    val frontCount = clipHalfPlane(out.scratchA, 4, out.front, midX, midY, dx, dy, keepPositive = true)
    out.frontCount = clipRect(
        frontCount,
        out.front,
        out.scratchA,
        out.scratchB,
        boundLeft,
        boundTop,
        boundRight,
        boundBottom,
        out.front,
    )

    // Revealed next page: the part the flap uncovers.
    fillPageRect(boundLeft, boundTop, boundRight, boundBottom, out.scratchA)
    val revealedCount = clipHalfPlane(out.scratchA, 4, out.scratchB, midX, midY, dx, dy, keepPositive = false)
    out.revealedCount = clipRect(
        revealedCount,
        out.scratchB,
        out.scratchA,
        out.revealed,
        boundLeft,
        boundTop,
        boundRight,
        boundBottom,
        out.revealed,
    )

    // Flap: the peeled part reflected across the fold, clipped to the sheet or, for a
    // spread, to the page across the spine as well.
    val reflected = reflectAcrossFold(
        out.revealed,
        out.revealedCount,
        midX,
        midY,
        dx,
        dy,
        len,
        out.scratchA,
    )
    val flapLeft: Float
    val flapTop: Float
    val flapRight: Float
    val flapBottom: Float
    if (bounds != null) {
        // The flap sweeps across the turning half and lands on the opposite half, so its clip
        // covers the bounds and their mirror about the spine (local 0 for Right, width for Left).
        val mirrorLeft = if (side == CurlSide.Right) -boundRight else 2f * width - boundRight
        val mirrorRight = if (side == CurlSide.Right) -boundLeft else 2f * width - boundLeft
        flapLeft = min(boundLeft, mirrorLeft)
        flapTop = boundTop
        flapRight = max(boundRight, mirrorRight)
        flapBottom = boundBottom
    } else {
        flapLeft = if (spreadFlap && side == CurlSide.Right) -width else 0f
        flapTop = 0f
        flapRight = if (spreadFlap && side == CurlSide.Left) 2f * width else width
        flapBottom = height
    }
    out.flapCount = clipRect(
        reflected,
        out.scratchA,
        out.scratchB,
        out.scratchA,
        flapLeft,
        flapTop,
        flapRight,
        flapBottom,
        out.flap,
    )
}

/**
 * Fold line (perpendicular bisector of origin→tip) clipped to the page.
 * Returns null when progress is too small to form a fold.
 */
fun curlGeometry(width: Float, height: Float, progress: Float, side: CurlSide): CurlGeometry? {
    if (width <= 0f || height <= 0f || progress <= 0.001f) return null
    val shape = CurlShape()
    updateCurlShape(width, height, progress, side, shape)
    if (!shape.foldValid) return null
    return CurlGeometry(
        tip = Offset(shape.tipX, shape.tipY),
        origin = Offset(shape.originX, shape.originY),
        foldTop = Offset(shape.foldTopX, shape.foldTopY),
        foldBottom = Offset(shape.foldBottomX, shape.foldBottomY),
    )
}

/** Shadow band width as a fraction of its maximum: full until 80%, gone at the end. */
fun curlShadowWidthFraction(progress: Float): Float =
    min(1f, (1f - clampCurlProgress(progress)) / 0.2f)

/** Drawn front-page area as a fraction of the page; shrinks monotonically to 0. */
fun curlFrontVisibleFraction(
    width: Float,
    height: Float,
    progress: Float,
    side: CurlSide,
): Float {
    if (width <= 0f || height <= 0f) return 0f
    val shape = CurlShape()
    updateCurlShape(width, height, progress, side, shape)
    return (polygonArea(shape.front, shape.frontCount) / (width * height)).coerceIn(0f, 1f)
}

private fun fillPageRect(left: Float, top: Float, right: Float, bottom: Float, out: FloatArray) {
    out[0] = left
    out[1] = top
    out[2] = right
    out[3] = top
    out[4] = right
    out[5] = bottom
    out[6] = left
    out[7] = bottom
}

/**
 * Collects the two ends of the fold line inside the bounds rect (as x,y pairs).
 * The fold always passes through an interior point, so there are two.
 */
private fun collectFoldEnds(
    midX: Float,
    midY: Float,
    nx: Float,
    ny: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    out: FloatArray,
): Int {
    var count = 0
    fun add(x: Float, y: Float) {
        for (i in 0 until count) {
            if (abs(out[i * 2] - x) < 0.01f && abs(out[i * 2 + 1] - y) < 0.01f) return
        }
        if (count < 4) {
            out[count * 2] = x
            out[count * 2 + 1] = y
            count++
        }
    }
    if (nx != 0f) {
        val t = (left - midX) / nx
        val y = midY + t * ny
        if (y >= top - 0.01f && y <= bottom + 0.01f) add(left, y.coerceIn(top, bottom))
        val t2 = (right - midX) / nx
        val y2 = midY + t2 * ny
        if (y2 >= top - 0.01f && y2 <= bottom + 0.01f) add(right, y2.coerceIn(top, bottom))
    }
    if (ny != 0f) {
        val t = (top - midY) / ny
        val x = midX + t * nx
        if (x >= left - 0.01f && x <= right + 0.01f) add(x.coerceIn(left, right), top)
        val t2 = (bottom - midY) / ny
        val x2 = midX + t2 * nx
        if (x2 >= left - 0.01f && x2 <= right + 0.01f) add(x2.coerceIn(left, right), bottom)
    }
    return count
}

/** Writes the part of [src] where `(point - p) · n` has the [keepPositive] sign. */
private fun clipHalfPlane(
    src: FloatArray,
    count: Int,
    dst: FloatArray,
    px: Float,
    py: Float,
    nx: Float,
    ny: Float,
    keepPositive: Boolean,
): Int {
    if (count <= 0) return 0
    var out = 0
    var ax = src[(count - 1) * 2]
    var ay = src[(count - 1) * 2 + 1]
    var da = (ax - px) * nx + (ay - py) * ny
    var aIn = if (keepPositive) da >= 0f else da <= 0f
    for (i in 0 until count) {
        val bx = src[i * 2]
        val by = src[i * 2 + 1]
        val db = (bx - px) * nx + (by - py) * ny
        val bIn = if (keepPositive) db >= 0f else db <= 0f
        if (aIn != bIn) {
            val t = da / (da - db)
            dst[out * 2] = ax + (bx - ax) * t
            dst[out * 2 + 1] = ay + (by - ay) * t
            out++
        }
        if (bIn) {
            dst[out * 2] = bx
            dst[out * 2 + 1] = by
            out++
        }
        ax = bx
        ay = by
        da = db
        aIn = bIn
    }
    return out
}

private fun clipRect(
    count: Int,
    src: FloatArray,
    bufA: FloatArray,
    bufB: FloatArray,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    out: FloatArray,
): Int {
    var current = clipHalfPlane(src, count, bufA, left, top, 1f, 0f, keepPositive = true)
    current = clipHalfPlane(bufA, current, bufB, right, top, -1f, 0f, keepPositive = true)
    current = clipHalfPlane(bufB, current, bufA, left, top, 0f, 1f, keepPositive = true)
    current = clipHalfPlane(bufA, current, bufB, left, bottom, 0f, -1f, keepPositive = true)
    if (bufB !== out) {
        for (i in 0 until current * 2) out[i] = bufB[i]
    }
    return current
}

private fun reflectAcrossFold(
    src: FloatArray,
    count: Int,
    midX: Float,
    midY: Float,
    dx: Float,
    dy: Float,
    len: Float,
    out: FloatArray,
): Int {
    val inv = 2f / (len * len)
    for (i in 0 until count) {
        val x = src[i * 2]
        val y = src[i * 2 + 1]
        val dot = ((x - midX) * dx + (y - midY) * dy) * inv
        out[i * 2] = x - dot * dx
        out[i * 2 + 1] = y - dot * dy
    }
    return count
}

fun polygonArea(points: FloatArray, count: Int): Float {
    if (count < 3) return 0f
    var sum = 0f
    for (i in 0 until count) {
        val j = (i + 1) % count
        sum += points[i * 2] * points[j * 2 + 1] - points[j * 2] * points[i * 2 + 1]
    }
    return abs(sum) / 2f
}

data class PixelCrop(val left: Int, val top: Int, val width: Int, val height: Int)

/** Maps a fractional source rect (0..1) onto bitmap pixels; null keeps the whole bitmap. */
fun sourceCropPixels(srcWidth: Int, srcHeight: Int, crop: Rect?): PixelCrop {
    if (srcWidth <= 0 || srcHeight <= 0) return PixelCrop(0, 0, 0, 0)
    if (crop == null) return PixelCrop(0, 0, srcWidth, srcHeight)
    val left = (crop.left.coerceIn(0f, 1f) * srcWidth).roundToInt().coerceIn(0, srcWidth - 1)
    val top = (crop.top.coerceIn(0f, 1f) * srcHeight).roundToInt().coerceIn(0, srcHeight - 1)
    val right = (crop.right.coerceIn(0f, 1f) * srcWidth).roundToInt().coerceIn(left + 1, srcWidth)
    val bottom = (crop.bottom.coerceIn(0f, 1f) * srcHeight).roundToInt().coerceIn(top + 1, srcHeight)
    return PixelCrop(left, top, right - left, bottom - top)
}

fun curlDurationMs(animatorDurationScale: Float): Long {
    if (animatorDurationScale <= 0f) return 0L
    return max(1L, (CURL_BASE_DURATION_MS * animatorDurationScale).toLong())
}

/**
 * Remaining travel in max(150ms, 300ms × remaining), scaled by the animator
 * duration scale; short releases no longer snap back within a few frames.
 */
fun curlRemainingDurationMs(
    animatorDurationScale: Float,
    fromProgress: Float,
    animateForward: Boolean,
): Long {
    if (animatorDurationScale <= 0f) return 0L
    val remaining = if (animateForward) 1f - fromProgress else fromProgress
    val ms = max(
        CURL_MIN_SETTLE_DURATION_MS.toFloat(),
        CURL_BASE_DURATION_MS * remaining.coerceIn(0f, 1f),
    )
    return max(1L, (ms * animatorDurationScale).toLong())
}

fun animationsOff(animatorDurationScale: Float): Boolean = animatorDurationScale <= 0f

fun clampCurlProgress(progress: Float): Float = min(1f, max(0f, progress))

/** Pager offset that follows the finger when the next page is not cached (Slide fallback). */
fun curlSlideOffset(progress: Float, forward: Boolean): Float =
    clampCurlProgress(progress) * 0.5f * if (forward) 1f else -1f

/**
 * Compensates drag progress for the slide-follow displacement, which moves the
 * pointer's local coordinates and would otherwise shrink the measured progress.
 */
fun curlFollowProgress(raw: Float, followOffset: Float): Float =
    clampCurlProgress(raw + abs(followOffset))

data class FittedPageRect(val left: Float, val top: Float, val width: Float, val height: Float)

/** Destination rect for ContentScale.Fit inside a canvas. */
fun fittedPageRect(srcW: Int, srcH: Int, dstW: Float, dstH: Float): FittedPageRect? {
    if (srcW <= 0 || srcH <= 0 || dstW <= 0f || dstH <= 0f) return null
    val scale = minOf(dstW / srcW, dstH / srcH)
    val dw = srcW * scale
    val dh = srcH * scale
    return FittedPageRect((dstW - dw) / 2f, (dstH - dh) / 2f, dw, dh)
}
