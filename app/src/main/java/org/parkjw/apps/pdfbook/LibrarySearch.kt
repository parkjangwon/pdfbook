package org.parkjw.apps.pdfbook

import java.text.Normalizer

data class LibraryMatches(
    val books: List<BookInfo>,
    val subfolders: List<SubfolderItem>,
)

fun searchKey(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFC).trim()

fun filterLibrary(
    books: List<BookInfo>,
    subfolders: List<SubfolderItem>,
    query: String,
): LibraryMatches {
    val key = searchKey(query)
    if (key.isEmpty()) return LibraryMatches(books, subfolders)
    fun matches(name: String): Boolean =
        Normalizer.normalize(name, Normalizer.Form.NFC).contains(key, ignoreCase = true)
    return LibraryMatches(
        books = books.filter { matches(titleOf(it.name)) },
        subfolders = subfolders.filter { matches(it.node.name) },
    )
}
