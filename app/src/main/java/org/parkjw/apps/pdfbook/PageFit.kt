package org.parkjw.apps.pdfbook

import kotlin.math.max
import kotlin.math.min
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
 * Extra layer scale that turns the ContentScale.Fit drawing of the page into native pixels.
 * Content is the *cropped* size: a crop changes the aspect the page must fill, so a width fit
 * has to zoom to the cropped width instead of leaving side bands or cutting text off.
 */
fun pageFitBaseScale(
    fit: PageFit,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Float {
    if (fit == PageFit.Screen || viewportWidth <= 0f || viewportHeight <= 0f) return 1f
    if (contentWidth <= 0f || contentHeight <= 0f) return 1f
    val inner = min(viewportWidth / contentWidth, viewportHeight / contentHeight)
    val target = when (fit) {
        PageFit.Width -> viewportWidth / contentWidth
        PageFit.Height -> viewportHeight / contentHeight
        PageFit.Screen -> inner
    }
    return (target / inner).coerceAtLeast(1f)
}

/**
 * Drawn size in screen px at scale 1: width fit fills the width, height fit fills the height,
 * so the scroll range and the start corner follow the cropped content.
 */
fun pageFitDrawnSize(
    fit: PageFit,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Pair<Float, Float> {
    if (fit == PageFit.Screen || viewportWidth <= 0f || viewportHeight <= 0f) {
        return contentWidth to contentHeight
    }
    if (contentWidth <= 0f || contentHeight <= 0f) return contentWidth to contentHeight
    val scale = when (fit) {
        PageFit.Width -> viewportWidth / contentWidth
        PageFit.Height -> viewportHeight / contentHeight
        PageFit.Screen -> 1f
    }
    return contentWidth * scale to contentHeight * scale
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

/** Start or end corner of the drawn page at an arbitrary zoom scale. */
fun pageFitAnchorState(
    fit: PageFit,
    direction: ReadingDirection,
    atEnd: Boolean,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): ZoomState {
    if (fit == PageFit.Screen) return ZoomState()
    val (x, y) = readingAnchorOffset(
        direction = direction,
        atEnd = atEnd,
        scale = scale,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
    )
    return ZoomState(scale, x, y)
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
    if (fit == PageFit.Screen) return ZoomState()
    return pageFitAnchorState(
        fit = fit,
        direction = direction,
        atEnd = false,
        scale = MinZoomScale,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
    )
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
    if (fit == PageFit.Screen) return ZoomState()
    return pageFitAnchorState(
        fit = fit,
        direction = direction,
        atEnd = true,
        scale = MinZoomScale,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
    )
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
