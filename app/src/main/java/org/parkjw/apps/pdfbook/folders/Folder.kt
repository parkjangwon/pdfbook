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
    val lastOpenedAt: Long = 0L,
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

data class ReplacePrimaryResult(
    val folders: List<Folder>,
    val releaseUris: List<String>,
)

data class RepickResult(
    val folders: List<Folder>,
    val releaseUris: List<String>,
)

fun primaryWorkspaceUri(loaded: Boolean, folders: List<Folder>?): String? =
    if (!loaded || folders == null) null else folders.firstOrNull()?.uri

fun displayNameForUri(uri: String): String {
    val encoded = uri.trimEnd('/').substringAfterLast('/')
    val decoded = percentDecode(encoded)
    val colon = decoded.lastIndexOf(':')
    return if (colon >= 0) decoded.substring(colon + 1) else decoded
}

fun percentDecode(value: String): String {
    if ('%' !in value) return value
    val bytes = ByteArray(value.length * 3)
    var length = 0
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '%' && index + 2 < value.length) {
            val high = value[index + 1].digitToIntOrNull(16)
            val low = value[index + 2].digitToIntOrNull(16)
            if (high != null && low != null) {
                bytes[length++] = ((high shl 4) + low).toByte()
                index += 3
                continue
            }
        }
        val end =
            if (char.isHighSurrogate() &&
                index + 1 < value.length &&
                value[index + 1].isLowSurrogate()
            ) {
                index + 2
            } else {
                index + 1
            }
        val utf8 = value.substring(index, end).toByteArray(Charsets.UTF_8)
        utf8.copyInto(bytes, length)
        length += utf8.size
        index = end
    }
    return String(bytes, 0, length, Charsets.UTF_8)
}

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
        folders + Folder(
            uri = uri,
            displayName = displayName,
            order = order,
            access = FolderAccess.Ok,
            lastOpenedAt = 0L,
        ),
        AddKind.Created,
    )
}

fun markOpened(folders: List<Folder>, uri: String, at: Long): List<Folder> =
    folders.map { folder ->
        if (folder.uri == uri) folder.copy(lastOpenedAt = at) else folder
    }

fun repick(folders: List<Folder>, targetUri: String, newUri: String): RepickResult {
    val targetIndex = folders.indexOfFirst { it.uri == targetUri }
    if (targetIndex < 0) return RepickResult(folders, emptyList())
    val target = folders[targetIndex]
    if (targetUri == newUri) {
        return RepickResult(
            folders.map {
                if (it.uri == targetUri) it.copy(access = FolderAccess.Ok) else it
            },
            emptyList(),
        )
    }
    if (folders.any { it.uri == newUri }) {
        val removed = remove(folders, targetUri)
        return RepickResult(
            removed.folders.map {
                if (it.uri == newUri) it.copy(access = FolderAccess.Ok) else it
            },
            removed.releaseUris,
        )
    }
    val updated = folders.map {
        if (it.uri == targetUri) {
            target.copy(uri = newUri, access = FolderAccess.Ok)
        } else {
            it
        }
    }
    val releaseUris = if (updated.none { it.uri == targetUri }) listOf(targetUri) else emptyList()
    return RepickResult(updated, releaseUris)
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
    val seen = linkedSetOf<String>()
    val reordered = mutableListOf<Folder>()
    for (uri in orderedUris) {
        if (!seen.add(uri)) continue
        val folder = byUri[uri] ?: continue
        reordered += folder
    }
    for (folder in folders.sortedBy { it.order }) {
        if (seen.add(folder.uri)) reordered += folder
    }
    return reordered.mapIndexed { index, folder -> folder.copy(order = index) }
}

fun migrateFromLegacy(folders: List<Folder>, legacyUri: String?, displayName: String): List<Folder> {
    if (folders.isNotEmpty()) return folders
    if (legacyUri.isNullOrBlank()) return folders
    return listOf(
        Folder(uri = legacyUri, displayName = displayName, order = 0, access = FolderAccess.Ok),
    )
}

fun replacePrimary(folders: List<Folder>, uri: String, displayName: String): ReplacePrimaryResult {
    if (folders.isEmpty()) {
        return ReplacePrimaryResult(
            listOf(Folder(uri = uri, displayName = displayName, order = 0, access = FolderAccess.Ok)),
            emptyList(),
        )
    }
    val primary = folders.minBy { it.order }
    if (primary.uri == uri) {
        return ReplacePrimaryResult(
            folders.map {
                if (it.uri == uri && it.access != FolderAccess.Ok) it.copy(access = FolderAccess.Ok) else it
            },
            emptyList(),
        )
    }
    val existing = folders.find { it.uri == uri }
    if (existing != null) {
        val rest = folders.filter { it.uri != uri }.sortedBy { it.order }
        val promoted = existing.copy(order = 0, access = FolderAccess.Ok)
        val updated = listOf(promoted) + rest.mapIndexed { index, folder -> folder.copy(order = index + 1) }
        return ReplacePrimaryResult(updated, emptyList())
    }
    val updated = folders.map {
        if (it.uri == primary.uri) {
            Folder(uri = uri, displayName = displayName, order = it.order, access = FolderAccess.Ok)
        } else {
            it
        }
    }
    val releaseUris = if (updated.none { it.uri == primary.uri }) listOf(primary.uri) else emptyList()
    return ReplacePrimaryResult(updated, releaseUris)
}
