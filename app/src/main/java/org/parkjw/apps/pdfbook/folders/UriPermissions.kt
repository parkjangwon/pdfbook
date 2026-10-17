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
        contentResolver.releasePersistableUriPermission(
            Uri.parse(uri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun persistedUris(): Set<String> =
        contentResolver.persistedUriPermissions.mapTo(mutableSetOf()) { it.uri.toString() }
}
