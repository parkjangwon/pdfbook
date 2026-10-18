package org.parkjw.apps.pdfbook

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.folders.AddKind
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.folders.add
import org.parkjw.apps.pdfbook.folders.markOpened

class FolderShelfTest {
    private fun folder(
        uri: String,
        name: String = uri,
        order: Int = 0,
        access: FolderAccess = FolderAccess.Ok,
        lastOpenedAt: Long = 0L,
    ) = Folder(uri, name, order, access, lastOpenedAt)

    @Test
    fun shelfCardsFollowSavedOrderNotRecentOpen() {
        val folders = listOf(
            folder("a", order = 2, lastOpenedAt = 50L),
            folder("b", order = 0, lastOpenedAt = 1L),
            folder("c", order = 1, lastOpenedAt = 99L),
        )
        val ui = buildFolderShelfUi(folders, emptyMap(), emptyMap())
        assertEquals(listOf("b", "c", "a"), ui.cards.map { it.uri })
    }

    @Test
    fun duplicateAddAndOpeningDoNotMoveCard() {
        val base = listOf(
            folder("a", order = 0, lastOpenedAt = 1L),
            folder("b", order = 1, lastOpenedAt = 9L),
        )
        val duplicate = add(base, "b", "Other")
        assertEquals(AddKind.Duplicate, duplicate.kind)
        assertEquals(base, duplicate.folders)
        val opened = markOpened(duplicate.folders, "b", 99L)
        assertEquals(listOf("a", "b"), buildFolderShelfUi(opened, emptyMap(), emptyMap()).cards.map { it.uri })
    }

    @Test
    fun newFolderGoesToBottom() {
        val base = listOf(folder("a", order = 0), folder("b", order = 1))
        val added = add(base, "c", "C")
        assertEquals(AddKind.Created, added.kind)
        assertEquals(listOf("a", "b", "c"), buildFolderShelfUi(added.folders, emptyMap(), emptyMap()).cards.map { it.uri })
    }

    @Test
    fun orderedCardsUsesPendingOrderOnlyForSameSet() {
        val cards = buildFolderShelfUi(
            listOf(folder("a", order = 0), folder("b", order = 1), folder("c", order = 2)),
            emptyMap(),
            emptyMap(),
        ).cards
        assertEquals(listOf("c", "a", "b"), orderedCards(cards, listOf("c", "a", "b")).map { it.uri })
        assertEquals(listOf("a", "b", "c"), orderedCards(cards, null).map { it.uri })
        assertEquals(listOf("a", "b", "c"), orderedCards(cards, listOf("a", "b")).map { it.uri })
        assertEquals(listOf("a", "b", "c"), orderedCards(cards, listOf("a", "b", "x")).map { it.uri })
    }

