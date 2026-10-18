package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class DoubleBackExitTest {
    @Test
    fun firstPressShowsHint() {
        val gate = DoubleBackExit()
        assertEquals(DoubleBackDecision.ShowHint, gate.onBack(1_000L))
    }

    @Test
    fun secondPressInsideWindowExits() {
        val gate = DoubleBackExit()
        gate.onBack(1_000L)
        assertEquals(DoubleBackDecision.Exit, gate.onBack(2_999L))
    }

    @Test
    fun pressAtWindowBoundaryExits() {
        val gate = DoubleBackExit()
        gate.onBack(1_000L)
        assertEquals(DoubleBackDecision.Exit, gate.onBack(3_000L))
    }

    @Test
    fun pressAfterWindowShowsHintAgain() {
        val gate = DoubleBackExit()
        gate.onBack(1_000L)
        assertEquals(DoubleBackDecision.ShowHint, gate.onBack(3_001L))
        assertEquals(DoubleBackDecision.Exit, gate.onBack(5_001L))
    }

    @Test
    fun resetTreatsNextPressAsFirst() {
        val gate = DoubleBackExit()
        gate.onBack(1_000L)
        gate.reset()
        assertEquals(DoubleBackDecision.ShowHint, gate.onBack(1_500L))
    }

    @Test
    fun rootFolderListOnlyWhenShelfIsShowing() {
        assertEquals(
            true,
            showsRootFolderList(
                showSettings = false,
                showLost = false,
                loaded = true,
                readerOpen = false,
                contentPhase = FolderContentPhase.None,
                folderCount = 1,
            ),
        )
        assertEquals(
            false,
            showsRootFolderList(
                showSettings = false,
                showLost = false,
                loaded = true,
                readerOpen = false,
                contentPhase = FolderContentPhase.None,
                folderCount = 0,
            ),
        )
        for (phase in listOf(
            FolderContentPhase.Loading,
            FolderContentPhase.AccessLost,
            FolderContentPhase.Empty,
            FolderContentPhase.Ready,
        )) {
            assertEquals(
                false,
                showsRootFolderList(
                    showSettings = false,
                    showLost = false,
                    loaded = true,
                    readerOpen = false,
                    contentPhase = phase,
                    folderCount = 2,
                ),
            )
        }
        assertEquals(
            false,
            showsRootFolderList(
                showSettings = true,
                showLost = false,
                loaded = true,
                readerOpen = false,
                contentPhase = FolderContentPhase.None,
                folderCount = 2,
            ),
        )
        assertEquals(
            false,
            showsRootFolderList(
                showSettings = false,
                showLost = true,
                loaded = true,
                readerOpen = false,
                contentPhase = FolderContentPhase.None,
                folderCount = 2,
            ),
        )
        assertEquals(
            false,
            showsRootFolderList(
                showSettings = false,
                showLost = false,
                loaded = false,
                readerOpen = false,
                contentPhase = FolderContentPhase.None,
                folderCount = 2,
            ),
        )
        assertEquals(
            false,
            showsRootFolderList(
                showSettings = false,
                showLost = false,
                loaded = true,
                readerOpen = true,
                contentPhase = FolderContentPhase.None,
                folderCount = 2,
            ),
        )
    }
}
