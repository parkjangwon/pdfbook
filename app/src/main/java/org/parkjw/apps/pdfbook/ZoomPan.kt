package org.parkjw.apps.pdfbook

const val MinZoomScale = 1f
const val MaxZoomScale = 4f
const val DoubleTapZoomScale = 2f

data class ZoomState(
    val scale: Float = MinZoomScale,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    val allowsPageTurn: Boolean
        get() = scale <= MinZoomScale
}

fun clampZoomOffset(
    x: Float,
    y: Float,
    scale: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): Pair<Float, Float> {
    if (scale <= 1f || viewportWidth <= 0f || viewportHeight <= 0f) {
        return 0f to 0f
    }
    val maxX = viewportWidth * (scale - 1f) / 2f
    val maxY = viewportHeight * (scale - 1f) / 2f
    return x.coerceIn(-maxX, maxX) to y.coerceIn(-maxY, maxY)
}

fun ZoomState.applyPinch(
    zoomChange: Float,
    centroidX: Float,
    centroidY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): ZoomState {
    val newScale = (scale * zoomChange).coerceIn(MinZoomScale, MaxZoomScale)
    if (newScale == scale) {
        return clampToViewport(viewportWidth, viewportHeight)
    }
    val ratio = newScale / scale
    val newOffsetX = offsetX * ratio + (centroidX - viewportWidth / 2f) * (1f - ratio)
    val newOffsetY = offsetY * ratio + (centroidY - viewportHeight / 2f) * (1f - ratio)
    return ZoomState(newScale, newOffsetX, newOffsetY)
        .clampToViewport(viewportWidth, viewportHeight)
}

fun ZoomState.applyPan(
    deltaX: Float,
    deltaY: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): ZoomState {
    if (allowsPageTurn) {
        return this
    }
    return copy(offsetX = offsetX + deltaX, offsetY = offsetY + deltaY)
        .clampToViewport(viewportWidth, viewportHeight)
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
): ZoomState {
    val (x, y) = clampZoomOffset(offsetX, offsetY, scale, viewportWidth, viewportHeight)
    return copy(offsetX = x, offsetY = y)
}
