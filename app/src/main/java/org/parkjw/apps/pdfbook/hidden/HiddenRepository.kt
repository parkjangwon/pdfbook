package org.parkjw.apps.pdfbook.hidden

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hiddenDataStore by preferencesDataStore(name = "hidden")

class HiddenRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.hiddenDataStore)

    val items: Flow<List<HiddenItem>> = dataStore.data.map { prefs ->
        decodeHiddenItems(prefs[ItemsKey])
    }

    suspend fun hide(item: HiddenItem) {
        dataStore.edit { prefs ->
            val current = decodeHiddenItems(prefs[ItemsKey])
                .filterNot { it.kind == item.kind && it.id == item.id }
            prefs[ItemsKey] = encodeHiddenItems(listOf(item) + current)
        }
    }

    suspend fun unhide(kind: HiddenKind, id: String) {
        dataStore.edit { prefs ->
            val current = decodeHiddenItems(prefs[ItemsKey])
            prefs[ItemsKey] = encodeHiddenItems(current.filterNot { it.kind == kind && it.id == id })
        }
    }

    companion object {
        val ItemsKey = stringPreferencesKey("items")
    }
}
