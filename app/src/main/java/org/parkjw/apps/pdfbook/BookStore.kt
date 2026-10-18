package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val BOOK_PREFIX = "book:"

class BookStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.pdfbookDataStore)

    val states: Flow<Map<String, BookState>> = store.data.map(::decodeAll)

    suspend fun state(key: String): BookState? =
        decodeAll(store.data.first())[key]

    suspend fun save(key: String, state: BookState) {
        store.edit { it[stringPreferencesKey(BOOK_PREFIX + key)] = encodeState(state) }
    }

    private fun decodeAll(prefs: Preferences): Map<String, BookState> =
        buildMap {
            for ((k, v) in prefs.asMap()) {
                if (!k.name.startsWith(BOOK_PREFIX)) continue
                decodeState(v as? String ?: continue)?.let { put(k.name.removePrefix(BOOK_PREFIX), it) }
            }
        }
}
