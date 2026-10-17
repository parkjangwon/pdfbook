package org.parkjw.apps.pdfbook.folders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderWorkspaceTest {
    private fun folder(
        uri: String,
        name: String = uri,
        order: Int = 0,
        access: FolderAccess = FolderAccess.Ok,
    ) = Folder(uri, name, order, access)

    @Test
    fun addAppendsWithNextOrder() {
        val first = add(emptyList(), "a", "A")
        assertEquals(AddKind.Created, first.kind)
        assertEquals(listOf(folder("a", "A", 0)), first.folders)

        val second = add(first.folders, "b", "B")
        assertEquals(AddKind.Created, second.kind)
        assertEquals(
            listOf(folder("a", "A", 0), folder("b", "B", 1)),
            second.folders,
        )
    }

    @Test
    fun addDuplicateKeepsExisting() {
        val base = listOf(folder("a", "Custom", 0), folder("b", "B", 1))
        val result = add(base, "a", "Other")
        assertEquals(AddKind.Duplicate, result.kind)
        assertEquals(base, result.folders)
    }

    @Test
    fun addRestoresLostKeepingNameAndOrder() {
        val base = listOf(
            folder("a", "Keep", 0, FolderAccess.Lost),
            folder("b", "B", 1),
        )
        val result = add(base, "a", "Ignored")
        assertEquals(AddKind.Restored, result.kind)
        assertEquals(
            listOf(folder("a", "Keep", 0, FolderAccess.Ok), folder("b", "B", 1)),
            result.folders,
        )
    }

    @Test
    fun removeReordersAndReturnsReleaseUri() {
        val base = listOf(folder("a", "A", 0), folder("b", "B", 1), folder("c", "C", 2))
        val result = remove(base, "b")
        assertEquals(listOf(folder("a", "A", 0), folder("c", "C", 1)), result.folders)
        assertEquals(listOf("b"), result.releaseUris)
    }

    @Test
    fun removeLastFolderLeavesEmpty() {
        val result = remove(listOf(folder("only", "Only", 0)), "only")
        assertTrue(result.folders.isEmpty())
        assertEquals(listOf("only"), result.releaseUris)
    }

    @Test
    fun removeMissingIsNoOp() {
        val base = listOf(folder("a", "A", 0))
        val result = remove(base, "missing")
        assertEquals(base, result.folders)
        assertTrue(result.releaseUris.isEmpty())
    }

    @Test
    fun renameBlankUsesDefault() {
        val base = listOf(folder("a", "Old", 0))
        assertEquals(listOf(folder("a", "Default", 0)), rename(base, "a", "  ", "Default"))
        assertEquals(listOf(folder("a", "Default", 0)), rename(base, "a", "", "Default"))
        assertEquals(listOf(folder("a", "New", 0)), rename(base, "a", "New", "Default"))
    }

    @Test
    fun markLostOnlyTouchesListedUris() {
        val base = listOf(folder("a", "A", 0), folder("b", "B", 1))
        assertEquals(
            listOf(folder("a", "A", 0, FolderAccess.Lost), folder("b", "B", 1)),
            markLost(base, setOf("a")),
        )
    }

    @Test
    fun applyPersistedPermissionsMarksMissingAsLostAndRestoresPresent() {
        val base = listOf(
            folder("a", "A", 0, FolderAccess.Ok),
            folder("b", "B", 1, FolderAccess.Lost),
        )
        assertEquals(
            listOf(
                folder("a", "A", 0, FolderAccess.Lost),
                folder("b", "B", 1, FolderAccess.Ok),
            ),
            applyPersistedPermissions(base, setOf("b")),
        )
    }

    @Test
    fun reorderAssignsDenseOrders() {
        val base = listOf(folder("a", "A", 0), folder("b", "B", 1), folder("c", "C", 2))
        assertEquals(
            listOf(folder("c", "C", 0), folder("a", "A", 1), folder("b", "B", 2)),
            reorder(base, listOf("c", "a", "b")),
        )
    }

    @Test
    fun migrateFromLegacyCreatesFirstFolderOnce() {
        val migrated = migrateFromLegacy(emptyList(), "content://tree/old", "Old")
        assertEquals(listOf(folder("content://tree/old", "Old", 0)), migrated)
        assertEquals(migrated, migrateFromLegacy(migrated, "content://tree/old", "Ignored"))
        assertTrue(migrateFromLegacy(emptyList(), null, "X").isEmpty())
        assertTrue(migrateFromLegacy(emptyList(), "  ", "X").isEmpty())
    }

    @Test
    fun displayNameForUriUsesDecodedSegmentAfterColon() {
        assertEquals(
            "Books",
            displayNameForUri("content://com.android.externalstorage.documents/tree/primary%3ABooks"),
        )
        assertEquals(
            "My Docs",
            displayNameForUri("content://com.android.externalstorage.documents/tree/primary%3AMy%20Docs"),
        )
        assertEquals("plain", displayNameForUri("content://authority/tree/plain"))
    }

    @Test
    fun codecRoundTrip() {
        val folders = listOf(
            folder("content://a", "Name|Weird", 0, FolderAccess.Lost),
            folder("content://b", "B", 1),
        )
        assertEquals(folders, decodeFolders(encodeFolders(folders)))
        assertTrue(decodeFolders(null).isEmpty())
        assertTrue(decodeFolders("").isEmpty())
    }
}
