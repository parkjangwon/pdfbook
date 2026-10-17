package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryTest {
    private fun book(name: String, size: Long = 1) = BookInfo("content://x/$name", name, size)

    @Test
    fun keyUsesUriAndSize() {
        assertEquals("u|5", bookKey("u", 5))
        assertNotEquals(bookKey("u", 5), bookKey("u", 6))
    }

    @Test
    fun titleStripsPdfExtension() {
        assertEquals("One Piece 01", titleOf("One Piece 01.pdf"))
        assertEquals("a", titleOf("a.PDF"))
        assertEquals("a.pdf.txt", titleOf("a.pdf.txt"))
    }

    @Test
    fun progressIsClampedFraction() {
        assertEquals(0f, progressOf(3, 0), 0f)
        assertEquals(0.5f, progressOf(3, 6), 0f)
        assertEquals(1f, progressOf(9, 6), 0f)
    }

    @Test
    fun columnsFollowWidth() {
        assertEquals(2, columnsFor(360))
        assertEquals(2, columnsFor(599))
        assertEquals(4, columnsFor(600))
        assertEquals(4, columnsFor(720))
        assertEquals(5, columnsFor(850))
        assertEquals(5, columnsFor(2000))
    }

    @Test
    fun coverFileNameIsStableHex() {
        assertEquals(coverFileName("a|1"), coverFileName("a|1"))
        assertNotEquals(coverFileName("a|1"), coverFileName("a|2"))
        assertEquals(40, coverFileName("a|1").length)
    }

    @Test
    fun stateRoundTrips() {
        val s = BookState(7, 200, ReadingDirection.LTR, 123L)
        assertEquals(s, decodeState(encodeState(s)))
        assertEquals(BookState(), decodeState(encodeState(BookState())))
    }

    @Test
    fun badStateDecodesToNull() {
        assertNull(decodeState(""))
        assertNull(decodeState("1,2,RTL"))
        assertNull(decodeState("x,2,RTL,0"))
        assertNull(decodeState("1,2,UP,0"))
    }

    @Test
    fun sortByNameIgnoresCase() {
        val books = listOf(book("b.pdf"), book("A.pdf"), book("c.pdf"))
        assertEquals(listOf("A.pdf", "b.pdf", "c.pdf"), sortBooks(books, emptyMap(), true).map { it.name })
    }

    @Test
    fun sortByRecentPutsOpenedFirstThenName() {
        val a = book("a.pdf")
        val b = book("b.pdf")
        val c = book("c.pdf")
        val states = mapOf(
            bookKey(c.uri, c.size) to BookState(openedAt = 10),
            bookKey(b.uri, b.size) to BookState(openedAt = 20),
        )
        assertEquals(listOf(b, c, a), sortBooks(listOf(a, b, c), states, false))
    }
}
