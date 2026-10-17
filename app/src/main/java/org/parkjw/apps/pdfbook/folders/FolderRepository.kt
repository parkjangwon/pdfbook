package org.parkjw.apps.pdfbook.folders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.parkjw.apps.pdfbook.pdfbookDataStore

class FolderRepository(
    private val dataStore: DataStore<Preferences>,
    private val uriPermissions: UriPermissions,
    private val displayNameFor: (String) -> String = ::displayNameForUri,
) {
    constructor(context: Context, uriPermissions: UriPermissions) : this(
        context.applicationContext.pdfbookDataStore,
        uriPermissions,
    )

    val folders: Flow<List<Folder>> = dataStore.data.map { prefs ->
        decodeFolders(prefs[FoldersKey]).sortedBy { it.order }
    }

    suspend fun migrateFromLegacyIfNeeded() {
        dataStore.edit { migrateInto(it) }
    }

    suspend fun add(uri: String, displayName: String): AddKind {
        val preview = previewAdd(uri, displayName)
        if (preview == AddKind.Duplicate) return AddKind.Duplicate
        uriPermissions.take(uri)
        var kind = preview
        dataStore.edit { prefs ->
            migrateInto(prefs)
            val result = add(decodeFolders(prefs[FoldersKey]), uri, displayName)
            kind = result.kind
            if (result.kind != AddKind.Duplicate) {
                prefs[FoldersKey] = encodeFolders(result.folders)
            }
            prefs[MigratedKey] = true
        }
        return kind
    }

    suspend fun remove(uri: String) {
        var releaseUris = emptyList<String>()
        dataStore.edit { prefs ->
            migrateInto(prefs)
            val result = remove(decodeFolders(prefs[FoldersKey]), uri)
            prefs[FoldersKey] = encodeFolders(result.folders)
            prefs[MigratedKey] = true
            releaseUris = result.releaseUris
        }
        for (releaseUri in releaseUris) {
            uriPermissions.release(releaseUri)
        }
    }

    suspend fun rename(uri: String, displayName: String, defaultName: String) {
        dataStore.edit { prefs ->
            migrateInto(prefs)
            prefs[FoldersKey] = encodeFolders(
                rename(decodeFolders(prefs[FoldersKey]), uri, displayName, defaultName),
            )
            prefs[MigratedKey] = true
        }
    }

    suspend fun reorder(orderedUris: List<String>) {
        dataStore.edit { prefs ->
            migrateInto(prefs)
            prefs[FoldersKey] = encodeFolders(reorder(decodeFolders(prefs[FoldersKey]), orderedUris))
            prefs[MigratedKey] = true
        }
    }

    suspend fun refreshAccess() {
        dataStore.edit { prefs ->
            migrateInto(prefs)
            prefs[FoldersKey] = encodeFolders(
                applyPersistedPermissions(decodeFolders(prefs[FoldersKey]), uriPermissions.persistedUris()),
            )
            prefs[MigratedKey] = true
        }
    }

    suspend fun replacePrimary(uri: String, displayName: String) {
        uriPermissions.take(uri)
        var releaseUris = emptyList<String>()
        dataStore.edit { prefs ->
            migrateInto(prefs)
            val result = replacePrimary(decodeFolders(prefs[FoldersKey]), uri, displayName)
            prefs[FoldersKey] = encodeFolders(result.folders)
            prefs[MigratedKey] = true
            releaseUris = result.releaseUris
        }
        for (releaseUri in releaseUris) {
            uriPermissions.release(releaseUri)
        }
    }

    suspend fun repick(targetUri: String, newUri: String) {
        uriPermissions.take(newUri)
        var releaseUris = emptyList<String>()
        dataStore.edit { prefs ->
            migrateInto(prefs)
            val result = repick(decodeFolders(prefs[FoldersKey]), targetUri, newUri)
            prefs[FoldersKey] = encodeFolders(result.folders)
            prefs[MigratedKey] = true
            releaseUris = result.releaseUris
        }
        for (releaseUri in releaseUris) {
            uriPermissions.release(releaseUri)
        }
    }

    suspend fun markOpened(uri: String, at: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            migrateInto(prefs)
            prefs[FoldersKey] = encodeFolders(markOpened(decodeFolders(prefs[FoldersKey]), uri, at))
            prefs[MigratedKey] = true
        }
    }

    private suspend fun previewAdd(uri: String, displayName: String): AddKind {
        val prefs = dataStore.data.first()
        val current = if (prefs[MigratedKey] == true) {
            decodeFolders(prefs[FoldersKey])
        } else {
            migrateFromLegacy(
                folders = decodeFolders(prefs[FoldersKey]),
                legacyUri = prefs[LegacyFolderKey],
                displayName = prefs[LegacyFolderKey]?.let(displayNameFor) ?: "",
            )
        }
        return add(current, uri, displayName).kind
    }

    private fun migrateInto(prefs: MutablePreferences) {
        if (prefs[MigratedKey] == true) return
        val legacyUri = prefs[LegacyFolderKey]
        val migrated = migrateFromLegacy(
            folders = decodeFolders(prefs[FoldersKey]),
            legacyUri = legacyUri,
            displayName = legacyUri?.let(displayNameFor) ?: "",
        )
        prefs[FoldersKey] = encodeFolders(migrated)
        prefs[MigratedKey] = true
    }

    companion object {
        val FoldersKey = stringPreferencesKey("folders")
        val MigratedKey = booleanPreferencesKey("folders_migrated")
        val LegacyFolderKey = stringPreferencesKey("folder")
    }
}
