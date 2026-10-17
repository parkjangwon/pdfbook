package org.parkjw.apps.pdfbook

import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.folders.displayNameForUri

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
            coverKeys = books.take(4).map { bookKey(it.uri, it.size) },
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
