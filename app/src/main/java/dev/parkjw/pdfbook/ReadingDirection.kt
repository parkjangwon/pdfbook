package dev.parkjw.pdfbook

enum class ReadingDirection {
    RTL,
    LTR,
}

fun spreadLayout(spread: List<Int>, direction: ReadingDirection): List<Int> {
    if (spread.size <= 1) return spread
    return when (direction) {
        ReadingDirection.LTR -> spread
        ReadingDirection.RTL -> spread.reversed()
    }
}
