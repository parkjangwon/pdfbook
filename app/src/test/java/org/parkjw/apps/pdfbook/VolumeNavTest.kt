package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeNavTest {
    private fun book(id: String, name: String = "$id.pdf") = BookInfo("doc:$id", name, 10)

    private val v1 = book("v1", "시리즈 1권.pdf")
    private val v2 = book("v2", "시리즈 2권.pdf")
    private val v10 = book("v10", "시리즈 10권.pdf")
    private val dot = book("dot", ".시리즈 3권.pdf")
    private val shuffled = listOf(v10, dot, v1, v2)

    @Test
    fun nb1VolumesFollowNaturalOrderNotRecentOrder() {
        assertEquals(listOf(v1, v2, v10), volumeList(shuffled, emptySet(), showDotFiles = false))
        val adjacent = adjacentVolumes(volumeList(shuffled, emptySet(), false), v2)
        assertEquals(v1, adjacent.previous)
        assertEquals(v10, adjacent.next)
    }

    @Test
    fun nb2HiddenAndDotFilesAreSkipped() {
        val volumes = volumeList(shuffled, setOf(v2.uri), showDotFiles = false)
        assertEquals(listOf(v1, v10), volumes)
        assertEquals(v10, adjacentVolumes(volumes, v1).next)
        val withDots = volumeList(shuffled, emptySet(), showDotFiles = true)
        assertTrue(dot in withDots)
    }

    @Test
    fun nb3EdgesHaveNoNeighbourAndSingleBookHasNone() {
        val volumes = volumeList(shuffled, emptySet(), false)
        assertNull(adjacentVolumes(volumes, v1).previous)
        assertNull(adjacentVolumes(volumes, v10).next)
        val single = adjacentVolumes(listOf(v1), v1)
        assertNull(single.previous)
        assertNull(single.next)
    }

    @Test
    fun nb2HiddenCurrentBookStillFindsNeighbours() {
        val volumes = volumeList(shuffled, setOf(v2.uri), false)
        val adjacent = adjacentVolumes(volumes, v2)
        assertEquals(v1, adjacent.previous)
        assertEquals(v10, adjacent.next)
    }

    @Test
    fun bl1BookListIsNaturalOrderWithCurrentMarkedAndProgress() {
        val volumes = volumeList(shuffled, setOf(v2.uri), false)
        val states = mapOf(bookKey(v10.uri, v10.size) to BookState(page = 5, pageCount = 10))
        val entries = bookListEntries(volumes, v10, states)
        assertEquals(listOf(v1, v10), entries.map { it.book })
        assertEquals(listOf(false, true), entries.map { it.isCurrent })
        assertEquals(1, currentEntryIndex(entries))
        assertNull(entries[0].percent)
        assertEquals(50, entries[1].percent)
    }

    @Test
    fun bl1BookListExcludesHiddenButKeepsHiddenCurrentBook() {
        val volumes = volumeList(shuffled, setOf(v2.uri), false)
        assertEquals(listOf(v1, v10), bookListEntries(volumes, v1, emptyMap()).map { it.book })
        val peeked = bookListEntries(volumes, v2, emptyMap())
        assertEquals(listOf(v1, v2, v10), peeked.map { it.book })
        assertEquals(1, currentEntryIndex(peeked))
    }

    @Test
    fun nb4Bl3SwitchingOpensAtSavedPageAndDirection() {
        val saved = BookState(page = 37, pageCount = 120, direction = ReadingDirection.LTR, openedAt = 5)
        assertEquals(saved, openStateFor(saved, ReadingDirection.RTL))
        assertEquals(BookState(direction = ReadingDirection.RTL), openStateFor(null, ReadingDirection.RTL))
    }
}
