package org.parkjw.apps.pdfbook

fun spreads(
    pageCount: Int,
    twoPage: Boolean,
    coverAlone: Boolean,
    shift: Int = 0,
    widePages: Set<Int> = emptySet(),
): List<List<Int>> {
    if (pageCount <= 0) return emptyList()
    val pages = (1..pageCount).toList()
    if (!twoPage) return pages.map { listOf(it) }
    val lone = (if (coverAlone) 1 else 0) + shift.coerceAtLeast(0)
    val head = pages.take(lone).map { listOf(it) }
    return head + pairRest(pages.drop(lone), widePages)
}

// A wide page takes a spread of its own and the pages around it keep pairing in order.
private fun pairRest(pages: List<Int>, widePages: Set<Int>): List<List<Int>> {
    val result = ArrayList<List<Int>>()
    var index = 0
    while (index < pages.size) {
        val page = pages[index]
        val paired = page !in widePages &&
            index + 1 < pages.size &&
            pages[index + 1] !in widePages
        if (paired) {
            result += listOf(page, pages[index + 1])
            index += 2
        } else {
            result += listOf(page)
            index += 1
        }
    }
    return result
}

fun spreadIndexOf(spreads: List<List<Int>>, page: Int): Int {
    val index = spreads.indexOfFirst { page in it }
    return if (index >= 0) index else 0
}

fun keptPageFromSettle(spreads: List<List<Int>>, settledIndex: Int): Int? =
    spreads.getOrNull(settledIndex)?.firstOrNull()

fun hasReadableSpreads(spreads: List<List<Int>>): Boolean = spreads.isNotEmpty()
