package org.parkjw.apps.pdfbook

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SortStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.dataStore() =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmp.root, "pdfbook.preferences_pb") },
        )

    @Test
    fun defaultIsRecentWhenNothingSaved() = runTest {
        val store = SortStore(dataStore())
        val sorts = store.sorts.first()
        assertEquals(BookSort.Recent, sortFor(sorts, "content://a"))
        assertEquals(BookSort.Recent, sortFor(sorts, null))
    }

    @Test
    fun savedSortIsKeptPerFolder() = runTest {
        val store = SortStore(dataStore())
        store.save(sortKey("content://a"), BookSort.Name)
        val sorts = store.sorts.first()
        assertEquals(BookSort.Name, sortFor(sorts, "content://a"))
        assertEquals(BookSort.Recent, sortFor(sorts, "content://b"))
    }

    @Test
    fun choiceStaysUntilChangedAgain() = runTest {
        val store = SortStore(dataStore())
        store.save(sortKey("content://a"), BookSort.Name)
        store.save(sortKey("content://b"), BookSort.Name)
        assertEquals(BookSort.Name, sortFor(store.sorts.first(), "content://a"))
        store.save(sortKey("content://a"), BookSort.Recent)
        val sorts = store.sorts.first()
        assertEquals(BookSort.Recent, sortFor(sorts, "content://a"))
        assertEquals(BookSort.Name, sortFor(sorts, "content://b"))
    }

    @Test
    fun ignoresOtherKeysAndBrokenValues() = runTest {
        val prefs = dataStore()
        val store = SortStore(prefs)
        val book = BookStore(prefs)
        book.save("content://a|1", BookState(page = 3, pageCount = 9))
        prefs.edit { it[stringPreferencesKey("sort:content://x")] = "Sideways" }
        store.save(sortKey("content://a"), BookSort.Name)
        assertEquals(mapOf("content://a" to BookSort.Name), store.sorts.first())
        assertEquals(BookState(page = 3, pageCount = 9), book.state("content://a|1"))
    }

    @Test
    fun decodeSortRejectsUnknown() {
        assertEquals(BookSort.Name, decodeSort("Name"))
        assertNull(decodeSort("name"))
        assertNull(decodeSort(null))
    }

    @Test
    fun unsetSubfolderFollowsNearestParent() = runTest {
        val store = SortStore(dataStore())
        store.save(sortKey("content://a"), BookSort.Name)
        store.save(sortKey("content://a", listOf("s1", "s2")), BookSort.Recent)
        val sorts = store.sorts.first()
        assertEquals(BookSort.Name, sortFor(sorts, "content://a", listOf("s1")))
        assertEquals(BookSort.Recent, sortFor(sorts, "content://a", listOf("s1", "s2")))
        assertEquals(BookSort.Recent, sortFor(sorts, "content://a", listOf("s1", "s2", "s3")))
        assertEquals(BookSort.Name, sortFor(sorts, "content://a", listOf("x", "s2")))
    }

    @Test
    fun removingRootClearsItsSubfoldersOnly() = runTest {
        val store = SortStore(dataStore())
        store.save(sortKey("content://a"), BookSort.Name)
        store.save(sortKey("content://a", listOf("s1")), BookSort.Name)
        store.save(sortKey("content://ab"), BookSort.Name)
        store.save(sortKey("content://b"), BookSort.Name)
        store.removeRoot("content://a")
        assertEquals(
            setOf(sortKey("content://ab"), sortKey("content://b")),
            store.sorts.first().keys,
        )
        assertEquals(BookSort.Recent, sortFor(store.sorts.first(), "content://a", listOf("s1")))
    }

    @Test
    fun keyChainGoesFromDeepestToRoot() {
        assertEquals(
            listOf(sortKey("r", listOf("a", "b")), sortKey("r", listOf("a")), "r"),
            sortKeyChain("r", listOf("a", "b")),
        )
        assertEquals(listOf("r"), sortKeyChain("r"))
    }

    @Test
    fun repickMovesSortsToNewFolderUri() = runTest {
        val store = SortStore(dataStore())
        store.save(sortKey("content://old"), BookSort.Name)
        store.save(sortKey("content://old", listOf("s1")), BookSort.Recent)
        store.save(sortKey("content://other"), BookSort.Name)
        store.moveRoot("content://old", "content://new")
        val sorts = store.sorts.first()
        assertEquals(BookSort.Name, sortFor(sorts, "content://new"))
        assertEquals(BookSort.Recent, sortFor(sorts, "content://new", listOf("s1")))
        assertEquals(BookSort.Recent, sortFor(sorts, "content://old"))
        assertEquals(BookSort.Name, sortFor(sorts, "content://other"))
    }

    @Test
    fun repickOntoFolderWithOwnSortKeepsThatSort() = runTest {
        val store = SortStore(dataStore())
        store.save(sortKey("content://old"), BookSort.Name)
        store.save(sortKey("content://new"), BookSort.Recent)
        store.moveRoot("content://old", "content://new")
        val sorts = store.sorts.first()
        assertEquals(mapOf(sortKey("content://new") to BookSort.Recent), sorts)
    }
}
