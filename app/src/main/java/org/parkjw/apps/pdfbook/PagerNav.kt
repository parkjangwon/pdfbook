package org.parkjw.apps.pdfbook

fun pagerTargetIndex(targetPage: Int, delta: Int, pageCount: Int): Int {
    if (pageCount <= 0) return 0
    return (targetPage + delta).coerceIn(0, pageCount - 1)
}
