package org.parkjw.apps.pdfbook

fun spreads(pageCount: Int, twoPage: Boolean, coverAlone: Boolean, shift: Int = 0): List<List<Int>> {
    if (pageCount <= 0) return emptyList()
    val pages = (1..pageCount).toList()
    if (!twoPage) return pages.map { listOf(it) }
    val lone = (if (coverAlone) 1 else 0) + shift.coerceAtLeast(0)
    if (lone == 0) return pages.chunked(2)
    val head = pages.take(lone).map { listOf(it) }
    return head + pages.drop(lone).chunked(2)
}

fun spreadIndexOf(spreads: List<List<Int>>, page: Int): Int {
    val index = spreads.indexOfFirst { page in it }
    return if (index >= 0) index else 0
}

fun keptPageFromSettle(spreads: List<List<Int>>, settledIndex: Int): Int? =
    spreads.getOrNull(settledIndex)?.firstOrNull()

fun hasReadableSpreads(spreads: List<List<Int>>): Boolean = spreads.isNotEmpty()
