package org.parkjw.apps.pdfbook

import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess

enum class FolderContentPhase {
    None,
    Loading,
    AccessLost,
    Empty,
    Ready,
}

enum class FolderBackAction {
    None,
    CloseSettings,
    CloseReader,
    CloseLibrary,
    CloseLost,
}

data class LastBookTarget(val folderUri: String, val bookUri: String)

fun folderContentPhase(
    selectedFolderUri: String?,
    scanned: Boolean,
    listing: Listing?,
): FolderContentPhase {
    if (selectedFolderUri == null) return FolderContentPhase.None
    if (!scanned) return FolderContentPhase.Loading
    if (listing == null) return FolderContentPhase.AccessLost
    if (listing.books.isEmpty()) return FolderContentPhase.Empty
    return FolderContentPhase.Ready
}

fun canApplyStartScreen(
    startApplied: Boolean,
    loaded: Boolean,
    settingsReady: Boolean,
    statesReady: Boolean,
    folderCount: Int,
    shelfListingCount: Int,
): Boolean {
    if (startApplied || !loaded || !settingsReady || !statesReady) return false
    if (folderCount > 0 && shelfListingCount < folderCount) return false
    return true
}

fun shouldClearScrollTarget(cardIndex: Int): Boolean = cardIndex >= 0

fun listingWithFolderName(listing: Listing?, displayName: String?): Listing? = when {
    listing == null -> null
    displayName == null -> listing
    else -> listing.copy(folderName = displayName)
}

fun findLastBookTarget(
    folders: List<Folder>,
    listings: Map<String, Listing?>,
    states: Map<String, BookState>,
): LastBookTarget? {
    var best: LastBookTarget? = null
    var bestAt = 0L
    for (folder in folders) {
        if (folder.access != FolderAccess.Ok) continue
        for (book in listings[folder.uri]?.books.orEmpty()) {
            val openedAt = states[bookKey(book.uri, book.size)]?.openedAt ?: 0L
            if (openedAt > bestAt) {
                bestAt = openedAt
                best = LastBookTarget(folder.uri, book.uri)
            }
        }
    }
    return best
}

fun folderBackAction(
    showSettings: Boolean,
    readerOpen: Boolean,
    folderSelected: Boolean,
    showLost: Boolean,
): FolderBackAction = when {
    showSettings -> FolderBackAction.CloseSettings
    readerOpen -> FolderBackAction.CloseReader
    folderSelected && !showLost -> FolderBackAction.CloseLibrary
    showLost -> FolderBackAction.CloseLost
    else -> FolderBackAction.None
}
