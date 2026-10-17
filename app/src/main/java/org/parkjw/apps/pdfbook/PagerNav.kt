package org.parkjw.apps.pdfbook

import kotlin.math.roundToInt

fun pagerTargetIndex(targetPage: Int, delta: Int, pageCount: Int): Int {
    if (pageCount <= 0) return 0
    return (targetPage + delta).coerceIn(0, pageCount - 1)
}

fun pageToSlider(page: Int, pageCount: Int, direction: ReadingDirection): Float {
    if (pageCount <= 1) return 0f
    val index = (page - 1).coerceIn(0, pageCount - 1)
    return when (direction) {
        ReadingDirection.LTR -> index.toFloat()
        ReadingDirection.RTL -> (pageCount - 1 - index).toFloat()
    }
}

fun sliderToPage(slider: Float, pageCount: Int, direction: ReadingDirection): Int {
    if (pageCount <= 1) return 1
    val index = slider.roundToInt().coerceIn(0, pageCount - 1)
    return when (direction) {
        ReadingDirection.LTR -> index + 1
        ReadingDirection.RTL -> pageCount - index
    }
}
