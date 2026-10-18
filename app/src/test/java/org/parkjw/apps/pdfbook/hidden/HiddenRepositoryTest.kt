package org.parkjw.apps.pdfbook.hidden

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HiddenRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun repository(): HiddenRepository {
        val file = File(tmp.root, "hidden.preferences_pb")
        return HiddenRepository(PreferenceDataStoreFactory.create(produceFile = { file }))
    }

    @Test
    fun hideUnhideKeepsNewestFirstAndDropsBrokenRows() = runBlocking {
        val file = File(tmp.root, "broken.preferences_pb")
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        val repo = HiddenRepository(store)
        val first = HiddenItem(HiddenKind.Folder, "content://f", "Folder", "content://f")
        val second = HiddenItem(HiddenKind.Book, "content://b", "Book", "content://f")
        repo.hide(first)
        repo.hide(second)
        assertEquals(listOf(second, first), repo.items.first())
        repo.unhide(HiddenKind.Book, second.id)
        assertEquals(listOf(first), repo.items.first())
        val kept = HiddenItem(HiddenKind.Book, "content://ok", "Ok", null)
        store.edit { prefs ->
            prefs[HiddenRepository.ItemsKey] = "not-a-row\n" + encodeHiddenItems(listOf(kept))
        }
        assertEquals(listOf(kept), repo.items.first())
    }

    @Test
    fun namesWithTabNewlineAndPercentRoundTrip() = runBlocking {
        val repo = repository()
        val item = HiddenItem(
            kind = HiddenKind.Book,
            id = "content://id/%0A",
            name = "a\tb\n%c",
            folderUri = "folder\nuri",
        )
        repo.hide(item)
        assertEquals(listOf(item), repo.items.first())
    }
}
