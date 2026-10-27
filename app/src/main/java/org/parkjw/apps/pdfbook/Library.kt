package org.parkjw.apps.pdfbook

import java.security.MessageDigest

data class BookInfo(val uri: String, val name: String, val size: Long)

data class BookState(
    val page: Int = 1,
    val pageCount: Int = 0,
    val direction: ReadingDirection = ReadingDirection.RTL,
    val openedAt: Long = 0,
    val autoCrop: Boolean = false,
)

fun bookKey(uri: String, size: Long): String = "$uri|$size"

fun titleOf(fileName: String): String = fileName.removeSuffix(".pdf").removeSuffix(".PDF")

fun progressOf(page: Int, pageCount: Int): Float =
    if (pageCount <= 0) 0f else (page.toFloat() / pageCount).coerceIn(0f, 1f)

fun columnsFor(widthDp: Int): Int = if (widthDp < 600) 2 else (widthDp / 170).coerceIn(4, 5)

fun coverFileName(key: String): String =
    MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }

fun encodeState(state: BookState): String =
    "${state.page},${state.pageCount},${state.direction.name},${state.openedAt},${if (state.autoCrop) 1 else 0}"

fun decodeState(raw: String): BookState? {
    val parts = raw.split(",")
    if (parts.size !in 4..5) return null
    val page = parts[0].toIntOrNull() ?: return null
    val count = parts[1].toIntOrNull() ?: return null
    val direction = ReadingDirection.entries.firstOrNull { it.name == parts[2] } ?: return null
    val openedAt = parts[3].toLongOrNull() ?: return null
    val autoCrop = when (parts.getOrNull(4)) {
        null, "0" -> false
        "1" -> true
        else -> return null
    }
    return BookState(page, count, direction, openedAt, autoCrop)
}

fun bookOrder(states: Map<String, BookState>, byName: Boolean): Comparator<BookInfo> {
    val byTitle = compareBy<BookInfo, String>(NaturalOrder) { titleOf(it.name) }
    if (byName) return byTitle
    return compareByDescending<BookInfo> { states[bookKey(it.uri, it.size)]?.openedAt ?: 0L }.then(byTitle)
}

fun sortBooks(
    books: List<BookInfo>,
    states: Map<String, BookState>,
    byName: Boolean,
): List<BookInfo> = books.sortedWith(bookOrder(states, byName))
