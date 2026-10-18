package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import org.parkjw.apps.pdfbook.hidden.isDotName

data class AdjacentVolumes(val previous: BookInfo?, val next: BookInfo?)

data class BookListEntry(
    val book: BookInfo,
    val isCurrent: Boolean,
    val percent: Int?,
)

val VolumeOrder: Comparator<BookInfo> =
    compareBy<BookInfo, String>(NaturalOrder) { titleOf(it.name) }.thenBy { it.uri }

fun volumeList(
    books: List<BookInfo>,
    hiddenBookIds: Set<String>,
    showDotFiles: Boolean,
): List<BookInfo> =
    books
        .filter { it.uri !in hiddenBookIds && (showDotFiles || !isDotName(it.name)) }
        .sortedWith(VolumeOrder)

fun adjacentVolumes(volumes: List<BookInfo>, current: BookInfo): AdjacentVolumes {
    val previous = volumes.lastOrNull { it.uri != current.uri && VolumeOrder.compare(it, current) < 0 }
    val next = volumes.firstOrNull { it.uri != current.uri && VolumeOrder.compare(it, current) > 0 }
    return AdjacentVolumes(previous, next)
}

fun bookListEntries(
    volumes: List<BookInfo>,
    current: BookInfo,
    states: Map<String, BookState>,
): List<BookListEntry> {
    val books = if (volumes.any { it.uri == current.uri }) volumes else (volumes + current).sortedWith(VolumeOrder)
    return books.map { book ->
        val state = states[bookKey(book.uri, book.size)]
        BookListEntry(
            book = book,
            isCurrent = book.uri == current.uri,
            percent = state?.takeIf { it.pageCount > 0 }?.let { (progressOf(it.page, it.pageCount) * 100f).toInt() },
        )
    }
}

fun currentEntryIndex(entries: List<BookListEntry>): Int = entries.indexOfFirst { it.isCurrent }.coerceAtLeast(0)

fun openStateFor(saved: BookState?, defaultDirection: ReadingDirection): BookState =
    saved ?: BookState(direction = defaultDirection)

const val BOOK_CHANGED_TOAST_MILLIS = 1_500L

data class BookListUi(
    val folderName: String,
    val entries: List<BookListEntry>,
    val covers: Map<String, Bitmap?> = emptyMap(),
)

data class ReaderVolumes(
    val onPrevious: (() -> Unit)? = null,
    val onNext: (() -> Unit)? = null,
    val bookList: BookListUi? = null,
    val onPick: (BookInfo) -> Unit = {},
)
