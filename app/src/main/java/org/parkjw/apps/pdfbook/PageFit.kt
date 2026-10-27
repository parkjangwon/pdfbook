package org.parkjw.apps.pdfbook

import kotlin.math.max
import kotlin.math.roundToInt

/** How one page is scaled into the reading area. Two-page view always uses [Screen]. */
enum class PageFit {
    Screen,
    Width,
    Height,
}

fun effectivePageFit(fit: PageFit, twoPage: Boolean): PageFit =
    if (twoPage) PageFit.Screen else fit

fun pageFitOrDefault(raw: String?): PageFit =
    PageFit.entries.firstOrNull { it.name == raw } ?: PageFit.Screen

/** One in-page step covers a screen minus [PageFitScreenFraction]'s complement (10% overlap). */
const val PageFitScreenFraction = 0.9f

/** Cap on a fit render's unconstrained axis, so a strip page cannot allocate a huge bitmap. */
const val PageFitMaxAspect = 3f

private const val FitEpsilonPx = 0.5f

fun pageFitOverflows(
    fit: PageFit,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Boolean = when (fit) {
    PageFit.Screen -> false
    PageFit.Width -> contentHeight > viewportHeight + FitEpsilonPx
    PageFit.Height -> contentWidth > viewportWidth + FitEpsilonPx
}

/** Render request size: the overflowing axis is left open up to [PageFitMaxAspect]. */
fun pageFitRenderSize(fit: PageFit, slotWidth: Int, slotHeight: Int): Pair<Int, Int> = when (fit) {
    PageFit.Screen -> slotWidth to slotHeight
    PageFit.Width -> slotWidth to (slotWidth * PageFitMaxAspect).roundToInt().coerceAtLeast(1)
    PageFit.Height -> (slotHeight * PageFitMaxAspect).roundToInt().coerceAtLeast(1) to slotHeight
}

/**
 * Scale that turns the ContentScale.Fit drawing of a fit-rendered bitmap back into its native
 * pixels. 1 for a page that already fits the screen.
 */
fun pageFitBaseScale(
    fit: PageFit,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Float {
    if (fit == PageFit.Screen || viewportWidth <= 0f || viewportHeight <= 0f) return 1f
    return max(contentWidth / viewportWidth, contentHeight / viewportHeight).coerceAtLeast(1f)
}

fun pageFitMaxOffset(
    fit: PageFit,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Pair<Float, Float> = when (fit) {
    PageFit.Screen -> 0f to 0f
    PageFit.Width -> 0f to (max(0f, (contentHeight - viewportHeight) / 2f))
    PageFit.Height -> (max(0f, (contentWidth - viewportWidth) / 2f)) to 0f
}

/** Opening position: reading-direction top corner (RTL right, LTR left). */
fun pageFitStartState(
    fit: PageFit,
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): ZoomState {
    val (maxX, maxY) = pageFitMaxOffset(fit, viewportWidth, viewportHeight, contentWidth, contentHeight)
    val x = if (fit == PageFit.Height) {
        if (direction == ReadingDirection.RTL) -maxX else maxX
    } else {
        0f
    }
    return ZoomState(offsetX = x, offsetY = maxY)
}

/** Last position on the page: where a page opened backwards (previous page) starts. */
fun pageFitEndState(
    fit: PageFit,
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): ZoomState {
    val (maxX, maxY) = pageFitMaxOffset(fit, viewportWidth, viewportHeight, contentWidth, contentHeight)
    val x = if (fit == PageFit.Height) {
        if (direction == ReadingDirection.RTL) maxX else -maxX
    } else {
        0f
    }
    return ZoomState(offsetX = x, offsetY = -maxY)
}

/**
 * Moves one screen along the overflowing axis. Returns the same state at the end of the page,
 * which is the signal for the caller to turn the page instead.
 */
fun ZoomState.scrollWithinPage(
    fit: PageFit,
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
    forward: Boolean,
): ZoomState {
    if (fit == PageFit.Screen) return this
    val (maxX, maxY) = pageFitMaxOffset(fit, viewportWidth, viewportHeight, contentWidth, contentHeight)
    val pageStepY = viewportHeight * PageFitScreenFraction
    val pageStepX = viewportWidth * PageFitScreenFraction
    val stepY = when {
        fit != PageFit.Width -> 0f
        forward -> -pageStepY
        else -> pageStepY
    }
    val stepX = when {
        fit != PageFit.Height -> 0f
        forward == (direction == ReadingDirection.RTL) -> pageStepX
        else -> -pageStepX
    }
    return copy(
        offsetX = (offsetX + stepX).coerceIn(-maxX, maxX),
        offsetY = (offsetY + stepY).coerceIn(-maxY, maxY),
    )
}
