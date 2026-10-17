package org.parkjw.apps.pdfbook.folders

import android.content.Context
import androidx.datastore.core.DataStore
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
) {
    constructor(context: Context, uriPermissions: UriPermissions) : this(
        context.applicationContext.pdfbookDataStore,
        uriPermissions,
    )

    val folders: Flow<List<Folder>> = dataStore.data.map { prefs ->
        decodeFolders(prefs[FoldersKey]).sortedBy { it.order }
    }

    suspend fun migrateFromLegacyIfNeeded(displayNameFor: (String) -> String) {
        dataStore.edit { prefs ->
            if (prefs[MigratedKey] == true) return@edit
            val current = decodeFolders(prefs[FoldersKey])
            val legacyUri = prefs[LegacyFolderKey]
            val migrated = migrateFromLegacy(
                folders = current,
                legacyUri = legacyUri,
                displayName = legacyUri?.let(displayNameFor) ?: "",
            )
            prefs[FoldersKey] = encodeFolders(migrated)
            prefs[MigratedKey] = true
        }
    }

    suspend fun add(uri: String, displayName: String): AddKind {
        migrateFromLegacyIfNeeded { displayName }
        val result = add(folders.first(), uri, displayName)
        when (result.kind) {
            AddKind.Created, AddKind.Restored -> {
                uriPermissions.take(uri)
                persist(result.folders)
            }
            AddKind.Duplicate -> Unit
        }
        return result.kind
    }

    suspend fun remove(uri: String) {
        migrateFromLegacyIfNeeded { uri }
        val result = remove(folders.first(), uri)
        persist(result.folders)
        for (releaseUri in result.releaseUris) {
            uriPermissions.release(releaseUri)
        }
    }

    suspend fun rename(uri: String, displayName: String, defaultName: String) {
        migrateFromLegacyIfNeeded { defaultName }
        persist(rename(folders.first(), uri, displayName, defaultName))
    }

    suspend fun reorder(orderedUris: List<String>) {
        migrateFromLegacyIfNeeded { it }
        persist(reorder(folders.first(), orderedUris))
    }

    suspend fun refreshAccess() {
        migrateFromLegacyIfNeeded { it }
        persist(applyPersistedPermissions(folders.first(), uriPermissions.persistedUris()))
    }

    private suspend fun persist(folders: List<Folder>) {
        dataStore.edit { prefs ->
            prefs[FoldersKey] = encodeFolders(folders)
            prefs[MigratedKey] = true
        }
    }

    companion object {
        val FoldersKey = stringPreferencesKey("folders")
        val MigratedKey = booleanPreferencesKey("folders_migrated")
        val LegacyFolderKey = stringPreferencesKey("folder")
    }
}
