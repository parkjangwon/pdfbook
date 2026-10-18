package org.parkjw.apps.pdfbook.hidden

enum class HiddenKind {
    Folder,
    Book,
}

data class HiddenItem(
    val kind: HiddenKind,
    val id: String,
    val name: String,
    val folderUri: String?,
)
