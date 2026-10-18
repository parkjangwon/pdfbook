package org.parkjw.apps.pdfbook

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w411dp-h891dp")
class SubfolderComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val root = FolderNode(
        documentId = "root",
        uri = "doc:root",
        name = "Root",
        books = listOf(BookInfo("doc:a", "Alpha.pdf", 1)),
        folders = listOf(
            FolderNode("s10", "doc:s10", "Series 10", listOf(BookInfo("doc:x", "X.pdf", 1)), emptyList()),
            FolderNode("s2", "doc:s2", "Series 2", listOf(BookInfo("doc:y", "Y.pdf", 1)), emptyList()),
        ),
    )

    @Test
    fun subfoldersComeFirstInNaturalOrderAndOpenOnTap() {
        val opened = mutableListOf<List<String>>()
        val items = subfolderItems(root, emptyList(), VisibilityRules(), peek = false)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("Root", root.books, root),
                    states = emptyMap(),
                    onOpen = {},
                    onSettings = {},
                    covers = mapOf(
                        bookKey("doc:a", 1) to null,
                        bookKey("doc:x", 1) to null,
                        bookKey("doc:y", 1) to null,
                    ),
                    subfolders = items,
                    onOpenSubfolder = { opened += it.path },
                )
            }
        }
        val s2 = composeRule.onNodeWithText("Series 2").fetchSemanticsNode().positionInRoot
        val s10 = composeRule.onNodeWithText("Series 10").fetchSemanticsNode().positionInRoot
        val book = composeRule.onNodeWithText("Alpha").fetchSemanticsNode().positionInRoot
        assertTrue(s2.y < book.y || (s2.y == book.y && s2.x < book.x))
        assertTrue(s2.y < s10.y || (s2.y == s10.y && s2.x < s10.x))
        assertTrue(s10.y < book.y || (s10.y == book.y && s10.x < book.x))
        composeRule.onNodeWithText("Series 10").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(listOf("s10")), opened)
    }

    @Test
    fun insideSubfolderShowsPathLine() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("Series 2", emptyList(), root),
                    states = emptyMap(),
                    onOpen = {},
                    onSettings = {},
                    onBack = {},
                    pathLine = "Root / Series 2",
                )
            }
        }
        composeRule.onNodeWithText("Series 2").assertExists()
        composeRule.onNodeWithText("Root / Series 2").assertExists()
    }
}
