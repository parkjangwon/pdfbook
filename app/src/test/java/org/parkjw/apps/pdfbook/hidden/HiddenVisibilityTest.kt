package org.parkjw.apps.pdfbook.hidden

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.buildFolderShelfUi
import org.parkjw.apps.pdfbook.findLastBookTarget
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.SettingsRepository
import java.io.File

class HiddenVisibilityTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun h1_hiddenFolderLeavesShelfAndResume() {
        val recent = BookInfo("content://new", "New.pdf", 1)
        val older = BookInfo("content://old", "Old.pdf", 2)
        val folders = listOf(
            Folder("fa", "Alpha", 0, FolderAccess.Ok, lastOpenedAt = 2),
            Folder("fb", "Beta", 1, FolderAccess.Ok, lastOpenedAt = 1),
        )
        val listings = mapOf(
            "fa" to Listing("Alpha", listOf(recent)),
            "fb" to Listing("Beta", listOf(older)),
        )
        val states = mapOf(
            bookKey(recent.uri, recent.size) to BookState(page = 2, pageCount = 10, openedAt = 500),
            bookKey(older.uri, older.size) to BookState(page = 1, pageCount = 4, openedAt = 100),
        )
        val visible = visibleFolders(
            folders,
            listOf(HiddenItem(HiddenKind.Folder, "fa", "Alpha", "fa")),
        )
        val ui = buildFolderShelfUi(visible, listings, states)
        assertEquals(listOf("fb"), ui.cards.map { it.uri })
        assertEquals(older.uri, ui.globalResume?.book?.uri)
        assertEquals("fb", ui.globalResume?.folderUri)
        assertEquals("fb", findLastBookTarget(visible, listings, states)?.folderUri)
        assertEquals(older.uri, findLastBookTarget(visible, listings, states)?.bookUri)
    }

    @Test
    fun h2_hiddenBookLeavesLibraryResumeAndLastTarget() {
        val hiddenBook = BookInfo("content://hide", "Hide.pdf", 3)
        val kept = BookInfo("content://keep", "Keep.pdf", 4)
        val filtered = filterListing(
            Listing("Shelf", listOf(hiddenBook, kept)),
            hiddenBookIds(listOf(HiddenItem(HiddenKind.Book, hiddenBook.uri, "Hide", "f"))),
            showDotFiles = true,
        )
        assertEquals(listOf(kept), filtered.books)
        val folders = listOf(Folder("f", "Shelf", 0))
        val listings = mapOf("f" to filtered)
        val states = mapOf(
            bookKey(hiddenBook.uri, hiddenBook.size) to BookState(page = 8, pageCount = 10, openedAt = 900),
            bookKey(kept.uri, kept.size) to BookState(page = 2, pageCount = 10, openedAt = 100),
        )
        val ui = buildFolderShelfUi(folders, listings, states)
        assertEquals(listOf(kept.uri), ui.cards.single().coverKeys.map { it.substringBefore('|') })
        assertEquals("Keep", ui.cards.single().resume?.title)
        assertEquals(kept.uri, ui.globalResume?.book?.uri)
        assertEquals(kept.uri, findLastBookTarget(folders, listings, states)?.bookUri)
    }

    @Test
    fun h6_showDotFilesFalseDropsDotPdfAndDefaultsOff() = runTest {
        val dot = BookInfo("content://dot", ".secret.pdf", 1)
        val plain = BookInfo("content://plain", "Visible.pdf", 2)
        val listing = Listing("Shelf", listOf(dot, plain))
        assertEquals(listOf(plain.uri), filterListing(listing, emptySet(), showDotFiles = false).books.map { it.uri })
        assertEquals(listOf(dot.uri, plain.uri), filterListing(listing, emptySet(), showDotFiles = true).books.map { it.uri })
        assertTrue(isDotName(".secret.pdf"))
        assertFalse(isDotName("Visible.pdf"))
        val registered = listOf(Folder("content://tree/.secret", ".secret", 0))
        assertEquals(registered, visibleFolders(registered, emptyList()))
        assertFalse(AppSettings().showDotFiles)
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { File(tmp.root, "settings.preferences_pb") },
            ),
        )
        assertFalse(settings.settings.first().showDotFiles)
    }
}
