package org.parkjw.apps.pdfbook

import androidx.compose.runtime.saveable.Saver

enum class ReadingDirection {
    RTL,
    LTR,
}

val ReadingDirectionSaver = Saver<ReadingDirection, String>(
    save = { it.name },
    restore = { ReadingDirection.valueOf(it) },
)

fun toggledDirection(direction: ReadingDirection): ReadingDirection =
    when (direction) {
        ReadingDirection.RTL -> ReadingDirection.LTR
        ReadingDirection.LTR -> ReadingDirection.RTL
    }

fun resolveReadingDirection(
    saved: ReadingDirection?,
    default: ReadingDirection,
): ReadingDirection = saved ?: default

fun spreadLayout(spread: List<Int>, direction: ReadingDirection): List<Int> {
    if (spread.size <= 1) return spread
    return when (direction) {
        ReadingDirection.LTR -> spread
        ReadingDirection.RTL -> spread.reversed()
    }
}
