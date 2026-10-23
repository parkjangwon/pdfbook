package org.parkjw.apps.pdfbook

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.hidden.filterListing
import org.parkjw.apps.pdfbook.hidden.findLibraryResumeBook
import java.io.File

class MarkFinishedTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val book = BookInfo("content://books/a.pdf", "A.pdf", 42)
    private val key = bookKey(book.uri, book.size)

    @Test
    fun mf1_markFinishedStoresLastPageAndKeepsDirection() = runTest {
        val store = BookStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                File(tmp.root, "books.preferences_pb")
            },
        )
        store.save(key, BookState(page = 3, pageCount = 10, direction = ReadingDirection.LTR, openedAt = 7))
        store.save(key, markFinished(store.state(key)!!))
        val after = store.state(key)!!
        assertEquals(10, after.page)
        assertEquals(ReadingDirection.LTR, after.direction)
        assertEquals(7L, after.openedAt)
        assertEquals(1f, progressOf(after.page, after.pageCount), 0f)
    }

    @Test
    fun mf10_undoMarkFinishedRestoresSavedPageAndSession() = runTest {
        val store = BookStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                File(tmp.root, "books.preferences_pb")
            },
        )
        val before = BookState(page = 3, pageCount = 10, direction = ReadingDirection.LTR, openedAt = 7)
        store.save(key, before)
        val undo = store.applyProgress(key, ::markFinished)!!
        assertEquals(10, store.state(key)!!.page)
        store.undoProgress(undo)
        assertEquals(before, store.state(key))
    }

    @Test
    fun mf11_undoStartOverRestoresSavedPageAndSession() = runTest {
        val store = BookStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                File(tmp.root, "books.preferences_pb")
            },
        )
        val before = BookState(page = 6, pageCount = 10, direction = ReadingDirection.LTR, openedAt = 7)
        store.save(key, before)
        val undo = store.applyProgress(key, ::startOver)!!
        assertEquals(1, store.state(key)!!.page)
        assertEquals(0L, store.state(key)!!.openedAt)
        store.undoProgress(undo)
        assertEquals(before, store.state(key))
    }

    @Test
    fun mf2_startOverStoresFirstPageAndClearsOpenedAt() = runTest {
        val store = BookStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                File(tmp.root, "books.preferences_pb")
            },
        )
        store.save(key, BookState(page = 6, pageCount = 10, openedAt = 7))
        store.save(key, startOver(store.state(key)!!))
        val after = store.state(key)!!
        assertEquals(1, after.page)
        assertEquals(10, after.pageCount)
        assertEquals(0L, after.openedAt)
    }

    @Test
    fun mf3_menuItemsFollowProgress() {
        assertTrue(canMarkFinished(BookState(page = 1, pageCount = 10, openedAt = 1)))
        assertTrue(canMarkFinished(BookState(page = 9, pageCount = 10, openedAt = 0)))
        assertFalse(canMarkFinished(BookState(page = 10, pageCount = 10, openedAt = 1)))
        assertFalse(canMarkFinished(BookState(page = 12, pageCount = 10, openedAt = 1)))
        assertFalse(canMarkFinished(BookState(page = 1, pageCount = 0, openedAt = 1)))
        assertFalse(canMarkFinished(null))

        assertTrue(canStartOver(BookState(page = 1, pageCount = 10, openedAt = 1)))
        assertTrue(canStartOver(BookState(page = 10, pageCount = 10, openedAt = 1)))
        assertFalse(canStartOver(BookState(page = 1, pageCount = 10)))
        assertFalse(canStartOver(null))
    }

    @Test
    fun mf4_startOverLeavesContinueReadingOnShelfAndLibrary() {
        val folder = Folder("f", "Shelf", 0, FolderAccess.Ok)
        val listing = Listing("Shelf", listOf(book))
        val states = mapOf(key to startOver(BookState(page = 6, pageCount = 10, openedAt = 500)))

        assertNull(findLibraryResumeBook(listOf(book), states, emptySet()))
        val shelf = buildFolderShelfUi(listOf(folder), mapOf("f" to listing), states)
        assertNull(shelf.globalResume)
        assertNull(shelf.cards.single().resume)
        assertNull(findLastBookTarget(listOf(folder), mapOf("f" to listing), states))
        assertEquals(listOf(book), listing.books)
    }

    @Test
    fun mf5_markedFinishedBookReadsAsHundredPercentInContinueReading() {
        val folder = Folder("f", "Shelf", 0, FolderAccess.Ok)
        val listing = Listing("Shelf", listOf(book))
        val states = mapOf(key to markFinished(BookState(page = 2, pageCount = 10, openedAt = 500)))

        assertEquals(book, findLibraryResumeBook(listOf(book), states, emptySet()))
        val shelf = buildFolderShelfUi(listOf(folder), mapOf("f" to listing), states)
        val resume = checkNotNull(shelf.globalResume)
        assertEquals(1f, resume.progress, 0f)
        assertEquals(100, resume.percent)
    }

    @Test
    fun mf6_startOverLeavesHiddenBookHidden() {
        val states = mapOf(key to startOver(BookState(page = 4, pageCount = 10, openedAt = 1)))
        val hiddenIds = setOf(book.uri)
        val listing = Listing("Shelf", listOf(book))
        assertTrue(filterListing(listing, hiddenIds, showDotFiles = true).books.isEmpty())
        assertNull(findLibraryResumeBook(listing.books, states, hiddenIds))
    }
}
