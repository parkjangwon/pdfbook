package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {
    private fun sorted(vararg names: String) = names.toList().sortedWith(NaturalOrder)

    @Test
    fun episodesSortByNumberValue() {
        assertEquals(
            listOf("1화", "2화", "10화", "100화", "101화"),
            sorted("100화", "1화", "101화", "10화", "2화"),
        )
    }

    @Test
    fun leadingZerosCompareByValue() {
        assertEquals(listOf("001", "2", "10"), sorted("10", "2", "001"))
    }

    @Test
    fun equalValuesTieBreakOnLeadingZeros() {
        assertEquals(listOf("001", "01", "1"), sorted("1", "001", "01"))
        assertTrue(NaturalOrder.compare("1", "01") > 0)
        assertEquals(listOf("a01b2", "a1b02"), sorted("a1b02", "a01b2"))
    }

    @Test
    fun prefixedVolumes() {
        assertEquals(listOf("권1", "권2", "권10"), sorted("권1", "권10", "권2"))
    }

    @Test
    fun caseInsensitiveWithStableTieBreak() {
        assertEquals(listOf("apple", "Banana", "cherry"), sorted("cherry", "Banana", "apple"))
        assertEquals(listOf("Vol 2", "vol 3", "VOL 10"), sorted("VOL 10", "vol 3", "Vol 2"))
        assertEquals(listOf("ABC", "abc"), sorted("abc", "ABC"))
        assertEquals(listOf("ABC", "abc"), sorted("ABC", "abc"))
    }

    @Test
    fun koreanAndEnglishMixed() {
        assertEquals(
            listOf("Book 2", "book 10", "가나 1권", "가나 3권", "나무 2권"),
            sorted("나무 2권", "가나 3권", "book 10", "가나 1권", "Book 2"),
        )
    }

    @Test
    fun fileNamesWithExtension() {
        assertEquals(listOf("Vol 2.pdf", "Vol 10.pdf"), sorted("Vol 10.pdf", "Vol 2.pdf"))
        assertEquals(listOf("v1.pdf", "v1a.pdf", "v2.pdf"), sorted("v2.pdf", "v1a.pdf", "v1.pdf"))
    }

    @Test
    fun longNumbersDoNotOverflow() {
        val small = "9".repeat(30)
        val big = "1" + "0".repeat(30)
        assertEquals(listOf("x$small", "x$big"), sorted("x$big", "x$small"))
        assertEquals(listOf("x000$small", "x$small"), sorted("x$small", "x000$small"))
    }

    @Test
    fun digitsSortBeforeOtherCharactersAtSamePosition() {
        assertEquals(listOf("a1", "a!", "ab", "a~"), sorted("ab", "a~", "a!", "a1"))
        assertEquals(listOf("1", "!", "a"), sorted("a", "!", "1"))
    }

    @Test
    fun comparatorIsConsistent() {
        val names = listOf("1화", "01화", "a", "A", "a1", "a01", "a!", "!", "1", "가", "10", "", "Vol 2.pdf", "vol 02.pdf", "a!0", "ab", "01a", "1a", "01", "a~", "1aa", "001")
        for (x in names) {
            assertEquals(0, NaturalOrder.compare(x, x))
            for (y in names) {
                assertEquals("$x vs $y", Integer.signum(NaturalOrder.compare(x, y)), -Integer.signum(NaturalOrder.compare(y, x)))
                if (x != y) assertTrue("$x vs $y", NaturalOrder.compare(x, y) != 0)
                for (z in names) {
                    if (NaturalOrder.compare(x, y) < 0 && NaturalOrder.compare(y, z) < 0) {
                        assertTrue("$x < $y < $z", NaturalOrder.compare(x, z) < 0)
                    }
                }
            }
        }
    }

    @Test
    fun sortBooksUsesNaturalOrderForNameAndRecentTies() {
        val books = listOf("10화.pdf", "1화.pdf", "2화.pdf").map { BookInfo("u/$it", it, 1L) }
        assertEquals(listOf("1화.pdf", "2화.pdf", "10화.pdf"), sortBooks(books, emptyMap(), byName = true).map { it.name })
        assertEquals(listOf("1화.pdf", "2화.pdf", "10화.pdf"), sortBooks(books, emptyMap(), byName = false).map { it.name })
        val states = mapOf(bookKey("u/10화.pdf", 1L) to BookState(openedAt = 5L))
        assertEquals(listOf("10화.pdf", "1화.pdf", "2화.pdf"), sortBooks(books, states, byName = false).map { it.name })
    }

    @Test
    fun sortIsStableForDistinctBooksWithSameTitle() {
        val a = BookInfo("u/a/1화.pdf", "1화.pdf", 1L)
        val b = BookInfo("u/b/1화.pdf", "1화.pdf", 2L)
        assertEquals(listOf(a, b), sortBooks(listOf(a, b), emptyMap(), byName = true))
        assertEquals(listOf(b, a), sortBooks(listOf(b, a), emptyMap(), byName = true))
    }
}
