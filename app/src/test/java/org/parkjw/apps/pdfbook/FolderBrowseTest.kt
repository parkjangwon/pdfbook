package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess

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

    @Test
    fun listingWithFolderName_usesDisplayNameForLibraryTitle() {
        val listing = Listing("Scanned", listOf(BookInfo("b", "b.pdf", 1)))
        assertEquals("Renamed", listingWithFolderName(listing, "Renamed")?.folderName)
        assertEquals("Scanned", listingWithFolderName(listing, null)?.folderName)
        assertNull(listingWithFolderName(null, "Renamed"))
    }

    @Test
    fun findLastBookTarget_skipsLostAndDeletedFolders() {
        val ok = Folder("ok", "Ok", 0, FolderAccess.Ok)
        val lost = Folder("lost", "Lost", 1, FolderAccess.Lost)
        val bookOk = BookInfo("content://ok/a.pdf", "a.pdf", 1)
        val bookLost = BookInfo("content://lost/b.pdf", "b.pdf", 2)
        val listings = mapOf(
            "ok" to Listing("Ok", listOf(bookOk)),
            "lost" to null,
        )
        val states = mapOf(
            bookKey(bookOk.uri, bookOk.size) to BookState(page = 1, pageCount = 2, openedAt = 10L),
            bookKey(bookLost.uri, bookLost.size) to BookState(page = 1, pageCount = 2, openedAt = 99L),
        )
        assertEquals(
            LastBookTarget("ok", bookOk.uri),
            findLastBookTarget(listOf(ok, lost), listings, states),
        )
        assertNull(findLastBookTarget(listOf(lost), listings, states))
        assertNull(findLastBookTarget(emptyList(), emptyMap(), states))
    }

    @Test
    fun folderBackAction_readerThenLibraryThenShelfThenExit() {
        assertEquals(
            FolderBackAction.CloseReader,
            folderBackAction(
                showSettings = false,
                readerOpen = true,
                folderSelected = true,
                showLost = false,
            ),
        )
        assertEquals(
            FolderBackAction.CloseLibrary,
            folderBackAction(
                showSettings = false,
                readerOpen = false,
                folderSelected = true,
                showLost = false,
            ),
        )
        assertEquals(
            FolderBackAction.None,
            folderBackAction(
                showSettings = false,
                readerOpen = false,
                folderSelected = false,
                showLost = false,
            ),
        )
        assertEquals(
            FolderBackAction.CloseSettings,
            folderBackAction(
                showSettings = true,
                readerOpen = true,
                folderSelected = true,
                showLost = false,
            ),
        )
        assertEquals(
            FolderBackAction.CloseLost,
            folderBackAction(
                showSettings = false,
                readerOpen = false,
                folderSelected = false,
                showLost = true,
            ),
        )
    }
}
