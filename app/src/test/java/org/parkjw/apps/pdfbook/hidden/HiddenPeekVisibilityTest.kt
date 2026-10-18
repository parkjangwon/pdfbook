package org.parkjw.apps.pdfbook.hidden

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.findLastBookTarget
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.sortBooks
import java.io.File

class HiddenPeekVisibilityTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Before
    fun resetPeek() {
        HiddenPeek.reset()
    }

    @Test
    fun h8_peekShowsHiddenInPlaceButResumeStaysFiltered() {
        val folders = listOf(
            Folder("fa", "Alpha", 0, FolderAccess.Ok, lastOpenedAt = 2),
            Folder("fb", "Beta", 1, FolderAccess.Ok, lastOpenedAt = 1),
            Folder("fc", "Gamma", 2, FolderAccess.Ok, lastOpenedAt = 0),
        )
        val hidden = listOf(
            HiddenItem(HiddenKind.Folder, "fa", "Alpha", "fa"),
            HiddenItem(HiddenKind.Folder, "fc", "Gamma", "fc"),
        )
        val recent = BookInfo("content://new", "New.pdf", 1)
        val older = BookInfo("content://old", "Old.pdf", 2)
        val listings = mapOf(
            "fa" to Listing("Alpha", listOf(recent)),
            "fb" to Listing("Beta", listOf(older)),
            "fc" to Listing("Gamma", emptyList()),
        )
        val states = mapOf(
            bookKey(recent.uri, recent.size) to BookState(page = 2, pageCount = 10, openedAt = 500),
            bookKey(older.uri, older.size) to BookState(page = 1, pageCount = 4, openedAt = 100),
        )
        val resumeFolders = visibleFolders(folders, hidden)
        val resumeListings = listings.mapValues { (_, listing) ->
            filterListing(listing, hiddenBookIds(hidden), showDotFiles = true)
        }
        val displayFolders = shelfFolders(folders, hidden, peek = true).map { it.folder }
        val ui = buildPeekShelfUi(
            displayFolders,
            resumeFolders,
            hiddenFolderIds(hidden),
            listings,
            states,
        )
        assertEquals(listOf("fa", "fb", "fc"), ui.cards.map { it.uri })
        assertEquals(listOf(true, false, true), ui.cards.map { it.isHidden })
        assertEquals(older.uri, ui.globalResume?.book?.uri)
        assertNull(ui.cards.first { it.uri == "fa" }.resume)
        assertNull(ui.cards.first { it.uri == "fc" }.resume)
        assertEquals(older.uri, findLastBookTarget(resumeFolders, resumeListings, states)?.bookUri)
    }

    @Test
    fun h8_peekBooksKeepOrderAndResumeExcludesHidden() {
        val a = BookInfo("content://a", "A.pdf", 1)
        val b = BookInfo("content://b", "B.pdf", 2)
        val c = BookInfo("content://c", "C.pdf", 3)
        val hiddenIds = setOf(b.uri)
        assertEquals(listOf(a, c), libraryBooks(listOf(a, b, c), hiddenIds, peek = false, showDotFiles = true).map { it.book })
        val on = libraryBooks(listOf(a, b, c), hiddenIds, peek = true, showDotFiles = true)
        assertEquals(listOf(a, b, c), on.map { it.book })
        assertEquals(listOf(false, true, false), on.map { it.isHidden })
        val states = mapOf(
            bookKey(a.uri, a.size) to BookState(page = 1, pageCount = 2, openedAt = 10),
            bookKey(b.uri, b.size) to BookState(page = 1, pageCount = 2, openedAt = 100),
            bookKey(c.uri, c.size) to BookState(page = 1, pageCount = 2, openedAt = 50),
        )
        val sorted = sortBooks(on.map { it.book }, states, byName = false)
        assertEquals(c.uri, findLibraryResumeBook(sorted, states, hiddenIds)?.uri)
        val folders = listOf(Folder("f", "F", 0, FolderAccess.Ok))
        val resumeListings = mapOf(
            "f" to filterListing(Listing("F", listOf(a, b, c)), hiddenIds, showDotFiles = true),
        )
        assertEquals(c.uri, findLastBookTarget(folders, resumeListings, states)?.bookUri)
    }

    @Test
    fun h9_unhideThenUndoHideRoundTrip() = runBlocking {
        val repo = HiddenRepository(
            PreferenceDataStoreFactory.create { File(tmp.root, "hidden.preferences_pb") },
        )
        val item = HiddenItem(HiddenKind.Book, "content://b", "B", "f")
        repo.hide(item)
        assertEquals(listOf(item), repo.items.first())
        repo.unhide(HiddenKind.Book, item.id)
        assertTrue(repo.items.first().isEmpty())
        repo.hide(item)
        assertEquals(listOf(item), repo.items.first())
    }

    @Test
    fun h12_peekAndShowDotFilesIndependent() {
        val dot = BookInfo("content://dot", ".secret.pdf", 1)
        val plain = BookInfo("content://plain", "Visible.pdf", 2)
        val hiddenIds = setOf(plain.uri)
        val books = listOf(dot, plain)
        assertEquals(
            emptyList<String>(),
            libraryBooks(books, hiddenIds, peek = false, showDotFiles = false).map { it.book.uri },
        )
        assertEquals(
            listOf(dot.uri),
            libraryBooks(books, hiddenIds, peek = false, showDotFiles = true).map { it.book.uri },
        )
        assertEquals(
            listOf(plain.uri),
            libraryBooks(books, hiddenIds, peek = true, showDotFiles = false).map { it.book.uri },
        )
        assertEquals(
            listOf(dot.uri, plain.uri),
            libraryBooks(books, hiddenIds, peek = true, showDotFiles = true).map { it.book.uri },
        )
        assertEquals(
            listOf(true),
            libraryBooks(books, hiddenIds, peek = true, showDotFiles = false).map { it.isHidden },
        )
        assertEquals(
            listOf(false, true),
            libraryBooks(books, hiddenIds, peek = true, showDotFiles = true).map { it.isHidden },
        )
    }

    @Test
    fun h13_peekMenuDisabledWhenNoHidden() {
        assertFalse(peekMenuEnabled(0))
        assertTrue(peekMenuEnabled(1))
        assertTrue(shouldUnhideOnLongPress(true))
        assertFalse(shouldUnhideOnLongPress(false))
    }
}
