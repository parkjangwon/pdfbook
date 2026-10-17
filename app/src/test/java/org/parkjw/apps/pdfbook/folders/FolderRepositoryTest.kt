package org.parkjw.apps.pdfbook.folders

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    private lateinit var job: Job
    private lateinit var scope: CoroutineScope
    private lateinit var permissions: FakeUriPermissions

    @Before
    fun setUp() {
        job = Job()
        scope = CoroutineScope(job + Dispatchers.Unconfined)
        permissions = FakeUriPermissions()
    }

    @After
    fun tearDown() = runBlocking {
        job.cancelAndJoin()
    }

    private fun repository(
        fileName: String = "pdfbook.preferences_pb",
        displayNameFor: (String) -> String = ::displayNameForUri,
    ): FolderRepository {
        val file = File(tmp.root, fileName)
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return FolderRepository(store, permissions, displayNameFor)
    }

    private fun store(fileName: String) =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, fileName) },
        )

    @Test
    fun addDoesNotUseNewNameForLegacyMigration() = runBlocking {
        val data = store("legacy-name.preferences_pb")
        data.edit {
            it[FolderRepository.LegacyFolderKey] =
                "content://com.android.externalstorage.documents/tree/primary%3AOldBooks"
        }
        val repo = FolderRepository(data, permissions)
        assertEquals(AddKind.Created, repo.add("content://tree/new", "NewName"))

        val folders = repo.folders.first()
        assertEquals(2, folders.size)
        assertEquals("OldBooks", folders[0].displayName)
        assertEquals("NewName", folders[1].displayName)
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3AOldBooks",
            folders[0].uri,
        )
    }

    @Test
    fun concurrentAddsBothPersist() = runBlocking {
        repeat(20) { attempt ->
            permissions = FakeUriPermissions()
            val repo = repository("concurrent-$attempt.preferences_pb")
            withContext(Dispatchers.Default) {
                coroutineScope {
                    val a = async { repo.add("content://tree/a", "A") }
                    val b = async { repo.add("content://tree/b", "B") }
                    assertEquals(AddKind.Created, a.await())
                    assertEquals(AddKind.Created, b.await())
                }
            }
            assertEquals(
                setOf("content://tree/a", "content://tree/b"),
                repo.folders.first().map { it.uri }.toSet(),
            )
        }
    }

    @Test
    fun migrateFromLegacyFolderKeyIsIdempotentAndKeepsLegacy() = runBlocking {
        val data = store("migrate.preferences_pb")
        data.edit { it[FolderRepository.LegacyFolderKey] = "content://tree/legacy" }
        data.edit {
            it[stringPreferencesKey("book:content://tree/legacy|1")] = "3,10,RTL,100"
        }

        val repo = FolderRepository(data, permissions) { "Legacy Name" }
        repo.migrateFromLegacyIfNeeded()
        repo.migrateFromLegacyIfNeeded()

        val folders = repo.folders.first()
        assertEquals(1, folders.size)
        assertEquals("content://tree/legacy", folders[0].uri)
        assertEquals("Legacy Name", folders[0].displayName)
        assertEquals(0, folders[0].order)

        val prefs = data.data.first()
        assertEquals("content://tree/legacy", prefs[FolderRepository.LegacyFolderKey])
        assertEquals(true, prefs[FolderRepository.MigratedKey])
        assertEquals("3,10,RTL,100", prefs[stringPreferencesKey("book:content://tree/legacy|1")])
    }

    @Test
    fun migrateWithoutLegacyYieldsEmptyList() = runBlocking {
        val data = store("no-legacy.preferences_pb")
        val repo = FolderRepository(data, permissions)
        repo.migrateFromLegacyIfNeeded()
        assertTrue(repo.folders.first().isEmpty())
        assertEquals(true, data.data.first()[FolderRepository.MigratedKey])
    }

    @Test
    fun migrateThenRefreshMarksMissingLegacyPermissionLost() = runBlocking {
        val data = store("lost-legacy.preferences_pb")
        data.edit { it[FolderRepository.LegacyFolderKey] = "content://tree/legacy" }
        permissions.persisted = emptySet()

        val repo = FolderRepository(data, permissions) { "Legacy" }
        repo.migrateFromLegacyIfNeeded()
        repo.refreshAccess()

        val folders = repo.folders.first()
        assertEquals(1, folders.size)
        assertEquals("content://tree/legacy", folders[0].uri)
        assertEquals(FolderAccess.Lost, folders[0].access)
    }

    @Test
    fun afterMigrationPrimaryUriMatchesLegacy() = runBlocking {
        val legacy = "content://tree/legacy-primary"
        val data = store("primary.preferences_pb")
        data.edit { it[FolderRepository.LegacyFolderKey] = legacy }
        permissions.persisted = setOf(legacy)

        val repo = FolderRepository(data, permissions) { "Name" }
        repo.migrateFromLegacyIfNeeded()
        repo.refreshAccess()

        assertEquals(legacy, repo.folders.first().firstOrNull()?.uri)
        assertEquals(FolderAccess.Ok, repo.folders.first().first().access)
    }

    @Test
    fun removeDoesNotClearBookReadingState() = runBlocking {
        val data = store("books.preferences_pb")
        val bookKey = stringPreferencesKey("book:content://doc/1|9")
        data.edit { it[bookKey] = "2,5,LTR,42" }

        val repo = FolderRepository(data, permissions)
        assertEquals(AddKind.Created, repo.add("content://tree/a", "A"))
        repo.remove("content://tree/a")

        assertTrue(repo.folders.first().isEmpty())
        assertEquals("2,5,LTR,42", data.data.first()[bookKey])
        assertEquals(listOf("content://tree/a"), permissions.released)
    }

    @Test
    fun replacePrimaryKeepsBookKeys() = runBlocking {
        val data = store("replace-books.preferences_pb")
        val bookKey = stringPreferencesKey("book:content://doc/old|1")
        data.edit { it[bookKey] = "1,2,RTL,7" }

        val repo = FolderRepository(data, permissions)
        repo.replacePrimary("content://tree/old", "Old")
        repo.replacePrimary("content://tree/new", "New")

        assertEquals("content://tree/new", repo.folders.first().single().uri)
        assertEquals("1,2,RTL,7", data.data.first()[bookKey])
        assertEquals(listOf("content://tree/old"), permissions.released)
    }

    @Test
    fun removeThenReaddRestoresAccessWithoutWipingBooks() = runBlocking {
        val data = store("readd.preferences_pb")
        val bookKey = stringPreferencesKey("book:content://doc/x|1")
        data.edit { it[bookKey] = "1,2,RTL,7" }

        val repo = FolderRepository(data, permissions)
        repo.add("content://tree/a", "A")
        repo.rename("content://tree/a", "Custom", "A")
        repo.remove("content://tree/a")
        assertEquals(AddKind.Created, repo.add("content://tree/a", "A"))

        val folders = repo.folders.first()
        assertEquals("A", folders.single().displayName)
        assertEquals("1,2,RTL,7", data.data.first()[bookKey])
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

    @Test
    fun takeSecurityExceptionDoesNotPersistAdd() = runBlocking {
        val repo = repository("sec.preferences_pb")
        permissions.throwOnTake = setOf("content://tree/bad")
        try {
            repo.add("content://tree/bad", "Bad")
            assertTrue("expected SecurityException", false)
        } catch (_: SecurityException) {
        }
        assertTrue(repo.folders.first().isEmpty())
    }

    @Test
    fun removeLostFolderDoesNotThrowWhenPermissionAlreadyGone() = runBlocking {
        val repo = repository("lost-release.preferences_pb")
        repo.add("content://tree/a", "A")
        permissions.persisted = emptySet()
        permissions.throwOnReleaseMissing = true
        repo.refreshAccess()
        repo.remove("content://tree/a")
        assertTrue(repo.folders.first().isEmpty())
        assertTrue(permissions.released.isEmpty())
    }

    @Test
    fun replacePrimaryLostOldDoesNotThrowOnRelease() = runBlocking {
        val repo = repository("replace-lost-release.preferences_pb")
        repo.replacePrimary("content://tree/old", "Old")
        permissions.persisted = setOf("content://tree/new")
        permissions.throwOnReleaseMissing = true
        repo.replacePrimary("content://tree/new", "New")
        assertEquals("content://tree/new", repo.folders.first().single().uri)
        assertTrue(permissions.released.isEmpty())
    }
}

private class FakeUriPermissions : UriPermissions {
    val taken = mutableListOf<String>()
    val released = mutableListOf<String>()
    var persisted: Set<String> = emptySet()
    var throwOnTake: Set<String> = emptySet()
    var throwOnReleaseMissing: Boolean = false

    override fun take(uri: String) {
        if (uri in throwOnTake) throw SecurityException("denied")
        taken += uri
        persisted = persisted + uri
    }

    override fun release(uri: String) {
        releasePersistableSafely(uri, persisted) {
            if (throwOnReleaseMissing) throw SecurityException("not held")
            released += it
            persisted = persisted - it
        }
    }

    override fun persistedUris(): Set<String> = persisted
}
