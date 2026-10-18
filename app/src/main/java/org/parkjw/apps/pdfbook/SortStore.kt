package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SORT_PREFIX = "sort:"
private const val PATH_SEPARATOR = "\u0001"

enum class BookSort {
    Recent,
    Name,
}

fun decodeSort(raw: String?): BookSort? = BookSort.entries.firstOrNull { it.name == raw }

fun sortKey(rootUri: String, path: List<String> = emptyList()): String =
    if (path.isEmpty()) rootUri else rootUri + PATH_SEPARATOR + path.joinToString(PATH_SEPARATOR)

fun sortKeyChain(rootUri: String, path: List<String> = emptyList()): List<String> =
    (path.size downTo 0).map { sortKey(rootUri, path.take(it)) }

fun sortFor(sorts: Map<String, BookSort>, rootUri: String?, path: List<String> = emptyList()): BookSort {
    if (rootUri == null) return BookSort.Recent
    return sortKeyChain(rootUri, path).firstNotNullOfOrNull { sorts[it] } ?: BookSort.Recent
}

fun belongsToRoot(key: String, rootUri: String): Boolean =
    key == rootUri || key.startsWith(rootUri + PATH_SEPARATOR)

class SortStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.pdfbookDataStore)

    val sorts: Flow<Map<String, BookSort>> = store.data.map(::decodeAll)

    suspend fun save(key: String, sort: BookSort) {
        store.edit { it[stringPreferencesKey(SORT_PREFIX + key)] = sort.name }
    }

    suspend fun removeRoot(rootUri: String) {
        store.edit { prefs ->
            val stale = prefs.asMap().keys.map { it.name }.filter {
                it.startsWith(SORT_PREFIX) && belongsToRoot(it.removePrefix(SORT_PREFIX), rootUri)
            }
            for (name in stale) prefs.remove(stringPreferencesKey(name))
        }
    }

    private fun decodeAll(prefs: Preferences): Map<String, BookSort> =
        buildMap {
            for ((k, v) in prefs.asMap()) {
                if (!k.name.startsWith(SORT_PREFIX)) continue
                decodeSort(v as? String)?.let { put(k.name.removePrefix(SORT_PREFIX), it) }
            }
        }
}
