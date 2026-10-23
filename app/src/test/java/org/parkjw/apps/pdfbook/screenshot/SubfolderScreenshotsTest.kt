package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import org.parkjw.apps.pdfbook.nodeAt
import org.parkjw.apps.pdfbook.subfolderItems
import org.parkjw.apps.pdfbook.visibleBooksUnder
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class SubfolderScreenshotsTest {
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
    fun subfoldersLightFolded() = captureRoot(dark = false, file = "library-subfolders-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun subfoldersDarkFolded() = captureRoot(dark = true, file = "library-subfolders-dark-folded.png")

    @Test
    @Config(qualifiers = UNFOLDED_QUALIFIERS)
    fun subfoldersLightUnfolded() = captureRoot(dark = false, file = "library-subfolders-light-unfolded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun subfoldersPeekLightFolded() = capturePeek(dark = false, file = "library-subfolders-peek-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun subfolderInsideLightFolded() = captureInside(dark = false, file = "library-subfolder-inside-light-folded.png")

    @Test
    @Config(qualifiers = FOLDED_NIGHT_QUALIFIERS)
    fun subfolderInsideDarkFolded() = captureInside(dark = true, file = "library-subfolder-inside-dark-folded.png")

    @Test
    @Config(qualifiers = FOLDED_QUALIFIERS)
    fun longRootTitleLightFolded() = captureRoot(
        dark = false,
        file = "library-long-title-light-folded.png",
        folderName = "가을에 읽은 오래된 잡지와 신문 모음",
    )

    private fun captureRoot(dark: Boolean, file: String, folderName: String = "내 서고") {
        val root = sampleTree()
        val rules = VisibilityRules()
        capture(file, dark) {
            LibraryScreen(
                listing = Listing(folderName, root.books, root),
                states = treeStates(root),
                onOpen = {},
                onSettings = {},
                covers = treeCovers(root),
                modifier = Modifier.fillMaxSize(),
                subfolders = subfolderItems(root, emptyList(), rules, peek = false),
                resumeBooks = visibleBooksUnder(root, rules),
            )
        }
    }

    private fun capturePeek(dark: Boolean, file: String) {
        val root = sampleTree()
        val rules = VisibilityRules(hiddenFolderIds = setOf("content://shot/essays"))
        capture(file, dark) {
            LibraryScreen(
                listing = Listing("내 서고", root.books, root),
                states = treeStates(root),
                onOpen = {},
                onSettings = {},
                covers = treeCovers(root),
                modifier = Modifier.fillMaxSize(),
                peekEnabled = true,
                peekMenuEnabled = true,
                subfolders = subfolderItems(root, emptyList(), rules, peek = true),
                resumeBooks = visibleBooksUnder(root, rules),
            )
        }
    }

    private fun captureInside(dark: Boolean, file: String) {
        val root = sampleTree()
        val rules = VisibilityRules()
        val path = listOf("series", "part3")
        val node = root.nodeAt(path)!!
        capture(file, dark) {
            LibraryScreen(
                listing = Listing(node.name, node.books, root),
                states = treeStates(root),
                onOpen = {},
                onSettings = {},
                onBack = {},
                covers = treeCovers(root),
                modifier = Modifier.fillMaxSize(),
                subfolders = subfolderItems(node, path, rules, peek = false),
                pathLine = "내 서고 / 긴 이름의 시리즈 모음 / 3부",
                resumeBooks = visibleBooksUnder(node, rules),
            )
        }
    }

    private fun capture(file: String, dark: Boolean, content: @Composable () -> Unit) {
        captureRoboImage(file) {
            ScreenshotTheme(dark, null, content)
        }
    }
}

private fun book(id: String, name: String) = BookInfo("content://shot/$id", "$name.pdf", id.length * 100L + 7)

private fun sampleTree(): FolderNode {
    val part3 = FolderNode(
        documentId = "part3",
        uri = "content://shot/part3",
        name = "3부",
        books = listOf(book("p3b2", "3부 2권"), book("p3b1", "3부 1권"), book("p3b10", "3부 10권")),
        folders = listOf(
            FolderNode("extra", "content://shot/extra", "외전", emptyList(), emptyList()),
        ),
    )
    val series = FolderNode(
        documentId = "series",
        uri = "content://shot/series",
        name = "긴 이름의 시리즈 모음",
        books = listOf(book("s1", "1부 1권"), book("s2", "1부 2권")),
        folders = listOf(part3),
    )
    val essays = FolderNode(
        documentId = "essays",
        uri = "content://shot/essays",
        name = "에세이",
        books = listOf(book("e1", "여름 일기")),
        folders = emptyList(),
    )
    val noCover = FolderNode(
        documentId = "nocover",
        uri = "content://shot/nocover",
        name = "새로 받은 책",
        books = listOf(book("n1", "표지 없음")),
        folders = emptyList(),
    )
    return FolderNode(
        documentId = "root",
        uri = "content://shot/root",
        name = "내 서고",
        books = listOf(book("r1", "단편집"), book("r2", "화보")),
        folders = listOf(series, essays, noCover),
    )
}

private fun allBooks(node: FolderNode): List<BookInfo> = node.books + node.folders.flatMap { allBooks(it) }

private fun treeStates(root: FolderNode): Map<String, BookState> {
    val books = allBooks(root)
    return books.mapIndexed { index, b ->
        bookKey(b.uri, b.size) to BookState(page = index + 1, pageCount = 12, openedAt = 1_000L + index)
    }.toMap()
}

private val palette = listOf(
    0xFFD97757.toInt() to 0xFF2F2A26.toInt(),
    0xFF6B8F71.toInt() to 0xFFF4F0E8.toInt(),
    0xFF4A6FA5.toInt() to 0xFFE8EEF5.toInt(),
    0xFFC9A55B.toInt() to 0xFF2F2A26.toInt(),
    0xFF8E5E6E.toInt() to 0xFFF4F0E8.toInt(),
)

private fun treeCovers(root: FolderNode): Map<String, Bitmap?> =
    allBooks(root).mapIndexed { index, b ->
        val (bg, fg) = palette[index % palette.size]
        bookKey(b.uri, b.size) to if (b.uri.endsWith("n1")) null else solidCover(bg, fg)
    }.toMap()
