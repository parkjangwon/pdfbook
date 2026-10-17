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
            folder.lastOpenedAt.toString(),
        ).joinToString(FIELD)
    }

fun decodeFolders(raw: String?): List<Folder> {
    if (raw.isNullOrEmpty()) return emptyList()
    return raw.split(RECORD).mapNotNull { record ->
        val parts = record.split(FIELD)
        when (parts.size) {
            4 -> {
                val order = parts[2].toIntOrNull() ?: return@mapNotNull null
                val access = runCatching { FolderAccess.valueOf(parts[3]) }.getOrNull()
                    ?: return@mapNotNull null
                Folder(
                    uri = parts[0],
                    displayName = parts[1],
                    order = order,
                    access = access,
                    lastOpenedAt = 0L,
                )
            }
            5 -> {
                val order = parts[2].toIntOrNull() ?: return@mapNotNull null
                val access = runCatching { FolderAccess.valueOf(parts[3]) }.getOrNull()
                    ?: return@mapNotNull null
                val lastOpenedAt = parts[4].toLongOrNull() ?: return@mapNotNull null
                Folder(
                    uri = parts[0],
                    displayName = parts[1],
                    order = order,
                    access = access,
                    lastOpenedAt = lastOpenedAt,
                )
            }
            else -> null
        }
    }
}
