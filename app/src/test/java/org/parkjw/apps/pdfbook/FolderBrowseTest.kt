package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderBrowseTest {
    @Test
    fun folderContentPhase_loadingUntilScannedEvenIfListingNull() {
        assertEquals(
            FolderContentPhase.Loading,
            folderContentPhase("uri", scanned = false, listing = null),
        )
        assertEquals(
            FolderContentPhase.AccessLost,
            folderContentPhase("uri", scanned = true, listing = null),
        )
    }

    @Test
    fun folderContentPhase_emptyAndReady() {
        assertEquals(
            FolderContentPhase.Empty,
            folderContentPhase("uri", scanned = true, Listing("A", emptyList())),
        )
        assertEquals(
            FolderContentPhase.Ready,
            folderContentPhase(
                "uri",
                scanned = true,
                Listing("A", listOf(BookInfo("b", "b.pdf", 1))),
            ),
        )
        assertEquals(
            FolderContentPhase.None,
            folderContentPhase(null, scanned = true, listing = null),
        )
    }

    @Test
    fun canApplyStartScreen_waitsForShelfListings() {
        assertFalse(
            canApplyStartScreen(
                startApplied = false,
                loaded = true,
                settingsReady = true,
                statesReady = true,
                folderCount = 2,
                shelfListingCount = 1,
            ),
        )
        assertTrue(
            canApplyStartScreen(
                startApplied = false,
                loaded = true,
                settingsReady = true,
                statesReady = true,
                folderCount = 2,
                shelfListingCount = 2,
            ),
        )
        assertFalse(
            canApplyStartScreen(
                startApplied = true,
                loaded = true,
                settingsReady = true,
                statesReady = true,
                folderCount = 0,
                shelfListingCount = 0,
            ),
        )
    }

    @Test
    fun shouldClearScrollTarget_onlyWhenFound() {
        assertFalse(shouldClearScrollTarget(-1))
        assertTrue(shouldClearScrollTarget(0))
    }
}
