package org.parkjw.apps.pdfbook

import kotlin.math.abs

const val NEXT_BOOK_NOTICE_MILLIS = 5_000L
const val NO_NEXT_BOOK_NOTICE_MILLIS = 2_000L
const val END_NOTICE_FADE_MILLIS = 150

fun nextBookIn(
    folderBooks: List<BookInfo>,
    current: BookInfo,
    hiddenBookIds: Set<String>,
    showDotFiles: Boolean,
    sort: BookSort,
    states: Map<String, BookState>,
): BookInfo? {
    val order = volumeOrder(sort, states)
    return adjacentVolumes(volumeList(folderBooks, hiddenBookIds, showDotFiles, order), current, order).next
}

sealed interface EndNotice {
    data class NextBook(val book: BookInfo) : EndNotice
    data object NoNextBook : EndNotice
}

sealed interface EndStep {
    data class Show(val notice: EndNotice) : EndStep
    data class Open(val book: BookInfo) : EndStep
}

fun endStep(visible: EndNotice?, next: BookInfo?): EndStep =
    when {
        next == null -> EndStep.Show(EndNotice.NoNextBook)
        visible is EndNotice.NextBook && visible.book.uri == next.uri -> EndStep.Open(next)
        else -> EndStep.Show(EndNotice.NextBook(next))
    }

fun endNoticeMillis(notice: EndNotice): Long =
    when (notice) {
        is EndNotice.NextBook -> NEXT_BOOK_NOTICE_MILLIS
        EndNotice.NoNextBook -> NO_NEXT_BOOK_NOTICE_MILLIS
    }

fun isTurnPastEnd(currentIndex: Int, delta: Int, pageCount: Int): Boolean =
    delta > 0 && pageCount > 0 && currentIndex >= pageCount - 1

fun isSwipeTowardNext(dx: Float, dy: Float, threshold: Float, direction: ReadingDirection): Boolean {
    if (abs(dx) <= abs(dy) || abs(dx) < threshold) return false
    return if (direction == ReadingDirection.RTL) dx > 0f else dx < 0f
}

fun folderBooksOf(root: FolderNode?, bookUri: String, fallback: List<BookInfo>): List<BookInfo> =
    root?.pathOfBook(bookUri)?.let { root.nodeAt(it) }?.books ?: fallback
