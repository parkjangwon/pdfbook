package org.parkjw.apps.pdfbook

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BookStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.dataStore() =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmp.root, "pdfbook.preferences_pb") },
        )

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
}
