package org.parkjw.apps.pdfbook

enum class DoubleBackDecision {
    ShowHint,
    Exit,
}

class DoubleBackExit(private val windowMs: Long = 2_000L) {
    private var hintUntilMs: Long = Long.MIN_VALUE

    fun onBack(nowMs: Long): DoubleBackDecision {
        if (nowMs <= hintUntilMs) {
            hintUntilMs = Long.MIN_VALUE
            return DoubleBackDecision.Exit
        }
        hintUntilMs = nowMs + windowMs
        return DoubleBackDecision.ShowHint
    }

    fun reset() {
        hintUntilMs = Long.MIN_VALUE
    }
}

fun showsRootFolderList(
    showSettings: Boolean,
    showLost: Boolean,
    loaded: Boolean,
    readerOpen: Boolean,
    contentPhase: FolderContentPhase,
    folderCount: Int,
): Boolean {
    if (showSettings || showLost || !loaded || readerOpen) return false
    if (contentPhase != FolderContentPhase.None) return false
    return folderCount > 0
}
