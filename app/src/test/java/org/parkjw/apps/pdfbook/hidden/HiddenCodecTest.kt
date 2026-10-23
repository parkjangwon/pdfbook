package org.parkjw.apps.pdfbook.hidden

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenCodecTest {
    private val folder = HiddenItem(HiddenKind.Folder, "content://tree/a", "책 폴더", null)
    private val book = HiddenItem(HiddenKind.Book, "doc:1", "tab\tnew\nline", "content://tree/a")

    @Test
    fun roundTripKeepsOrderUnicodeAndSeparators() {
        val items = listOf(folder, book)
        assertEquals(items, decodeHiddenItems(encodeHiddenItems(items)))
    }

    @Test
    fun emptyInputsDecodeToEmpty() {
        assertEquals("", encodeHiddenItems(emptyList()))
        assertTrue(decodeHiddenItems(null).isEmpty())
        assertTrue(decodeHiddenItems("").isEmpty())
    }

    @Test
    fun nullFolderUriEncodesAsEmptyAndDecodesBackToNull() {
        assertNull(decodeHiddenItems(encodeHiddenItems(listOf(folder))).single().folderUri)
    }

    @Test
    fun malformedRowsAreSkippedWithoutDroppingGoodOnes() {
        val good = encodeHiddenItems(listOf(book))
        val unknownKind = "41\t41\t41\t\n" // "A" is not a HiddenKind
        val oddHex = "426F6F6B\t4\t41\t\n"
        val badHex = "426F6F6B\tZZ\t41\t\n"
        val tooFewColumns = "426F6F6B\t41\t41\n"
        val raw = unknownKind + oddHex + badHex + tooFewColumns + "\n" + good
        assertEquals(listOf(book), decodeHiddenItems(raw))
    }
}
