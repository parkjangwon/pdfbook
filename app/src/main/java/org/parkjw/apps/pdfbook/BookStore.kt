package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val BOOK_PREFIX = "book:"
private const val BOOKMARK_PREFIX = "bookmarks:"
private const val FIT_PREFIX = "fit:"

class BookStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.pdfbookDataStore)

    val states: Flow<Map<String, BookState>> = store.data.map(::decodeAll)

    suspend fun state(key: String): BookState? =
        decodeAll(store.data.first())[key]

    suspend fun save(key: String, state: BookState) {
        store.edit { it[stringPreferencesKey(BOOK_PREFIX + key)] = encodeState(state) }
    }

    fun bookmarks(key: String, pageCount: Int = Int.MAX_VALUE): Flow<List<Int>> =
        store.data.map { decodeBookmarks(it[stringPreferencesKey(BOOKMARK_PREFIX + key)], pageCount) }
            .distinctUntilChanged()

    suspend fun toggleBookmark(key: String, page: Int) {
        store.edit {
            val k = stringPreferencesKey(BOOKMARK_PREFIX + key)
            it[k] = encodeBookmarks(toggledBookmark(decodeBookmarks(it[k]), page))
        }
    }

    fun fit(key: String): Flow<PageFit> =
        store.data.map { pageFitOrDefault(it[stringPreferencesKey(FIT_PREFIX + key)]) }
            .distinctUntilChanged()

    suspend fun saveFit(key: String, fit: PageFit) {
        store.edit {
            val k = stringPreferencesKey(FIT_PREFIX + key)
            if (fit == PageFit.Screen) it.remove(k) else it[k] = fit.name
        }
    }

    private fun decodeAll(prefs: Preferences): Map<String, BookState> =
        buildMap {
            for ((k, v) in prefs.asMap()) {
                if (!k.name.startsWith(BOOK_PREFIX)) continue
                decodeState(v as? String ?: continue)?.let { put(k.name.removePrefix(BOOK_PREFIX), it) }
            }
        }
}
