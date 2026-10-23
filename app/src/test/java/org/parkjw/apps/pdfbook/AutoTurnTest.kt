package org.parkjw.apps.pdfbook

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoTurnTest {
    @Test
    fun offersTheSpecifiedIntervals() {
        assertEquals(listOf(5, 10, 15, 20, 30, 45, 60), AUTO_TURN_INTERVAL_SECONDS)
    }

    @Test
    fun unknownIntervalFallsBackToDefault() {
        assertEquals(AUTO_TURN_DEFAULT_SECONDS, autoTurnSecondsOrDefault(0))
        assertEquals(AUTO_TURN_DEFAULT_SECONDS, autoTurnSecondsOrDefault(7))
        assertEquals(AUTO_TURN_DEFAULT_SECONDS, autoTurnSecondsOrDefault(120))
    }

    @Test
    fun keepsOfferedInterval() {
        assertEquals(30, autoTurnSecondsOrDefault(30))
        assertEquals(60, autoTurnSecondsOrDefault(60))
    }

    @Test
    fun intervalMillisUsesSeconds() {
        assertEquals(15_000L, autoTurnIntervalMillis(15))
        assertEquals(60_000L, autoTurnIntervalMillis(60))
        assertEquals(15_000L, autoTurnIntervalMillis(7))
    }

    @Test
    fun nextIndexAdvancesUntilTheLastSpread() {
        assertEquals(1, autoTurnNextIndex(0, 5))
        assertEquals(4, autoTurnNextIndex(3, 5))
        assertNull(autoTurnNextIndex(4, 5))
        assertNull(autoTurnNextIndex(0, 0))
        assertNull(autoTurnNextIndex(0, 1))
    }

    @Test
    fun schedulerWaitsOneFullIntervalBeforeTurning() = runTest {
        var turns = 0
        val job = launch { runAutoTurn(15_000L) { turns++; true } }
        runCurrent()
        advanceTimeBy(14_999)
        runCurrent()
        assertEquals(0, turns)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, turns)
        job.cancel()
    }

    @Test
    fun schedulerTurnsEveryInterval() = runTest {
        var turns = 0
        val job = launch { runAutoTurn(5_000L) { turns++; true } }
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(4, turns)
        job.cancel()
    }

    @Test
    fun schedulerStopsWhenTurnReportsTheEnd() = runTest {
        var turns = 0
        val job = launch { runAutoTurn(1_000L) { turns++; turns < 3 } }
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(3, turns)
        assertTrue(job.isCompleted)
    }
}
