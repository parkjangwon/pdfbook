package dev.parkjw.pdfbook

fun spreads(pageCount: Int, twoPage: Boolean, coverAlone: Boolean): List<List<Int>> {
    if (pageCount <= 0) return emptyList()
    val pages = (1..pageCount).toList()
    if (!twoPage) return pages.map { listOf(it) }
    val head = if (coverAlone) listOf(listOf(1)) else emptyList()
    val rest = if (coverAlone) pages.drop(1) else pages
    return head + rest.chunked(2)
}
