package org.parkjw.apps.pdfbook.hidden

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.BookStore
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.settings.SettingsRepository
import java.io.File

class HiddenBookStateTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun h3_hideUnhideKeepsPageDirectionAndCoverAloneIsGlobal() = runTest {
        val books = BookStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "pdfbook.preferences_pb") })
        val hidden = HiddenRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "hidden.preferences_pb") })
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") })
        settings.update { it.copy(coverAlone = false) }
        val info = BookInfo("content://book", "Test Book 01.pdf", 20)
        val state = BookState(page = 4, pageCount = 20, direction = ReadingDirection.LTR, openedAt = 9)
        val key = bookKey(info.uri, info.size)
        books.save(key, state)
        val item = HiddenItem(HiddenKind.Book, info.uri, info.name, "content://folder")
        hidden.hide(item)
        assertEquals(state, books.state(key))
        hidden.unhide(HiddenKind.Book, info.uri)
        assertEquals(state, books.state(key))
        assertEquals(4, books.state(key)?.page)
        assertEquals(ReadingDirection.LTR, books.state(key)?.direction)
        assertFalse(settings.settings.first().coverAlone)
        assertTrue(hidden.items.first().isEmpty())
        val back = filterListing(Listing("Folder", listOf(info)), hiddenBookIds(hidden.items.first()), true)
        assertEquals(listOf(info), back.books)
    }

    @Test
    fun h7_hideWritesOnlyHiddenStoreAndKeepsBookIdentity() = runTest {
        val bookFile = File(tmp.root, "pdfbook.preferences_pb")
        val hiddenFile = File(tmp.root, "hidden.preferences_pb")
        val books = BookStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { bookFile })
        val hidden = HiddenRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { hiddenFile })
        val info = BookInfo("content://books/a.pdf", "Same Name.pdf", 42)
        val state = BookState(page = 3, pageCount = 9, direction = ReadingDirection.RTL, openedAt = 8)
        val key = bookKey(info.uri, info.size)
        books.save(key, state)
        val beforeBooks = bookFile.readBytes()
        val beforeHidden = if (hiddenFile.exists()) hiddenFile.readBytes() else ByteArray(0)
        hidden.hide(HiddenItem(HiddenKind.Book, info.uri, info.name, "content://folder"))
        assertTrue(bookFile.readBytes().contentEquals(beforeBooks))
        assertFalse(hiddenFile.readBytes().contentEquals(beforeHidden))
        val stored = hidden.items.first().single()
        assertEquals(info.uri, stored.id)
        assertEquals(info.name, stored.name)
        assertEquals(state, books.state(key))
    }
}
