package org.parkjw.apps.pdfbook

/** One bookmark as the PDF stores it; [page] is 1-based and may be bad in broken files. */
data class OutlineNode(val title: String, val page: Int, val children: List<OutlineNode> = emptyList())

data class OutlineEntry(val title: String, val page: Int, val depth: Int)

/** Flattens the outline depth-first, dropping untitled entries and ones pointing outside the book. */
fun outlineEntries(nodes: List<OutlineNode>, pageCount: Int, depth: Int = 0): List<OutlineEntry> =
    nodes.flatMap { node ->
        val title = node.title.trim()
        val own = if (title.isEmpty() || node.page !in 1..pageCount) {
            emptyList()
        } else {
            listOf(OutlineEntry(title, node.page, depth))
        }
        own + outlineEntries(node.children, pageCount, depth + 1)
    }

/** The last entry that starts on or before [page], so the sheet can mark where the reader is. */
fun currentOutlineIndex(entries: List<OutlineEntry>, page: Int): Int =
    entries.indexOfLast { it.page <= page }

const val THUMB_WIDTH_DP = 72
const val THUMB_HEIGHT_DP = 100
const val THUMB_BUCKET_PX = 16
const val THUMB_CACHE_BYTES = 6L * 1024 * 1024

/** Rounds up to a bucket so nearby densities share cache keys. */
fun thumbRequest(page: Int, density: Float): PageRequest {
    fun px(dp: Int) = (Math.ceil((dp * density).toDouble() / THUMB_BUCKET_PX).toInt() * THUMB_BUCKET_PX)
        .coerceAtLeast(THUMB_BUCKET_PX)
    return PageRequest(page, px(THUMB_WIDTH_DP), px(THUMB_HEIGHT_DP))
}
