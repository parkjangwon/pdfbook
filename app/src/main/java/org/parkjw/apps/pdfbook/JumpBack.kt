package org.parkjw.apps.pdfbook

import kotlin.math.abs

const val JUMP_BACK_MILLIS = 5_000L
const val JUMP_BACK_MIN_PAGES = 3

data class JumpBack(val page: Int, val shownAt: Int)

fun jumpBackAfterSeek(spreads: List<List<Int>>, fromIndex: Int, toIndex: Int): JumpBack? {
    val from = spreads.getOrNull(fromIndex)?.firstOrNull() ?: return null
    val to = spreads.getOrNull(toIndex)?.firstOrNull() ?: return null
    if (abs(to - from) < JUMP_BACK_MIN_PAGES) return null
    return JumpBack(page = from, shownAt = toIndex)
}

data class JumpBackStep(val targetIndex: Int, val next: JumpBack?)

fun jumpBackStep(spreads: List<List<Int>>, jumpBack: JumpBack, currentIndex: Int): JumpBackStep {
    val target = spreadIndexOf(spreads, jumpBack.page)
    val here = spreads.getOrNull(currentIndex)?.firstOrNull()
    val next = if (here == null || target == currentIndex) null else JumpBack(page = here, shownAt = target)
    return JumpBackStep(target, next)
}

fun jumpBackHiddenBySettle(jumpBack: JumpBack?, settledIndex: Int): Boolean =
    jumpBack != null && jumpBack.shownAt != settledIndex

fun jumpBackVisible(jumpBack: JumpBack?, endNotice: EndNotice?, sheetOpen: Boolean): Boolean =
    jumpBack != null && endNotice == null && !sheetOpen
