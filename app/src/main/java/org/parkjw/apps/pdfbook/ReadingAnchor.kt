package org.parkjw.apps.pdfbook

/**
 * Reading position that survives fold/unfold and two-page mode changes.
 *
 * The anchor page changes only when the user settles on a spread that does not
 * contain it (then anchor = first page of that spread). While spreads are being
 * remapped, [pendingIndex] is the target spread index; settles are ignored until
 * the pager reaches it so a stale settle cannot overwrite the anchor (ZF-3).
 */
data class ReadingAnchor(
    val page: Int,
    val pendingIndex: Int? = null,
)

fun readingAnchorOf(page: Int): ReadingAnchor = ReadingAnchor(page = page)

fun readingAnchorBeginSync(anchor: ReadingAnchor, spreads: List<List<Int>>): ReadingAnchor {
    val index = spreadIndexOf(spreads, anchor.page)
    return anchor.copy(pendingIndex = index)
}

fun readingAnchorAfterSettle(
    anchor: ReadingAnchor,
    spreads: List<List<Int>>,
    settledIndex: Int,
): ReadingAnchor {
    val pending = anchor.pendingIndex
    if (pending != null) {
        return if (settledIndex == pending) anchor.copy(pendingIndex = null) else anchor
    }
    val spread = spreads.getOrNull(settledIndex) ?: return anchor
    return if (anchor.page in spread) anchor else ReadingAnchor(page = spread.first())
}

fun readingAnchorTargetIndex(spreads: List<List<Int>>, anchor: ReadingAnchor): Int =
    spreadIndexOf(spreads, anchor.page)
