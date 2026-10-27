package org.parkjw.apps.pdfbook

const val MinZoomScale = 1f
const val MaxZoomScale = 4f
const val DoubleTapZoomScale = 2f
const val PinchConsumeThreshold = 0.02f

data class ZoomState(
    val scale: Float = MinZoomScale,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    val allowsPageTurn: Boolean
        get() = scale <= MinZoomScale
}

data class PinchLatchState(
    val cumulativeZoom: Float = 1f,
    val latched: Boolean = false,
)

fun PinchLatchState.advance(
    zoomChange: Float,
    threshold: Float = PinchConsumeThreshold,
): PinchLatchState {
    val next = cumulativeZoom * zoomChange
    return PinchLatchState(
        cumulativeZoom = next,
        latched = latched || kotlin.math.abs(next - 1f) >= threshold,
    )
}

fun clampZoomOffset(
    x: Float,
    y: Float,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): Pair<Float, Float> = clampZoomOffset(x, y, scale, viewportWidth, viewportHeight, viewportWidth, viewportHeight)

/**
 * Clamps against the drawn content size instead of the viewport. A page fit to its width is
 * already taller than the viewport at scale 1, so its scroll range must come from the bitmap.
 */
fun clampZoomOffset(
    x: Float,
    y: Float,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Pair<Float, Float> {
    if (viewportWidth <= 0f || viewportHeight <= 0f) {
        return 0f to 0f
    }
    val maxX = ((contentWidth * scale - viewportWidth) / 2f).coerceAtLeast(0f)
    val maxY = ((contentHeight * scale - viewportHeight) / 2f).coerceAtLeast(0f)
    // + 0f normalizes a -0.0 bound back to 0.0.
    return (x.coerceIn(-maxX, maxX) + 0f) to (y.coerceIn(-maxY, maxY) + 0f)
}

/** Reading-direction top corner (RTL right, LTR left), or the bottom/other corner when [atEnd]. */
internal fun readingAnchorOffset(
    direction: ReadingDirection,
    atEnd: Boolean,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float,
): Pair<Float, Float> {
    val maxX = ((contentWidth * scale - viewportWidth) / 2f).coerceAtLeast(0f)
    val maxY = ((contentHeight * scale - viewportHeight) / 2f).coerceAtLeast(0f)
    val forwardX = if (direction == ReadingDirection.RTL) -maxX else maxX
    // + 0f normalizes -0.0 back to 0.0 so equal anchors compare equal.
    val x = if (atEnd) -forwardX else forwardX
    val y = if (atEnd) -maxY else maxY
    return (x + 0f) to (y + 0f)
}

fun ZoomState.applyPinch(
    zoomChange: Float,
    centroidX: Float,
    centroidY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
): ZoomState {
    val newScale = (scale * zoomChange).coerceIn(MinZoomScale, MaxZoomScale)
    if (newScale == scale) {
        return clampToViewport(viewportWidth, viewportHeight, contentWidth, contentHeight)
    }
    val ratio = newScale / scale
    val newOffsetX = offsetX * ratio + (centroidX - viewportWidth / 2f) * (1f - ratio)
    val newOffsetY = offsetY * ratio + (centroidY - viewportHeight / 2f) * (1f - ratio)
    return ZoomState(newScale, newOffsetX, newOffsetY)
        .clampToViewport(viewportWidth, viewportHeight, contentWidth, contentHeight)
}

fun ZoomState.applyPan(
    deltaX: Float,
    deltaY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
    allowPanAtBaseScale: Boolean = false,
): ZoomState {
    if (allowsPageTurn && !allowPanAtBaseScale) {
        return this
    }
    return copy(offsetX = offsetX + deltaX, offsetY = offsetY + deltaY)
        .clampToViewport(viewportWidth, viewportHeight, contentWidth, contentHeight)
}

fun ZoomState.applyGesture(
    zoomChange: Float,
    centroidX: Float,
    centroidY: Float,
    panX: Float,
    panY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
    allowPanAtBaseScale: Boolean = false,
): ZoomState {
    val afterZoom = if (zoomChange == 1f) {
        this
    } else {
        applyPinch(zoomChange, centroidX, centroidY, viewportWidth, viewportHeight, contentWidth, contentHeight)
    }
    return afterZoom.applyPan(
        panX,
        panY,
        viewportWidth,
        viewportHeight,
        contentWidth,
        contentHeight,
        allowPanAtBaseScale,
    )
}

/**
 * Zooms around the viewport centre, keeping the current reading position inside the drawn
 * content. Zooming back out keeps the position instead of dropping to the page centre, which
 * matters for a fit page that already overflows at scale 1.
 */
fun ZoomState.toggleDoubleTap(
    targetScale: Float = DoubleTapZoomScale,
    viewportWidth: Float = 0f,
    viewportHeight: Float = 0f,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
): ZoomState {
    return if (scale > MinZoomScale) {
        copy(scale = MinZoomScale).clampToViewport(viewportWidth, viewportHeight, contentWidth, contentHeight)
    } else {
        applyPinch(
            zoomChange = targetScale.coerceIn(MinZoomScale, MaxZoomScale) / scale.coerceAtLeast(MinZoomScale),
            centroidX = viewportWidth / 2f,
            centroidY = viewportHeight / 2f,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
        )
    }
}

fun ZoomState.reset(): ZoomState = ZoomState()

fun ZoomState.clampToViewport(
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
): ZoomState {
    val (x, y) = clampZoomOffset(offsetX, offsetY, scale, viewportWidth, viewportHeight, contentWidth, contentHeight)
    return copy(offsetX = x, offsetY = y)
}

/**
 * Reading-direction top corner at the current scale. Content defaults to the viewport, which is
 * the old behaviour; a fit page passes its drawn size so the corner follows the whole page.
 */
fun ZoomState.atReadingStart(
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
): ZoomState {
    if (scale <= MinZoomScale &&
        contentWidth <= viewportWidth + 0.5f &&
        contentHeight <= viewportHeight + 0.5f
    ) {
        return ZoomState()
    }
    val (x, y) = readingAnchorOffset(
        direction,
        atEnd = false,
        scale = scale,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        contentWidth = contentWidth,
        contentHeight = contentHeight,
    )
    return ZoomState(scale, x, y)
}

fun ZoomState.afterPageTurn(
    keepZoom: Boolean,
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
): ZoomState =
    if (keepZoom) {
        atReadingStart(direction, viewportWidth, viewportHeight, contentWidth, contentHeight)
    } else {
        ZoomState()
    }
