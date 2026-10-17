package org.parkjw.apps.pdfbook.folders

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

interface UriPermissions {
    fun take(uri: String)
    fun release(uri: String)
    fun persistedUris(): Set<String>
}

class ContentResolverUriPermissions(
    private val contentResolver: ContentResolver,
) : UriPermissions {
    override fun take(uri: String) {
        contentResolver.takePersistableUriPermission(
            Uri.parse(uri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun release(uri: String) {
        releasePersistableSafely(uri, persistedUris()) {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(it),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    override fun persistedUris(): Set<String> =
        readGrantedUris(
            contentResolver.persistedUriPermissions.map { it.uri.toString() to it.isReadPermission },
        )
}

fun readGrantedUris(entries: Iterable<Pair<String, Boolean>>): Set<String> =
    entries.mapNotNullTo(mutableSetOf()) { (uri, isRead) -> uri.takeIf { isRead } }

inline fun releasePersistableSafely(
    uri: String,
    persisted: Set<String>,
    release: (String) -> Unit,
) {
    if (uri !in persisted) return
    runCatching { release(uri) }
}
