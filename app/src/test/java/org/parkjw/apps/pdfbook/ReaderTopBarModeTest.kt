package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTopBarModeTest {
    @Test
    fun compactBelow400dp() {
        assertEquals(ReaderTopBarMode.Compact, readerTopBarMode(320))
        assertEquals(ReaderTopBarMode.Compact, readerTopBarMode(360))
        assertEquals(ReaderTopBarMode.Compact, readerTopBarMode(399))
    }

    @Test
    fun mediumFrom400To599dp() {
        assertEquals(ReaderTopBarMode.Medium, readerTopBarMode(400))
        assertEquals(ReaderTopBarMode.Medium, readerTopBarMode(412))
        assertEquals(ReaderTopBarMode.Medium, readerTopBarMode(599))
    }

    @Test
    fun expandedFrom600dp() {
        assertEquals(ReaderTopBarMode.Expanded, readerTopBarMode(600))
        assertEquals(ReaderTopBarMode.Expanded, readerTopBarMode(656))
        assertEquals(ReaderTopBarMode.Expanded, readerTopBarMode(1000))
    }
}
