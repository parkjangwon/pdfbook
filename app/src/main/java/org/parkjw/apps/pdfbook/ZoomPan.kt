package org.parkjw.apps.pdfbook

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
