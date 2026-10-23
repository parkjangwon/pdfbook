package org.parkjw.apps.pdfbook.folders

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FolderEdgeCasesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var job: Job
    private lateinit var scope: CoroutineScope
    private val permissions = StubPermissions()

    @Before
    fun setUp() {
        job = Job()
        scope = CoroutineScope(job + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = runBlocking { job.cancelAndJoin() }

    private fun store(fileName: String) =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(tmp.root, fileName) })

    private fun repository(fileName: String) = FolderRepository(store(fileName), permissions)

    private fun folder(uri: String, order: Int, access: FolderAccess = FolderAccess.Ok, opened: Long = 0L) =
        Folder(uri, uri.substringAfterLast('/'), order, access, opened)

    // --- codec ---

    @Test
    fun decodeSkipsBrokenRecordsButKeepsGoodOnes() {
        val good = Folder("content://a", "A", 0, FolderAccess.Ok, 5L)
        val raw = listOf(
            encodeFolders(listOf(good)),
            "content://b\u0001B\u0001x\u0001Ok",
            "content://c\u0001C\u00011\u0001Gone",
            "content://d\u0001D\u00012\u0001Ok\u0001nope",
            "content://e\u0001E\u00013\u0001Ok\u00010\u0001extra",
            "only-one-field",
            "",
        ).joinToString("\u0002")
        assertEquals(listOf(good), decodeFolders(raw))
    }

    @Test
    fun decodeHandlesNullEmptyAndDegenerateInput() {
        assertEquals(emptyList<Folder>(), decodeFolders(null))
        assertEquals(emptyList<Folder>(), decodeFolders(""))
        assertEquals(emptyList<Folder>(), decodeFolders("\u0002"))
        assertEquals(emptyList<Folder>(), decodeFolders("\u0001\u0001\u0001"))
    }

    @Test
    fun decodeKeepsEmptyUriAndNameWhenOtherFieldsAreValid() {
        assertEquals(
            listOf(Folder("", "", 0, FolderAccess.Lost, 0L)),
            decodeFolders("\u0001\u00010\u0001Lost"),
        )
    }

    @Test
    fun decodeAcceptsNegativeOrderAndOpenedAt() {
        assertEquals(
            listOf(Folder("u", "n", -2, FolderAccess.Ok, -1L)),
            decodeFolders("u\u0001n\u0001-2\u0001Ok\u0001-1"),
        )
    }

    @Test
    fun encodeOfEmptyListIsEmptyAndDecodesBackToEmpty() {
        assertEquals("", encodeFolders(emptyList()))
        assertEquals(emptyList<Folder>(), decodeFolders(encodeFolders(emptyList())))
    }

    // --- duplicate URIs ---

    @Test
    fun addTreatsLostDuplicateAsRestoredEvenWithTwoStoredRecords() {
        val folders = listOf(folder("u", 0, FolderAccess.Lost), folder("u", 1, FolderAccess.Lost))
        val result = add(folders, "u", "X")
        assertEquals(AddKind.Restored, result.kind)
        assertEquals(listOf(FolderAccess.Ok, FolderAccess.Lost), result.folders.map { it.access })
        assertEquals(2, result.folders.size)
    }

    @Test
    fun removeDropsEveryRecordWithThatUriAndReleasesOnce() {
        val folders = listOf(folder("u", 0), folder("x", 1), folder("u", 2))
        val result = remove(folders, "u")
        assertEquals(listOf("x"), result.folders.map { it.uri })
        assertEquals(listOf(0), result.folders.map { it.order })
        assertEquals(listOf("u"), result.releaseUris)
    }

    @Test
    fun markOpenedTouchesEveryRecordWithThatUri() {
        val folders = listOf(folder("u", 0), folder("u", 1), folder("x", 2))
        assertEquals(listOf(9L, 9L, 0L), markOpened(folders, "u", 9L).map { it.lastOpenedAt })
    }

    @Test
    fun markOpenedUnknownUriChangesNothing() {
        val folders = listOf(folder("u", 0))
        assertEquals(folders, markOpened(folders, "nope", 9L))
    }

    @Test
    fun reorderWithDuplicateStoredUrisCollapsesThemToOne() {
        val folders = listOf(folder("u", 0), folder("u", 1), folder("x", 2))
        val result = reorder(folders, listOf("x", "u"))
        assertEquals(listOf("x", "u"), result.map { it.uri })
        assertEquals(listOf(0, 1), result.map { it.order })
    }

    @Test
    fun uriVariantsWithTrailingSlashAreDistinctFolders() {
        val first = add(emptyList(), "content://tree/a", "A").folders
        val second = add(first, "content://tree/a/", "A")
        assertEquals(AddKind.Created, second.kind)
        assertEquals(2, second.folders.size)
    }

    // --- permission lost ---

    @Test
    fun applyPersistedPermissionsIgnoresUnknownGrantsAndEmptyList() {
        assertEquals(emptyList<Folder>(), applyPersistedPermissions(emptyList(), setOf("x")))
        val folders = listOf(folder("a", 0))
        assertEquals(folders, applyPersistedPermissions(folders, setOf("a", "unrelated")))
    }

    @Test
    fun markLostWithEmptySetOrUnknownUrisIsNoOp() {
        val folders = listOf(folder("a", 0), folder("b", 1, FolderAccess.Lost))
        assertEquals(folders, markLost(folders, emptySet()))
        assertEquals(folders, markLost(folders, setOf("zzz")))
    }

    @Test
    fun lostFolderKeepsOrderOpenedAtAndNameThroughRefresh() = runBlocking {
        val repo = repository("lost-keep.preferences_pb")
        repo.add("content://tree/a", "A")
        repo.add("content://tree/b", "B")
        repo.markOpened("content://tree/a", 77L)
        repo.rename("content://tree/a", "Mine", "A")
        permissions.persisted = setOf("content://tree/b")
        repo.refreshAccess()

        val a = repo.folders.first().first { it.uri == "content://tree/a" }
        assertEquals(FolderAccess.Lost, a.access)
        assertEquals("Mine", a.displayName)
        assertEquals(77L, a.lastOpenedAt)
        assertEquals(0, a.order)
    }

    @Test
    fun refreshAccessRestoresFolderWhenPermissionComesBack() = runBlocking {
        val repo = repository("lost-back.preferences_pb")
        repo.add("content://tree/a", "A")
        permissions.persisted = emptySet()
        repo.refreshAccess()
        assertEquals(FolderAccess.Lost, repo.folders.first().single().access)

        permissions.persisted = setOf("content://tree/a")
        repo.refreshAccess()
        assertEquals(FolderAccess.Ok, repo.folders.first().single().access)
    }

    @Test
    fun refreshAccessOnEmptyStoreStaysEmptyAndAddsNothingFromGrants() = runBlocking {
        val repo = repository("refresh-empty.preferences_pb")
        permissions.persisted = setOf("content://tree/stray")
        repo.refreshAccess()
        assertTrue(repo.folders.first().isEmpty())
    }

    @Test
    fun removeUnknownUriKeepsFoldersAndReleasesNothing() = runBlocking {
        val repo = repository("remove-unknown.preferences_pb")
        repo.add("content://tree/a", "A")
        repo.remove("content://tree/other")
        assertEquals(listOf("content://tree/a"), repo.folders.first().map { it.uri })
        assertTrue(permissions.released.isEmpty())
    }

    @Test
    fun repickLostFolderTakesNewGrantAndReleasesOldWhenStillHeld() = runBlocking {
        val repo = repository("repick.preferences_pb")
        repo.add("content://tree/old", "Old")
        repo.rename("content://tree/old", "Mine", "Old")
        repo.repick("content://tree/old", "content://tree/new")

        val only = repo.folders.first().single()
        assertEquals("content://tree/new", only.uri)
        assertEquals("Mine", only.displayName)
        assertEquals(FolderAccess.Ok, only.access)
        assertTrue("content://tree/new" in permissions.taken)
        assertEquals(listOf("content://tree/old"), permissions.released)
    }

    @Test
    fun repickWhenOldGrantAlreadyGoneDoesNotThrowOrRelease() = runBlocking {
        val repo = repository("repick-gone.preferences_pb")
        repo.add("content://tree/old", "Old")
        permissions.persisted = emptySet()
        permissions.throwOnReleaseMissing = true
        repo.refreshAccess()
        repo.repick("content://tree/old", "content://tree/new")

        assertEquals("content://tree/new", repo.folders.first().single().uri)
        assertTrue(permissions.released.isEmpty())
    }

    @Test
    fun repickDeniedByTakeLeavesFolderUntouched() = runBlocking {
        val repo = repository("repick-denied.preferences_pb")
        repo.add("content://tree/old", "Old")
        permissions.throwOnTake = setOf("content://tree/new")
        try {
            repo.repick("content://tree/old", "content://tree/new")
            fail("expected SecurityException")
        } catch (_: SecurityException) {
        }
        assertEquals("content://tree/old", repo.folders.first().single().uri)
    }

    @Test
    fun repickUnknownTargetStillTakesGrantButChangesNoFolder() = runBlocking {
        val repo = repository("repick-unknown.preferences_pb")
        repo.add("content://tree/a", "A")
        repo.repick("content://tree/missing", "content://tree/new")
        assertEquals(listOf("content://tree/a"), repo.folders.first().map { it.uri })
        assertTrue(permissions.released.isEmpty())
    }

    @Test
    fun repickOntoExistingFolderInStoreMergesIntoOne() = runBlocking {
        val repo = repository("repick-merge.preferences_pb")
        repo.add("content://tree/a", "A")
        repo.add("content://tree/b", "B")
        permissions.persisted = setOf("content://tree/b")
        repo.refreshAccess()
        repo.repick("content://tree/a", "content://tree/b")

        val folders = repo.folders.first()
        assertEquals(listOf("content://tree/b"), folders.map { it.uri })
        assertEquals(FolderAccess.Ok, folders.single().access)
        assertEquals(listOf(0), folders.map { it.order })
    }

    @Test
    fun replacePrimaryOverLostPrimaryKeepsSlotButResetsOpenedAt() {
        val folders = listOf(
            folder("old", 0, FolderAccess.Lost, opened = 50L),
            folder("other", 1),
        )
        val result = replacePrimary(folders, "new", "New")
        assertEquals(listOf("new", "other"), result.folders.map { it.uri })
        assertEquals(listOf(0, 1), result.folders.map { it.order })
        assertEquals(FolderAccess.Ok, result.folders[0].access)
        assertEquals(0L, result.folders[0].lastOpenedAt)
        assertEquals(listOf("old"), result.releaseUris)
    }

    // --- legacy migration ---

    @Test
    fun blankLegacyUriMigratesToNothing() {
        assertEquals(emptyList<Folder>(), migrateFromLegacy(emptyList(), "", "x"))
        assertEquals(emptyList<Folder>(), migrateFromLegacy(emptyList(), "   ", "x"))
        assertEquals(emptyList<Folder>(), migrateFromLegacy(emptyList(), null, "x"))
    }

    @Test
    fun existingFoldersWinOverLegacyUri() {
        val existing = listOf(folder("a", 0))
        assertEquals(existing, migrateFromLegacy(existing, "legacy", "L"))
    }

    @Test
    fun legacyMigrationRunsOnlyOnceEvenIfLegacyKeyChanges() = runBlocking {
        val data = store("legacy-once.preferences_pb")
        data.edit { it[FolderRepository.LegacyFolderKey] = "content://tree/first" }
        val repo = FolderRepository(data, permissions) { "First" }
        repo.migrateFromLegacyIfNeeded()
        repo.remove("content://tree/first")
        data.edit { it[FolderRepository.LegacyFolderKey] = "content://tree/second" }
        repo.migrateFromLegacyIfNeeded()
        assertTrue(repo.folders.first().isEmpty())
    }

    // --- display names ---

    @Test
    fun displayNameForUriEdgeShapes() {
        assertEquals("", displayNameForUri(""))
        assertEquals("", displayNameForUri("/"))
        assertEquals("Books", displayNameForUri("content://x/tree/primary%3ABooks/"))
        assertEquals("Books", displayNameForUri("content://x/tree/Books"))
        assertEquals("", displayNameForUri("content://x/tree/primary%3A"))
        assertEquals("c", displayNameForUri("content://x/tree/a:b:c"))
        assertEquals("만화", displayNameForUri("content://x/tree/primary%3A%EB%A7%8C%ED%99%94"))
    }

    @Test
    fun percentDecodeLeavesMalformedEscapesAlone() {
        assertEquals("50%", percentDecode("50%"))
        assertEquals("%zz", percentDecode("%zz"))
        assertEquals("%2", percentDecode("%2"))
        assertEquals("A", percentDecode("%41"))
        assertEquals("a%", percentDecode("a%25"))
        assertEquals("가", percentDecode("%EA%B0%80"))
        assertEquals("�", percentDecode("%EA%B0"))
        assertEquals("", percentDecode(""))
    }
}

private class StubPermissions : UriPermissions {
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

    override fun release(uri: String) =
        releasePersistableSafely(uri, persisted) {
            if (throwOnReleaseMissing) throw SecurityException("not held")
            released += it
            persisted = persisted - it
        }

    override fun persistedUris(): Set<String> = persisted
}
