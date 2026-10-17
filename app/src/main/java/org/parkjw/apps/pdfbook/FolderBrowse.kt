package org.parkjw.apps.pdfbook

enum class FolderContentPhase {
    None,
    Loading,
    AccessLost,
    Empty,
    Ready,
}

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