    @Test
    fun moveBeforeTakesTargetSlotBothWays() {
        val order = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), moveBefore(order, "a", "c"))
        assertEquals(listOf("a", "d", "b", "c"), moveBefore(order, "d", "b"))
        assertEquals(order, moveBefore(order, "a", "a"))
        assertEquals(order, moveBefore(order, "x", "a"))
    }

    @Test
    fun autoScrollOnlyNearEdges() {
        assertEquals(0f, reorderAutoScrollStep(200f, 300f, 0f, 1000f, 100f, 10f), 0f)
        assertEquals(-5f, reorderAutoScrollStep(50f, 150f, 0f, 1000f, 100f, 10f), 0.001f)
        assertEquals(-10f, reorderAutoScrollStep(-40f, 60f, 0f, 1000f, 100f, 10f), 0.001f)
        assertEquals(5f, reorderAutoScrollStep(850f, 950f, 0f, 1000f, 100f, 10f), 0.001f)
        assertEquals(10f, reorderAutoScrollStep(950f, 1100f, 0f, 1000f, 100f, 10f), 0.001f)
        assertEquals(0f, reorderAutoScrollStep(950f, 1100f, 0f, 1000f, 0f, 10f), 0f)
    }

    @Test
    fun coverKeysFollowNaturalNameOrder() {
        val books = listOf("10화.pdf", "1화.pdf", "100화.pdf", "2화.pdf", "3화.pdf")
            .map { BookInfo("content://$it", it, 1L) }
        val ui = buildFolderShelfUi(listOf(folder("f")), mapOf("f" to Listing("F", books)), emptyMap())
        assertEquals(
            listOf("1화.pdf", "2화.pdf", "3화.pdf", "10화.pdf").map { bookKey("content://$it", 1L) },
            ui.cards.single().coverKeys,
        )
    }

    @Test
    fun buildFolderShelfUiSummarizesCardsAndPicksGlobalResume() {
        val folders = listOf(
            folder("fa", "Alpha", 0, lastOpenedAt = 2L),
            folder("fb", "Beta", 1, FolderAccess.Lost, lastOpenedAt = 5L),
            folder("fc", "Gamma", 2, lastOpenedAt = 1L),
        )
        val a1 = BookInfo("content://a1", "A1.pdf", 10)
        val a2 = BookInfo("content://a2", "A2.pdf", 20)
        val c1 = BookInfo("content://c1", "C1.pdf", 30)
        val listings = mapOf(
            "fa" to Listing("Alpha", listOf(a1, a2)),
            "fb" to null,
            "fc" to Listing("Gamma", listOf(c1)),
        )
        val states = mapOf(
            bookKey(a1.uri, a1.size) to BookState(page = 2, pageCount = 10, openedAt = 100L),
            bookKey(a2.uri, a2.size) to BookState(page = 1, pageCount = 5, openedAt = 50L),
            bookKey(c1.uri, c1.size) to BookState(page = 4, pageCount = 8, openedAt = 200L),
        )
        val ui = buildFolderShelfUi(folders, listings, states)
        assertEquals(listOf("fa", "fb", "fc"), ui.cards.map { it.uri })
        assertEquals(2, ui.cards[0].bookCount)
        assertEquals(listOf(bookKey(a1.uri, a1.size), bookKey(a2.uri, a2.size)), ui.cards[0].coverKeys)
        assertEquals("A1", ui.cards[0].resume?.title)
        assertEquals(20, ui.cards[0].resume?.percent)
        assertEquals(FolderAccess.Lost, ui.cards[1].access)
        assertEquals(0, ui.cards[1].bookCount)
        assertEquals(c1.uri, ui.globalResume?.book?.uri)
        assertEquals("fc", ui.globalResume?.folderUri)
        assertEquals(50, ui.globalResume?.percent)
    }

    @Test
    fun buildFolderShelfUiSkipsLostFoldersForGlobalResume() {
        val folders = listOf(folder("fa", access = FolderAccess.Lost))
        val book = BookInfo("content://a1", "A1.pdf", 10)
        val ui = buildFolderShelfUi(
            folders,
            mapOf("fa" to Listing("A", listOf(book))),
            mapOf(bookKey(book.uri, book.size) to BookState(page = 1, pageCount = 2, openedAt = 9L)),
        )
        assertNull(ui.globalResume)
    }

    @Test
    fun folderShelfColumnsFollowWidth() {
        assertEquals(1, folderShelfColumns(360))
        assertEquals(1, folderShelfColumns(599))
        assertEquals(2, folderShelfColumns(600))
        assertEquals(2, folderShelfColumns(656))
    }

    @Test
    fun folderShelfScrollIndexAccountsForResume() {
        assertEquals(0, folderShelfScrollIndex(0, hasGlobalResume = false))
        assertEquals(1, folderShelfScrollIndex(0, hasGlobalResume = true))
        assertEquals(3, folderShelfScrollIndex(2, hasGlobalResume = true))
    }

    @Test
    fun folderPathBookLineJoinsPathAndCount() {
        assertEquals("weekly · 책 4권", folderPathBookLine("weekly", "책 4권"))
        assertEquals("weekly · 4 books", folderPathBookLine("weekly", "4 books"))
        assertEquals("책 4권", folderPathBookLine("", "책 4권"))
    }

    @Test
    fun folderPathBookAnnotatedSpansOnlyCountDigits() {
        val style = SpanStyle(fontFamily = FontFamily.Monospace)
        val annotated = folderPathBookAnnotated("2026_04", "책 3권", style)
        assertEquals("2026_04 · 책 3권", annotated.text)
        assertEquals(1, annotated.spanStyles.size)
        assertEquals(12, annotated.spanStyles[0].start)
        assertEquals(13, annotated.spanStyles[0].end)
    }

    @Test
    fun folderShelfIsLastRowUsesColumns() {
        assertTrue(folderShelfIsLastRow(2, 3, 2))
        assertTrue(folderShelfIsLastRow(1, 2, 2))
        assertFalse(folderShelfIsLastRow(0, 3, 2))
        assertTrue(folderShelfIsLastRow(2, 3, 1))
        assertFalse(folderShelfIsLastRow(1, 3, 1))
    }
}
