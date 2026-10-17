package org.parkjw.apps.pdfbook

enum class ReadingDirection {
    RTL,
    LTR,
}

fun toggledDirection(direction: ReadingDirection): ReadingDirection =
    when (direction) {
        ReadingDirection.RTL -> ReadingDirection.LTR
        ReadingDirection.LTR -> ReadingDirection.RTL
    }

fun spreadLayout(spread: List<Int>, direction: ReadingDirection): List<Int> {
    if (spread.size <= 1) return spread
    return when (direction) {
        ReadingDirection.LTR -> spread
        ReadingDirection.RTL -> spread.reversed()
    }
}
