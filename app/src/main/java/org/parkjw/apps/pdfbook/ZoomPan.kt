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
    if (scale <= 1f || viewportWidth <= 0f || viewportHeight <= 0f) {
        return 0f to 0f
    }
    val maxX = ((contentWidth * scale - viewportWidth) / 2f).coerceAtLeast(0f)
    val maxY = ((contentHeight * scale - viewportHeight) / 2f).coerceAtLeast(0f)
    return x.coerceIn(-maxX, maxX) to y.coerceIn(-maxY, maxY)
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

fun ZoomState.toggleDoubleTap(
    targetScale: Float = DoubleTapZoomScale,
): ZoomState {
    return if (scale > MinZoomScale) {
        ZoomState()
    } else {
        ZoomState(scale = targetScale.coerceIn(MinZoomScale, MaxZoomScale))
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

fun ZoomState.atReadingStart(
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
): ZoomState {
    if (scale <= MinZoomScale) return ZoomState()
    val maxX = viewportWidth * (scale - 1f) / 2f
    val maxY = viewportHeight * (scale - 1f) / 2f
    val x = if (direction == ReadingDirection.RTL) -maxX else maxX
    return ZoomState(scale, x, maxY).clampToViewport(viewportWidth, viewportHeight)
}

fun ZoomState.afterPageTurn(
    keepZoom: Boolean,
    direction: ReadingDirection,
    viewportWidth: Float,
    viewportHeight: Float,
): ZoomState =
    if (keepZoom) atReadingStart(direction, viewportWidth, viewportHeight) else ZoomState()
