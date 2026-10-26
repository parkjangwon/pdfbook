package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class BookInfoTest {
    @Test
    fun fileSizeUsesReadableUnits() {
        assertNull(fileSizeOf(0))
        assertNull(fileSizeOf(-1))
        assertEquals(FileSizeText("1023", FileSizeUnit.B), fileSizeOf(1023))
        assertEquals(FileSizeText("1", FileSizeUnit.KB), fileSizeOf(1024))
        assertEquals(FileSizeText("1.5", FileSizeUnit.KB), fileSizeOf(1536))
        assertEquals(FileSizeText("12.4", FileSizeUnit.MB), fileSizeOf(13_000_000))
        assertEquals(FileSizeText("2", FileSizeUnit.GB), fileSizeOf(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun fileSizePromotesWhenRoundingWouldShow1024() {
        assertEquals(FileSizeText("1", FileSizeUnit.MB), fileSizeOf(1_048_525))
        assertEquals(FileSizeText("1", FileSizeUnit.MB), fileSizeOf(1_048_575))
        assertEquals(FileSizeText("1", FileSizeUnit.GB), fileSizeOf(1_073_741_773))
    }

    @Test
    fun progressIsClampedAndZeroWhenNotRead() {
        assertEquals(
            ReadingProgress(page = 12, pageCount = 120, percent = 10),
            readingProgressOf(BookState(page = 12, pageCount = 120, openedAt = 1L), 120),
        )
        assertEquals(
            ReadingProgress(page = 0, pageCount = 120, percent = 0),
            readingProgressOf(BookState(page = 12, pageCount = 120, openedAt = 0L), 120),
        )
        assertEquals(
            ReadingProgress(page = 0, pageCount = 50, percent = 0),
            readingProgressOf(null, 50),
        )
        assertEquals(
            ReadingProgress(page = 120, pageCount = 120, percent = 100),
            readingProgressOf(BookState(page = 500, pageCount = 120, openedAt = 1L), 120),
        )
        assertNull(readingProgressOf(BookState(openedAt = 1L), 0))
    }

    @Test
    fun lastReadIsLocalizedAndEmptyWhenNeverRead() {
        val openedAt = 1_700_000_000_000L
        val utc = TimeZone.getTimeZone("UTC")
        assertNull(lastReadText(0L, Locale.US, utc))
        assertNull(lastReadText(-1L, Locale.US, utc))
        val english = lastReadText(openedAt, Locale.US, utc)
        val korean = lastReadText(openedAt, Locale.KOREAN, utc)
        assertTrue(english!!.contains("2023"))
        assertTrue(korean!!.contains("2023"))
        assertNotEquals(english, korean)
    }
}
