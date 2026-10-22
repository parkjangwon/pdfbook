package org.parkjw.apps.pdfbook

import org.junit.Assert.assertEquals
import org.junit.Test
import org.parkjw.apps.pdfbook.hidden.libraryBooks

class LibrarySearchTest {
    private fun book(name: String) = BookInfo("content://x/$name", name, 1)

    private fun folder(name: String) = SubfolderItem(
        node = FolderNode("doc:$name", "doc:$name", name, emptyList(), emptyList()),
        path = listOf("doc:$name"),
        bookCount = 0,
        coverBooks = emptyList(),
        isHidden = false,
    )

    @Test
    fun matchesTitlesAndSubfoldersCaseInsensitively() {
        val result = filterLibrary(
            books = listOf(book("Harbor Lights.pdf"), book("마음의 지도.pdf")),
            subfolders = listOf(folder("HARBOR Series"), folder("에세이")),
            query = "harbor",
        )
        assertEquals(listOf("Harbor Lights.pdf"), result.books.map { it.name })
        assertEquals(listOf("HARBOR Series"), result.subfolders.map { it.node.name })
    }

    @Test
    fun composesKoreanJamoBeforeMatching() {
        val composed = "\uD55C\uAC15" // 한강
        val decomposed = "\u1112\u1161\u11AB\u1100\u1161\u11BC"

        val byDecomposedQuery = filterLibrary(listOf(book("$composed.pdf")), emptyList(), decomposed)
        val byComposedQuery = filterLibrary(listOf(book("$decomposed.pdf")), emptyList(), composed)

        assertEquals(listOf("$composed.pdf"), byDecomposedQuery.books.map { it.name })
        assertEquals(listOf("$decomposed.pdf"), byComposedQuery.books.map { it.name })
    }

    @Test
    fun blankQueryKeepsEverything() {
        val books = listOf(book("a.pdf"), book("b.pdf"))
        val folders = listOf(folder("Folder"))
        val result = filterLibrary(books, folders, "   ")
        assertEquals(books, result.books)
        assertEquals(folders, result.subfolders)
    }

    @Test
    fun hiddenItemsStayHiddenUnlessPeeking() {
        val books = listOf(book("Hidden.pdf"), book("Shown.pdf"))
        val hidden = setOf("content://x/Hidden.pdf")

        val visible = libraryBooks(books, hidden, peek = false, showDotFiles = false)
        val peeked = libraryBooks(books, hidden, peek = true, showDotFiles = false)

        assertEquals(
            emptyList<BookInfo>(),
            filterLibrary(visible.map { it.book }, emptyList(), "hidden").books,
        )
        assertEquals(
            listOf("Hidden.pdf"),
            filterLibrary(peeked.map { it.book }, emptyList(), "hidden").books.map { it.name },
        )
    }

    @Test
    fun keepsTheChosenSortOrder() {
        val books = listOf(book("시리즈 10.pdf"), book("시리즈 2.pdf"), book("다른 책.pdf"))
        val sorted = sortBooks(books, emptyMap(), byName = true)

        val result = filterLibrary(sorted, emptyList(), "시리즈")

        assertEquals(listOf("시리즈 2.pdf", "시리즈 10.pdf"), result.books.map { it.name })
    }
}
