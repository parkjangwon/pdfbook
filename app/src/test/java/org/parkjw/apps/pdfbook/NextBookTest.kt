package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextBookTest {
    private fun book(id: String, name: String) = BookInfo("doc:$id", name, 10)

    private val v1 = book("v1", "샘플 시리즈 1권.pdf")
    private val v2 = book("v2", "샘플 시리즈 2권.pdf")
    private val v3 = book("v3", "샘플 시리즈 3권.pdf")
    private val v10 = book("v10", "샘플 시리즈 10권.pdf")
    private val dot = book("dot", ".샘플 시리즈 4권.pdf")
    private val folder = listOf(v10, v3, dot, v1, v2)

    private fun next(
        current: BookInfo,
        sort: BookSort = BookSort.Name,
        hidden: Set<String> = emptySet(),
        states: Map<String, BookState> = emptyMap(),
        books: List<BookInfo> = folder,
    ) = nextBookIn(books, current, hidden, showDotFiles = false, sort = sort, states = states)

    private fun opened(vararg pairs: Pair<BookInfo, Long>) =
        pairs.associate { (b, at) -> bookKey(b.uri, b.size) to BookState(openedAt = at) }

    @Test
    fun nb1NameSortUsesNaturalOrder() {
        assertEquals(v2, next(v1))
        assertEquals(v3, next(v2))
        assertEquals(v10, next(v3))
    }

    @Test
    fun recentSortFollowsListOrder() {
        val states = opened(v3 to 300L, v1 to 200L, v10 to 100L)
        val expected = sortBooks(listOf(v10, v3, v1, v2), states, byName = false)
        assertEquals(listOf(v3, v1, v10, v2), expected)
        assertEquals(v1, next(v3, BookSort.Recent, states = states))
        assertEquals(v10, next(v1, BookSort.Recent, states = states))
        assertEquals(v2, next(v10, BookSort.Recent, states = states))
        assertNull(next(v2, BookSort.Recent, states = states))
    }

    @Test
    fun nb2HiddenBooksAndDotFilesAreSkipped() {
        assertEquals(v3, next(v1, hidden = setOf(v2.uri)))
        assertEquals(v10, next(v3))
        assertEquals(v10, next(v2, hidden = setOf(v2.uri, v3.uri)))
    }

    @Test
    fun nb3LastBookHasNoNext() {
        assertNull(next(v10))
        assertNull(next(v3, hidden = setOf(v10.uri)))
        assertNull(next(v1, books = listOf(v1)))
    }

    @Test
    fun nextBookStaysInsideTheBookFolder() {
        val other = book("o1", "샘플 시리즈 11권.pdf")
        val sub = FolderNode(documentId = "sub", uri = "doc:sub", name = "Sub", books = listOf(other), folders = emptyList())
        val root = FolderNode(documentId = "root", uri = "doc:root", name = "Root", books = listOf(v1, v2), folders = listOf(sub))
        val books = folderBooksOf(root, v2.uri, emptyList())
        assertEquals(listOf(v1, v2), books)
        assertNull(next(v2, books = books))
        assertEquals(listOf(other), folderBooksOf(root, other.uri, emptyList()))
        assertNull(next(other, books = folderBooksOf(root, other.uri, emptyList())))
    }

    @Test
    fun firstNextShowsNoticeAndSecondNextOpens() {
        val first = endStep(null, v2)
        assertEquals(EndStep.Show(EndNotice.NextBook(v2)), first)
        val second = endStep((first as EndStep.Show).notice, v2)
        assertEquals(EndStep.Open(v2), second)
    }

    @Test
    fun noticeGoneMeansNextShowsNoticeAgain() {
        assertEquals(EndStep.Show(EndNotice.NextBook(v2)), endStep(null, v2))
        assertEquals(EndStep.Show(EndNotice.NextBook(v3)), endStep(EndNotice.NextBook(v2), v3))
    }

    @Test
    fun nb3NoNextBookOnlyShowsNotice() {
        assertEquals(EndStep.Show(EndNotice.NoNextBook), endStep(null, null))
        assertEquals(EndStep.Show(EndNotice.NoNextBook), endStep(EndNotice.NoNextBook, null))
    }

    @Test
    fun noticeDurations() {
        assertEquals(5_000L, endNoticeMillis(EndNotice.NextBook(v2)))
        assertEquals(2_000L, endNoticeMillis(EndNotice.NoNextBook))
    }

    @Test
    fun turnPastEndOnlyOnLastSpreadForward() {
        assertTrue(isTurnPastEnd(currentIndex = 9, delta = 1, pageCount = 10))
        assertFalse(isTurnPastEnd(currentIndex = 8, delta = 1, pageCount = 10))
        assertFalse(isTurnPastEnd(currentIndex = 9, delta = -1, pageCount = 10))
        assertFalse(isTurnPastEnd(currentIndex = 0, delta = 1, pageCount = 0))
        val twoPage = spreads(11, twoPage = true, coverAlone = true)
        assertTrue(isTurnPastEnd(twoPage.lastIndex, 1, twoPage.size))
        assertFalse(isTurnPastEnd(twoPage.lastIndex - 1, 1, twoPage.size))
    }

    @Test
    fun nb4SwipeTowardNextFollowsReadingDirection() {
        assertTrue(isSwipeTowardNext(-100f, 5f, 48f, ReadingDirection.LTR))
        assertFalse(isSwipeTowardNext(100f, 5f, 48f, ReadingDirection.LTR))
        assertTrue(isSwipeTowardNext(100f, 5f, 48f, ReadingDirection.RTL))
        assertFalse(isSwipeTowardNext(-100f, 5f, 48f, ReadingDirection.RTL))
        assertFalse(isSwipeTowardNext(30f, 0f, 48f, ReadingDirection.RTL))
        assertFalse(isSwipeTowardNext(100f, 200f, 48f, ReadingDirection.RTL))
    }

    @Test
    fun nb4NextBookOpensAtSavedPage() {
        val saved = BookState(page = 10, pageCount = 120, direction = ReadingDirection.RTL, openedAt = 1)
        assertEquals(10, openStateFor(saved, ReadingDirection.LTR).page)
        assertEquals(1, openStateFor(null, ReadingDirection.RTL).page)
    }
}
