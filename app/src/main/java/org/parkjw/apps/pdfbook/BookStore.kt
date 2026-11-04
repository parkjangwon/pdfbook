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
private const val ADJUST_PREFIX = "adjust:"

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

    fun adjustFlow(key: String): Flow<ImageAdjust> =
        store.data.map { decodeAdjust(it[stringPreferencesKey(ADJUST_PREFIX + key)]) }
            .distinctUntilChanged()

    suspend fun adjust(key: String): ImageAdjust =
        decodeAdjust(store.data.first()[stringPreferencesKey(ADJUST_PREFIX + key)])

    suspend fun saveAdjust(key: String, adjust: ImageAdjust) {
        val clamped = adjust.clamped()
        store.edit {
            val k = stringPreferencesKey(ADJUST_PREFIX + key)
            if (clamped.isDefault) it.remove(k) else it[k] = encodeAdjust(clamped)
        }
    }

    suspend fun toggleBookmark(key: String, page: Int) {
        store.edit {
            val k = stringPreferencesKey(BOOKMARK_PREFIX + key)
            it[k] = encodeBookmarks(toggledBookmark(decodeBookmarks(it[k]), page))
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
