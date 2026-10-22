package org.parkjw.apps.pdfbook

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
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

const val CURL_RELEASE_THRESHOLD = 0.33f
const val CURL_BASE_DURATION_MS = 320

fun curlSide(direction: ReadingDirection, forward: Boolean): CurlSide =
    when (direction) {
        ReadingDirection.RTL -> if (forward) CurlSide.Left else CurlSide.Right
        ReadingDirection.LTR -> if (forward) CurlSide.Right else CurlSide.Left
    }

fun shouldCompleteCurl(progress: Float, threshold: Float = CURL_RELEASE_THRESHOLD): Boolean =
    progress >= threshold

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

fun resolvePageTurn(
    preference: PageTurnAnimation,
    twoPage: Boolean,
    zoomed: Boolean,
    nextPageCached: Boolean,
    animationsOff: Boolean,
    endNoticeOnly: Boolean,
): ResolvedPageTurn = when {
    endNoticeOnly -> ResolvedPageTurn.Instant
    animationsOff -> ResolvedPageTurn.Instant
    preference == PageTurnAnimation.None -> ResolvedPageTurn.Instant
    preference == PageTurnAnimation.Slide -> ResolvedPageTurn.Slide
    twoPage || zoomed || !nextPageCached -> ResolvedPageTurn.Slide
    else -> ResolvedPageTurn.Curl
}

data class CurlEffectState(
    val fromIndex: Int,
    val toIndex: Int,
    val side: CurlSide,
    val progress: Float,
    val dragging: Boolean,
)

data class CurlInterrupt(
    /** Page index the interrupted effect must land on before the next starts. */
    val finishToIndex: Int?,
    val next: CurlEffectState,
)

/** Finish the in-flight curl immediately, then queue the next turn. Never drops input. */
fun interruptCurl(
    current: CurlEffectState?,
    fromIndex: Int,
    toIndex: Int,
    side: CurlSide,
): CurlInterrupt = CurlInterrupt(
    finishToIndex = current?.toIndex,
    next = CurlEffectState(fromIndex, toIndex, side, progress = 0f, dragging = false),
)

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

fun curlTip(width: Float, height: Float, progress: Float, side: CurlSide): Offset {
    val p = progress.coerceIn(0f, 1f)
    return when (side) {
        CurlSide.Left -> Offset(width * p * 1.2f, height * (1f - p * 0.4f))
        CurlSide.Right -> Offset(width * (1f - p * 1.2f), height * (1f - p * 0.4f))
    }
}

/**
 * Fold is the perpendicular bisector of origin→tip, clipped to the page rect.
 * Returns null when progress is too small to form a fold.
 */
fun curlGeometry(width: Float, height: Float, progress: Float, side: CurlSide): CurlGeometry? {
    if (width <= 0f || height <= 0f || progress <= 0.001f) return null
    val origin = curlOrigin(width, height, side)
    val tip = curlTip(width, height, progress, side)
    val mid = Offset((origin.x + tip.x) / 2f, (origin.y + tip.y) / 2f)
    val dx = tip.x - origin.x
    val dy = tip.y - origin.y
    val len = hypot(dx, dy)
    if (len < 1f) return null
    // Perpendicular direction to the origin→tip segment.
    val nx = -dy / len
    val ny = dx / len
    val hits = mutableListOf<Offset>()
    intersectRayWithRect(mid, nx, ny, width, height)?.let { hits.add(it) }
    intersectRayWithRect(mid, -nx, -ny, width, height)?.let { hits.add(it) }
    if (hits.size < 2) return null
    val top = if (hits[0].y <= hits[1].y) hits[0] else hits[1]
    val bottom = if (hits[0].y <= hits[1].y) hits[1] else hits[0]
    return CurlGeometry(tip = tip, origin = origin, foldTop = top, foldBottom = bottom)
}

private fun intersectRayWithRect(
    origin: Offset,
    dirX: Float,
    dirY: Float,
    width: Float,
    height: Float,
): Offset? {
    var bestT = Float.POSITIVE_INFINITY
    var best: Offset? = null
    fun consider(t: Float, x: Float, y: Float) {
        if (t <= 0f || t >= bestT) return
        if (x < -0.5f || x > width + 0.5f || y < -0.5f || y > height + 0.5f) return
        bestT = t
        best = Offset(x.coerceIn(0f, width), y.coerceIn(0f, height))
    }
    if (dirX != 0f) {
        val t0 = (0f - origin.x) / dirX
        consider(t0, 0f, origin.y + t0 * dirY)
        val t1 = (width - origin.x) / dirX
        consider(t1, width, origin.y + t1 * dirY)
    }
    if (dirY != 0f) {
        val t0 = (0f - origin.y) / dirY
        consider(t0, origin.x + t0 * dirX, 0f)
        val t1 = (height - origin.y) / dirY
        consider(t1, origin.x + t1 * dirX, height)
    }
    return best
}

fun curlDurationMs(animatorDurationScale: Float): Long {
    if (animatorDurationScale <= 0f) return 0L
    return max(1L, (CURL_BASE_DURATION_MS * animatorDurationScale).toLong())
}

fun animationsOff(animatorDurationScale: Float): Boolean = animatorDurationScale <= 0f

fun clampCurlProgress(progress: Float): Float = min(1f, max(0f, progress))
