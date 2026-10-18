package org.parkjw.apps.pdfbook.hidden

import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.folders.Folder

fun isDotName(name: String): Boolean = name.startsWith('.')

fun visibleFolders(folders: List<Folder>, hidden: List<HiddenItem>): List<Folder> {
    val hiddenIds = hidden.asSequence()
        .filter { it.kind == HiddenKind.Folder }
        .map { it.id }
        .toSet()
    if (hiddenIds.isEmpty()) return folders
    return folders.filter { it.uri !in hiddenIds }
}

fun hiddenBookIds(hidden: List<HiddenItem>): Set<String> =
    hidden.mapNotNullTo(linkedSetOf()) { item ->
        if (item.kind == HiddenKind.Book) item.id else null
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
