package org.parkjw.apps.pdfbook.hidden

import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.FolderShelfUi
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.buildFolderShelfUi
import org.parkjw.apps.pdfbook.folders.Folder

data class ShelfFolderItem(
    val folder: Folder,
    val isHidden: Boolean,
)

data class LibraryBookItem(
    val book: BookInfo,
    val isHidden: Boolean,
)

fun isDotName(name: String): Boolean = name.startsWith('.')

fun visibleFolders(folders: List<Folder>, hidden: List<HiddenItem>): List<Folder> =
    shelfFolders(folders, hidden, peek = false).map { it.folder }

fun hiddenBookIds(hidden: List<HiddenItem>): Set<String> =
    hidden.mapNotNullTo(linkedSetOf()) { item ->
        if (item.kind == HiddenKind.Book) item.id else null
    }

fun hiddenFolderIds(hidden: List<HiddenItem>): Set<String> =
    hidden.mapNotNullTo(linkedSetOf()) { item ->
        if (item.kind == HiddenKind.Folder) item.id else null
    }

fun filterListing(
    listing: Listing,
    hiddenBookIds: Set<String>,
    showDotFiles: Boolean,
): Listing {
    val books = listing.books.filter { book ->
        book.uri !in hiddenBookIds && (showDotFiles || !isDotName(book.name))
    }
    return if (books.size == listing.books.size) listing else listing.copy(books = books)
}

fun shelfFolders(
    folders: List<Folder>,
    hidden: List<HiddenItem>,
    peek: Boolean,
): List<ShelfFolderItem> {
    val hiddenIds = hiddenFolderIds(hidden)
    return folders.mapNotNull { folder ->
        val isHidden = folder.uri in hiddenIds
        when {
            !isHidden -> ShelfFolderItem(folder, isHidden = false)
            peek -> ShelfFolderItem(folder, isHidden = true)
            else -> null
        }
    }
}

fun libraryBooks(
    books: List<BookInfo>,
    hiddenBookIds: Set<String>,
    peek: Boolean,
    showDotFiles: Boolean,
): List<LibraryBookItem> =
    books.mapNotNull { book ->
        if (!showDotFiles && isDotName(book.name)) return@mapNotNull null
        val isHidden = book.uri in hiddenBookIds
        when {
            !isHidden -> LibraryBookItem(book, isHidden = false)
            peek -> LibraryBookItem(book, isHidden = true)
            else -> null
        }
    }

fun buildPeekShelfUi(
    displayFolders: List<Folder>,
    resumeFolders: List<Folder>,
    peekHiddenFolderIds: Set<String>,
    listings: Map<String, Listing?>,
    states: Map<String, BookState>,
): FolderShelfUi {
    val built = buildFolderShelfUi(displayFolders, listings, states)
    val resumeOnly = buildFolderShelfUi(resumeFolders, listings, states)
    return FolderShelfUi(
        cards = built.cards.map { card ->
            val hidden = card.uri in peekHiddenFolderIds
            card.copy(isHidden = hidden, resume = if (hidden) null else card.resume)
        },
        globalResume = resumeOnly.globalResume,
    )
}

fun findLibraryResumeBook(
    sortedBooks: List<BookInfo>,
    states: Map<String, BookState>,
    hiddenBookIds: Set<String>,
): BookInfo? =
    sortedBooks.firstOrNull { book ->
        book.uri !in hiddenBookIds && (states[bookKey(book.uri, book.size)]?.openedAt ?: 0L) > 0L
    }

fun peekMenuEnabled(hiddenCount: Int): Boolean = hiddenCount > 0

fun shouldUnhideOnLongPress(isHiddenMarked: Boolean): Boolean = isHiddenMarked
