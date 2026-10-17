package org.parkjw.apps.pdfbook.folders

enum class FolderAccess {
    Ok,
    Lost,
}

data class Folder(
    val uri: String,
    val displayName: String,
    val order: Int,
    val access: FolderAccess = FolderAccess.Ok,
)

enum class AddKind {
    Created,
    Duplicate,
    Restored,
}

data class AddResult(
    val folders: List<Folder>,
    val kind: AddKind,
)

data class RemoveResult(
    val folders: List<Folder>,
    val releaseUris: List<String>,
)

fun add(folders: List<Folder>, uri: String, displayName: String): AddResult {
    val index = folders.indexOfFirst { it.uri == uri }
    if (index >= 0) {
        val existing = folders[index]
        if (existing.access == FolderAccess.Lost) {
            val updated = folders.toMutableList()
            updated[index] = existing.copy(access = FolderAccess.Ok)
            return AddResult(updated, AddKind.Restored)
        }
        return AddResult(folders, AddKind.Duplicate)
    }
    val order = folders.maxOfOrNull { it.order }?.plus(1) ?: 0
    return AddResult(
        folders + Folder(uri = uri, displayName = displayName, order = order, access = FolderAccess.Ok),
        AddKind.Created,
    )
}

fun remove(folders: List<Folder>, uri: String): RemoveResult {
    if (folders.none { it.uri == uri }) {
        return RemoveResult(folders, emptyList())
    }
    val remaining = folders
        .filterNot { it.uri == uri }
        .sortedBy { it.order }
        .mapIndexed { index, folder -> folder.copy(order = index) }
    val releaseUris = if (remaining.none { it.uri == uri }) listOf(uri) else emptyList()
    return RemoveResult(remaining, releaseUris)
}

fun rename(folders: List<Folder>, uri: String, displayName: String, defaultName: String): List<Folder> {
    val resolved = displayName.ifBlank { defaultName }
    return folders.map { folder ->
        if (folder.uri == uri) folder.copy(displayName = resolved) else folder
    }
}

fun markLost(folders: List<Folder>, lostUris: Set<String>): List<Folder> =
    folders.map { folder ->
        if (folder.uri in lostUris && folder.access != FolderAccess.Lost) {
            folder.copy(access = FolderAccess.Lost)
        } else {
            folder
        }
    }

fun applyPersistedPermissions(folders: List<Folder>, persistedUris: Set<String>): List<Folder> =
    folders.map { folder ->
        val access = if (folder.uri in persistedUris) FolderAccess.Ok else FolderAccess.Lost
        if (folder.access == access) folder else folder.copy(access = access)
    }

fun reorder(folders: List<Folder>, orderedUris: List<String>): List<Folder> {
    val byUri = folders.associateBy { it.uri }
    return orderedUris.mapIndexedNotNull { index, uri ->
        byUri[uri]?.copy(order = index)
    }
}

fun migrateFromLegacy(folders: List<Folder>, legacyUri: String?, displayName: String): List<Folder> {
    if (folders.isNotEmpty()) return folders
    if (legacyUri.isNullOrBlank()) return folders
    return listOf(
        Folder(uri = legacyUri, displayName = displayName, order = 0, access = FolderAccess.Ok),
    )
}
