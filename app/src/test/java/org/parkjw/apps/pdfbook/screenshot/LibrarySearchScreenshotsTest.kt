package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.FolderNode
import org.parkjw.apps.pdfbook.LibraryScreen
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.VisibilityRules
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.subfolderItems
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class LibrarySearchScreenshotsTest {
    @Before
    fun onlyWhenRecordingOrVerifying() {
        assumeTrue(
            System.getProperty("roborazzi.test.record") == "true" ||
                System.getProperty("roborazzi.test.verify") == "true" ||
                System.getProperty("roborazzi.test.compare") == "true",
        )
    }

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun searchResultsLightFolded() = capture(
        dark = false,
        file = "library-search-light-folded.png",
        query = "마음",
        root = koreanTree(),
    )

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun searchResultsDarkFolded() = capture(
        dark = true,
        file = "library-search-dark-folded.png",
        query = "마음",
        root = koreanTree(),
    )

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun searchEmptyLightFolded() = capture(
        dark = false,
        file = "library-search-empty-light-folded.png",
        query = "없는 책",
        root = koreanTree(),
    )

    @Test
    @Config(qualifiers = FOLDED_EN_QUALIFIERS)
    fun searchResultsLightFoldedEn() = capture(
        dark = false,
        file = "library-search-light-folded-en.png",
        query = "harbor",
        root = englishTree(),
    )

    private fun capture(dark: Boolean, file: String, query: String, root: FolderNode) {
        val subfolders = subfolderItems(root, emptyList(), VisibilityRules(), peek = false)
        val books = allBooks(root)
        captureRoboImage(file) {
            ScreenshotTheme(dark) {
                LibraryScreen(
                    listing = Listing(root.name, root.books, root),
                    states = books.associate {
                        bookKey(it.uri, it.size) to BookState(page = 4, pageCount = 12)
                    },
                    onOpen = {},
                    onSettings = {},
                    covers = treeCovers(root),
                    subfolders = subfolders,
                    showSearch = true,
                    searchQuery = query,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun shotBook(id: String, name: String) = BookInfo("content://shot/$id", "$name.pdf", id.length * 100L + 7)

private fun koreanTree(): FolderNode {
    val series = FolderNode(
        documentId = "series",
        uri = "content://shot/series",
        name = "마음 시리즈",
        books = listOf(shotBook("m1", "마음 1권"), shotBook("m2", "마음 2권")),
        folders = emptyList(),
    )
    return FolderNode(
        documentId = "root",
        uri = "content://shot/root",
        name = "내 서고",
        books = listOf(
            shotBook("a", "마음의 지도"),
            shotBook("b", "Harbor Lights"),
            shotBook("c", "느린 오후"),
        ),
        folders = listOf(series),
    )
}

private fun englishTree(): FolderNode {
    val series = FolderNode(
        documentId = "series",
        uri = "content://shot/series-en",
        name = "과월호",
        books = listOf(shotBook("e1", "Old Harbor")),
        folders = emptyList(),
    )
    return FolderNode(
        documentId = "root",
        uri = "content://shot/root-en",
        name = "Library",
        books = listOf(
            shotBook("h1", "Harbor Lights"),
            shotBook("h2", "North Line"),
            shotBook("h3", "Paper Moon"),
        ),
        folders = listOf(series),
    )
}

private fun allBooks(node: FolderNode): List<BookInfo> =
    node.books + node.folders.flatMap { allBooks(it) }

private fun treeCovers(root: FolderNode): Map<String, Bitmap?> {
    val palette = listOf(
        0xFFD97757.toInt() to 0xFF2F2A26.toInt(),
        0xFF6B8F71.toInt() to 0xFFF4F0E8.toInt(),
        0xFF4A6FA5.toInt() to 0xFFE8EEF5.toInt(),
        0xFFC9A55B.toInt() to 0xFF2F2A26.toInt(),
    )
    return allBooks(root).mapIndexed { index, book ->
        val (background, accent) = palette[index % palette.size]
        bookKey(book.uri, book.size) to solidCover(background, accent)
    }.toMap()
}
