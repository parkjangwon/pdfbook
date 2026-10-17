package org.parkjw.apps.pdfbook.folders

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FolderRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var permissions: FakeUriPermissions

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        permissions = FakeUriPermissions()
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun repository(fileName: String = "pdfbook.preferences_pb"): FolderRepository {
        val file = File(tmp.root, fileName)
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return FolderRepository(store, permissions)
    }

    @Test
    fun migrateFromLegacyFolderKeyIsIdempotentAndKeepsLegacy() = runBlocking {
        val file = File(tmp.root, "migrate.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        store.edit { it[FolderRepository.LegacyFolderKey] = "content://tree/legacy" }
        store.edit {
            it[stringPreferencesKey("book:content://tree/legacy|1")] = "3,10,RTL,100"
        }

        val repo = FolderRepository(store, permissions)
        repo.migrateFromLegacyIfNeeded { "Legacy Name" }
        repo.migrateFromLegacyIfNeeded { "Should Not Replace" }

        val folders = repo.folders.first()
        assertEquals(1, folders.size)
        assertEquals("content://tree/legacy", folders[0].uri)
        assertEquals("Legacy Name", folders[0].displayName)
        assertEquals(0, folders[0].order)

        val prefs = store.data.first()
        assertEquals("content://tree/legacy", prefs[FolderRepository.LegacyFolderKey])
        assertEquals(true, prefs[FolderRepository.MigratedKey])
        assertEquals("3,10,RTL,100", prefs[stringPreferencesKey("book:content://tree/legacy|1")])
    }

    @Test
    fun removeDoesNotClearBookReadingState() = runBlocking {
        val file = File(tmp.root, "books.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val bookKey = stringPreferencesKey("book:content://doc/1|9")
        store.edit { it[bookKey] = "2,5,LTR,42" }

        val repo = FolderRepository(store, permissions)
        assertEquals(AddKind.Created, repo.add("content://tree/a", "A"))
        repo.remove("content://tree/a")

        assertTrue(repo.folders.first().isEmpty())
        assertEquals("2,5,LTR,42", store.data.first()[bookKey])
        assertEquals(listOf("content://tree/a"), permissions.released)
    }

    @Test
    fun removeThenReaddRestoresAccessWithoutWipingBooks() = runBlocking {
        val file = File(tmp.root, "readd.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val bookKey = stringPreferencesKey("book:content://doc/x|1")
        store.edit { it[bookKey] = "1,2,RTL,7" }

        val repo = FolderRepository(store, permissions)
        repo.add("content://tree/a", "A")
        repo.rename("content://tree/a", "Custom", "A")
        repo.remove("content://tree/a")
        assertEquals(AddKind.Created, repo.add("content://tree/a", "A"))

        val folders = repo.folders.first()
        assertEquals("A", folders.single().displayName)
        assertEquals("1,2,RTL,7", store.data.first()[bookKey])
    }

    @Test
    fun lostFolderReselectKeepsNameAndOrder() = runBlocking {
        val repo = repository("lost.preferences_pb")
        repo.add("content://tree/a", "A")
        repo.add("content://tree/b", "B")
        repo.rename("content://tree/a", "Kept", "A")
        permissions.persisted = setOf("content://tree/b")
        repo.refreshAccess()

        val lost = repo.folders.first()
        assertEquals(FolderAccess.Lost, lost[0].access)
        assertEquals(FolderAccess.Ok, lost[1].access)

        assertEquals(AddKind.Restored, repo.add("content://tree/a", "Ignored"))
        val restored = repo.folders.first()
        assertEquals("Kept", restored[0].displayName)
        assertEquals(0, restored[0].order)
        assertEquals(FolderAccess.Ok, restored[0].access)
        assertEquals(1, restored[1].order)
        assertTrue(permissions.taken.contains("content://tree/a"))
    }

    @Test
    fun duplicateAddDoesNotTakeAgainOrGrowList() = runBlocking {
        val repo = repository("dup.preferences_pb")
        assertEquals(AddKind.Created, repo.add("content://tree/a", "A"))
        permissions.taken.clear()
        assertEquals(AddKind.Duplicate, repo.add("content://tree/a", "Other"))
        assertEquals(1, repo.folders.first().size)
        assertTrue(permissions.taken.isEmpty())
    }

    @Test
    fun refreshAccessMarksMissingPermissionsLost() = runBlocking {
        val repo = repository("refresh.preferences_pb")
        repo.add("content://tree/a", "A")
        repo.add("content://tree/b", "B")
        permissions.persisted = emptySet()
        repo.refreshAccess()
        assertTrue(repo.folders.first().all { it.access == FolderAccess.Lost })
        assertFalse(permissions.released.contains("content://tree/a"))
    }
}

private class FakeUriPermissions : UriPermissions {
    val taken = mutableListOf<String>()
    val released = mutableListOf<String>()
    var persisted: Set<String> = emptySet()

    override fun take(uri: String) {
        taken += uri
        persisted = persisted + uri
    }

    override fun release(uri: String) {
        released += uri
        persisted = persisted - uri
    }

    override fun persistedUris(): Set<String> = persisted
}
