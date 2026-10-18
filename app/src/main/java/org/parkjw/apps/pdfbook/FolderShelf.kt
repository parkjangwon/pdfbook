package org.parkjw.apps.pdfbook

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.folders.displayNameForUri
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

data class FolderResumeSummary(
    val title: String,
    val progress: Float,
    val percent: Int,
)

data class FolderCardUi(
    val uri: String,
    val displayName: String,
    val pathTail: String,
    val bookCount: Int,
    val coverKeys: List<String>,
    val resume: FolderResumeSummary?,
    val access: FolderAccess,
    val isHidden: Boolean = false,
)

data class GlobalResumeUi(
    val book: BookInfo,
    val folderUri: String,
    val state: BookState,
    val progress: Float,
    val percent: Int,
)

data class FolderShelfUi(
    val cards: List<FolderCardUi>,
    val globalResume: GlobalResumeUi?,
)

fun pathTailForUri(uri: String): String = displayNameForUri(uri)

fun folderShelfColumns(widthDp: Int): Int = if (widthDp < 600) 1 else 2

fun folderShelfScrollIndex(cardIndex: Int, hasGlobalResume: Boolean): Int =
    cardIndex + if (hasGlobalResume) 1 else 0

fun folderShelfIsLastRow(cardIndex: Int, cardCount: Int, columns: Int): Boolean {
    if (cardCount <= 0 || columns <= 0 || cardIndex < 0) return false
    val lastRowStart = ((cardCount - 1) / columns) * columns
    return cardIndex >= lastRowStart
}

fun folderPathBookLine(pathTail: String, bookCountLabel: String): String =
    if (pathTail.isEmpty()) bookCountLabel else "$pathTail · $bookCountLabel"

fun folderPathBookAnnotated(
    pathTail: String,
    bookCountLabel: String,
    numericStyle: SpanStyle,
): AnnotatedString =
    buildAnnotatedString {
        if (pathTail.isNotEmpty()) {
            append(pathTail)
            append(" · ")
        }
        append(withNumericSpans(bookCountLabel, numericStyle))
    }

fun sortFoldersByRecent(folders: List<Folder>): List<Folder> =
    folders.sortedWith(
        compareByDescending<Folder> { it.lastOpenedAt }.thenBy { it.order },
    )

fun buildFolderShelfUi(
    folders: List<Folder>,
    listings: Map<String, Listing?>,
    states: Map<String, BookState>,
): FolderShelfUi {
    val sorted = sortFoldersByRecent(folders)
    val cards = sorted.map { folder ->
        val listing = listings[folder.uri]
        val books = listing?.books.orEmpty()
        val resumeBook = sortBooks(books, states, byName = false).firstOrNull {
            (states[bookKey(it.uri, it.size)]?.openedAt ?: 0L) > 0L
        }
        val resumeState = resumeBook?.let { states[bookKey(it.uri, it.size)] }
        val resume = if (resumeBook != null && resumeState != null && resumeState.pageCount > 0) {
            val progress = progressOf(resumeState.page, resumeState.pageCount)
            FolderResumeSummary(
                title = titleOf(resumeBook.name),
                progress = progress,
                percent = (progress * 100f).toInt(),
            )
        } else {
            null
        }
        FolderCardUi(
            uri = folder.uri,
            displayName = folder.displayName,
            pathTail = pathTailForUri(folder.uri),
            bookCount = books.size,
            coverKeys = sortBooks(books, emptyMap(), byName = true).take(4).map { bookKey(it.uri, it.size) },
            resume = resume,
            access = folder.access,
        )
    }
    var best: GlobalResumeUi? = null
    for (folder in folders) {
        if (folder.access != FolderAccess.Ok) continue
        val books = listings[folder.uri]?.books.orEmpty()
        for (book in books) {
            val state = states[bookKey(book.uri, book.size)] ?: continue
            if (state.openedAt <= 0L) continue
            if (best == null || state.openedAt > best.state.openedAt) {
                val progress = progressOf(state.page, state.pageCount)
                best = GlobalResumeUi(
                    book = book,
                    folderUri = folder.uri,
                    state = state,
                    progress = progress,
                    percent = (progress * 100f).toInt(),
                )
            }
        }
    }
    return FolderShelfUi(cards = cards, globalResume = best)
}
