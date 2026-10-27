package org.parkjw.apps.pdfbook

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BookStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.dataStore(fileName: String = "pdfbook.preferences_pb") =
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { File(tmp.root, fileName) })

    @Test
    fun emptyStoreHasNoStates() = runTest {
        val books = BookStore(dataStore())
        assertEquals(emptyMap<String, BookState>(), books.states.first())
        assertNull(books.state("u|1"))
    }

    @Test
    fun saveThenReadBackByKeyAndAcrossInstances() = runTest {
        val data = dataStore()
        val state = BookState(12, 300, ReadingDirection.LTR, 1_700_000_000_000L)
        BookStore(data).save(bookKey("content://doc/a", 10), state)

        val reopened = BookStore(data)
        assertEquals(state, reopened.state(bookKey("content://doc/a", 10)))
        assertEquals(mapOf("content://doc/a|10" to state), reopened.states.first())
    }

    @Test
    fun saveOverwritesPreviousState() = runTest {
        val books = BookStore(dataStore())
        books.save("k", BookState(1, 10, ReadingDirection.RTL, 1))
        books.save("k", BookState(9, 10, ReadingDirection.LTR, 2))
        assertEquals(BookState(9, 10, ReadingDirection.LTR, 2), books.state("k"))
        assertEquals(1, books.states.first().size)
    }

    @Test
    fun sameUriWithDifferentSizeIsADifferentBook() = runTest {
        val books = BookStore(dataStore())
        books.save(bookKey("u", 1), BookState(page = 3))
        books.save(bookKey("u", 2), BookState(page = 8))
        assertEquals(3, books.state(bookKey("u", 1))?.page)
        assertEquals(8, books.state(bookKey("u", 2))?.page)
    }

    @Test
    fun keysWithCommasPipesAndKoreanSurvive() = runTest {
        val books = BookStore(dataStore())
        val key = bookKey("content://tree/만화,1|2/원피스 01.pdf", 5)
        books.save(key, BookState(2, 4, ReadingDirection.RTL, 9))
        assertEquals(2, books.state(key)?.page)
        assertEquals(setOf(key), books.states.first().keys)
    }

    @Test
    fun keyThatItselfStartsWithPrefixKeepsFullNameOnReadBack() = runTest {
        val books = BookStore(dataStore())
        books.save("book:x", BookState(page = 4))
        assertEquals(setOf("book:x"), books.states.first().keys)
        assertEquals(4, books.state("book:x")?.page)
        assertNull(books.state("x"))
    }

    @Test
    fun emptyKeyIsStoredAndRead() = runTest {
        val books = BookStore(dataStore())
        books.save("", BookState(page = 2))
        assertEquals(2, books.state("")?.page)
    }

    @Test
    fun unrelatedMalformedAndWrongTypeEntriesAreSkipped() = runTest {
        val data = dataStore()
        data.edit {
            it[stringPreferencesKey("book:good|1")] = "5,50,LTR,7"
            it[stringPreferencesKey("book:bad|1")] = "five,50,LTR,7"
            it[stringPreferencesKey("book:short|1")] = "5,50"
            it[stringPreferencesKey("book:long|1")] = "5,50,LTR,7,8"
            it[stringPreferencesKey("book:dir|1")] = "5,50,ltr,7"
            it[stringPreferencesKey("book:empty|1")] = ""
            it[booleanPreferencesKey("book:flag|1")] = true
            it[stringPreferencesKey("folders")] = "1,2,RTL,3"
            it[stringPreferencesKey("sort:folder")] = "5,50,LTR,7"
        }
        val books = BookStore(data)
        assertEquals(mapOf("good|1" to BookState(5, 50, ReadingDirection.LTR, 7)), books.states.first())
        assertNull(books.state("bad|1"))
        assertNull(books.state("flag|1"))
        assertNull(books.state("folders"))
    }

    @Test
    fun saveKeepsMalformedNeighboursUntouched() = runTest {
        val data = dataStore()
        val bad = stringPreferencesKey("book:bad|1")
        data.edit { it[bad] = "garbage" }
        BookStore(data).save("good|1", BookState(page = 2))
        assertEquals("garbage", data.data.first()[bad])
    }

    @Test
    fun decodeRejectsWhitespaceOverflowAndNonNumericParts() {
        assertNull(decodeState(" 1,2,RTL,0"))
        assertNull(decodeState("1,2,RTL, 0"))
        assertNull(decodeState("1,2,RTL,"))
        assertNull(decodeState(",,,"))
        assertNull(decodeState("1,2,RTL,0,"))
        assertNull(decodeState("1,2,RTL,0.5"))
        assertNull(decodeState("2147483648,2,RTL,0"))
        assertNull(decodeState("1,2,RTL,9223372036854775808"))
        assertNull(decodeState("1;2;RTL;0"))
        assertNull(decodeState("1,2,rtl,0"))
    }

    @Test
    fun extremeButValidValuesRoundTrip() {
        val max = BookState(Int.MAX_VALUE, Int.MAX_VALUE, ReadingDirection.LTR, Long.MAX_VALUE)
        assertEquals(max, decodeState(encodeState(max)))
        val min = BookState(Int.MIN_VALUE, 0, ReadingDirection.RTL, Long.MIN_VALUE)
        assertEquals(min, decodeState(encodeState(min)))
    }

    @Test
    fun everyDirectionRoundTrips() {
        for (direction in ReadingDirection.entries) {
            val s = BookState(3, 9, direction, 1)
            assertEquals(s, decodeState(encodeState(s)))
        }
    }

    @Test
    fun autoCropDefaultsToOff() = runTest {
        val store = BookStore(dataStore())
        assertNull(store.state("content://a|1"))
        assertFalse(BookState().autoCrop)
    }

    @Test
    fun autoCropIsRememberedPerBook() = runTest {
        val store = BookStore(dataStore())
        store.save("content://a|1", BookState(page = 4, pageCount = 20, autoCrop = true))
        store.save("content://b|1", BookState(page = 1, pageCount = 8, autoCrop = false))

        assertTrue(store.state("content://a|1")?.autoCrop == true)
        assertFalse(store.state("content://b|1")?.autoCrop ?: true)
    }

    @Test
    fun togglingOffIsPersisted() = runTest {
        val store = BookStore(dataStore())
        store.save("content://a|1", BookState(autoCrop = true))
        store.save("content://a|1", store.state("content://a|1")!!.copy(autoCrop = false))
        assertEquals(BookState(autoCrop = false), store.state("content://a|1"))
    }

    @Test
    fun readingProgressKeepsAutoCrop() = runTest {
        val store = BookStore(dataStore())
        store.save("content://a|1", BookState(autoCrop = true))
        val saved = store.state("content://a|1")!!
        store.save("content://a|1", saved.copy(page = 9, openedAt = 99L))
        assertEquals(BookState(page = 9, openedAt = 99L, autoCrop = true), store.state("content://a|1"))
    }

    @Test
    fun pageFitDefaultsToScreenAndIsRememberedPerBook() = runTest {
        val store = BookStore(dataStore())
        assertEquals(PageFit.Screen, store.fit("content://a|1").first())

        store.saveFit("content://a|1", PageFit.Width)
        store.saveFit("content://b|1", PageFit.Height)

        assertEquals(PageFit.Width, store.fit("content://a|1").first())
        assertEquals(PageFit.Height, store.fit("content://b|1").first())
    }

    @Test
    fun pageFitBackToScreenDropsTheEntry() = runTest {
        val data = dataStore()
        val store = BookStore(data)
        store.saveFit("content://a|1", PageFit.Height)
        store.saveFit("content://a|1", PageFit.Screen)

        assertEquals(PageFit.Screen, store.fit("content://a|1").first())
        assertNull(data.data.first()[stringPreferencesKey("fit:content://a|1")])
    }

    @Test
    fun pageFitDoesNotLeakIntoBookStatesOrBookmarks() = runTest {
        val store = BookStore(dataStore())
        store.saveFit("content://a|1", PageFit.Width)
        store.save("content://a|1", BookState(page = 3, pageCount = 9))
        assertEquals(setOf("content://a|1"), store.states.first().keys)
        assertEquals(emptyList<Int>(), store.bookmarks("content://a|1").first())
    }
}
