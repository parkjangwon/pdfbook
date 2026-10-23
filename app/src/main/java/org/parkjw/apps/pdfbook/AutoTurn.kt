package org.parkjw.apps.pdfbook

import kotlinx.coroutines.delay

const val AUTO_TURN_DEFAULT_SECONDS = 15
const val AUTO_TURN_STATUS_MILLIS = 2_000L
val AUTO_TURN_INTERVAL_SECONDS = listOf(5, 10, 15, 20, 30, 45, 60)

sealed interface AutoTurnStatus {
    data class Running(val seconds: Int) : AutoTurnStatus
    data object Stopped : AutoTurnStatus
}

fun autoTurnSecondsOrDefault(seconds: Int): Int =
    if (seconds in AUTO_TURN_INTERVAL_SECONDS) seconds else AUTO_TURN_DEFAULT_SECONDS

fun autoTurnIntervalMillis(seconds: Int): Long = autoTurnSecondsOrDefault(seconds) * 1_000L

fun autoTurnNextIndex(currentIndex: Int, spreadCount: Int): Int? =
    if (spreadCount <= 0 || currentIndex >= spreadCount - 1) null else currentIndex + 1

suspend fun runAutoTurn(intervalMillis: Long, onTurn: () -> Boolean) {
    while (true) {
        delay(intervalMillis)
        if (!onTurn()) return
    }
}
