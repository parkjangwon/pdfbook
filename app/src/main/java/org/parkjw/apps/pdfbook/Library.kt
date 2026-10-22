package org.parkjw.apps.pdfbook

import java.security.MessageDigest

data class BookInfo(val uri: String, val name: String, val size: Long)

data class BookState(
    val page: Int = 1,
    val pageCount: Int = 0,
    val direction: ReadingDirection = ReadingDirection.RTL,
    val openedAt: Long = 0,
)

fun bookKey(uri: String, size: Long): String = "$uri|$size"

fun titleOf(fileName: String): String = fileName.removeSuffix(".pdf").removeSuffix(".PDF")

fun progressOf(page: Int, pageCount: Int): Float =
    if (pageCount <= 0) 0f else (page.toFloat() / pageCount).coerceIn(0f, 1f)

fun canMarkFinished(state: BookState?): Boolean =
    state != null && state.pageCount > 0 && state.page < state.pageCount

fun canStartOver(state: BookState?): Boolean = state != null && state.openedAt > 0L

fun markFinished(state: BookState): BookState =
    if (state.pageCount <= 0) state else state.copy(page = state.pageCount)

fun startOver(state: BookState): BookState = state.copy(page = 1, openedAt = 0L)

fun columnsFor(widthDp: Int): Int = if (widthDp < 600) 2 else (widthDp / 170).coerceIn(4, 5)

fun coverFileName(key: String): String =
    MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }

fun encodeState(state: BookState): String =
    "${state.page},${state.pageCount},${state.direction.name},${state.openedAt}"

fun decodeState(raw: String): BookState? {
    val parts = raw.split(",")
    if (parts.size != 4) return null
    val page = parts[0].toIntOrNull() ?: return null
    val count = parts[1].toIntOrNull() ?: return null
    val direction = ReadingDirection.entries.firstOrNull { it.name == parts[2] } ?: return null
    val openedAt = parts[3].toLongOrNull() ?: return null
    return BookState(page, count, direction, openedAt)
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
