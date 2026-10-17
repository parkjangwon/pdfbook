package org.parkjw.apps.pdfbook.folders

private const val FIELD = "\u0001"
private const val RECORD = "\u0002"

fun encodeFolders(folders: List<Folder>): String =
    folders.joinToString(RECORD) { folder ->
        listOf(
            folder.uri,
            folder.displayName,
            folder.order.toString(),
            folder.access.name,
        ).joinToString(FIELD)
    }

fun decodeFolders(raw: String?): List<Folder> {
    if (raw.isNullOrEmpty()) return emptyList()
    return raw.split(RECORD).mapNotNull { record ->
        val parts = record.split(FIELD)
        if (parts.size != 4) return@mapNotNull null
        val order = parts[2].toIntOrNull() ?: return@mapNotNull null
        val access = runCatching { FolderAccess.valueOf(parts[3]) }.getOrNull() ?: return@mapNotNull null
        Folder(uri = parts[0], displayName = parts[1], order = order, access = access)
    }
}
