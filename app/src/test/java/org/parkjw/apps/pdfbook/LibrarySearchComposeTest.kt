package org.parkjw.apps.pdfbook

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
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
@Config(sdk = [37], qualifiers = "ko-w411dp-h891dp")
class LibrarySearchComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val root = FolderNode(
        documentId = "root",
        uri = "doc:root",
        name = "서재",
        books = listOf(
            BookInfo("doc:a", "마음의 지도.pdf", 1),
            BookInfo("doc:b", "Harbor Lights.pdf", 2),
        ),
        folders = listOf(
            FolderNode(
                "s1",
                "doc:s1",
                "마음 시리즈",
                listOf(BookInfo("doc:c", "시리즈 2.pdf", 3)),
                emptyList(),
            ),
        ),
    )

    @Test
    fun opensFocusedSearchFieldAndHidesTheButton() {
        setLibrary()
        composeRule.onNodeWithTag(SEARCH_BUTTON_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assertIsFocused()
        composeRule.onAllNodesWithTag(SEARCH_BUTTON_TAG).assertCountEquals(0)
    }

    @Test
    fun filtersBooksAndSubfoldersAsTyped() {
        setLibrary()
        openSearch()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("harbor")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Harbor Lights").assertExists()
        composeRule.onNodeWithText("마음의 지도").assertDoesNotExist()
        composeRule.onNodeWithText("마음 시리즈").assertDoesNotExist()
    }

    @Test
    fun showsEmptyStateWhenNothingMatches() {
        setLibrary()
        openSearch()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("없는 책")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SEARCH_EMPTY_TAG).assertExists()
        composeRule.onNodeWithText("찾는 책이 없어요").assertExists()
        composeRule.onNodeWithText("Harbor Lights").assertDoesNotExist()
    }

    @Test
    fun closeRestoresTheNormalBarAndClearsTheFilter() {
        setLibrary()
        openSearch()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("harbor")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SEARCH_CLOSE_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(SEARCH_FIELD_TAG).assertCountEquals(0)
        composeRule.onNodeWithTag(SEARCH_BUTTON_TAG).assertExists()
        composeRule.onNodeWithText("마음의 지도").assertExists()
        composeRule.onNodeWithText("마음 시리즈").assertExists()

        composeRule.onNodeWithTag(SEARCH_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("책 이름").assertExists()
    }

    @Test
    fun clearButtonKeepsSearchOpenAndShowsEverythingAgain() {
        setLibrary()
        openSearch()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("harbor")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SEARCH_CLEAR_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assertExists()
        composeRule.onNodeWithText("마음의 지도").assertExists()
        composeRule.onNodeWithText("Harbor Lights").assertExists()
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp")
    fun keepsSubfolderTitleReadableAtFoldedWidth() {
        setLibrary(folderName = "3부", onBack = {})
        composeRule.waitForIdle()

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag(LIBRARY_TITLE_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)

        val layout = layouts.single()
        assertTrue("title was ellipsized to nothing", layout.getLineEnd(0) > 0)
        assertEquals(layout.layoutInput.text.length, layout.getLineEnd(0))
        composeRule.onNodeWithTag(SEARCH_BUTTON_TAG).assertExists()
    }

    @Test
    fun hidesContinueReadingWhileAQueryIsActive() {
        setLibrary(resume = true)
        composeRule.onNodeWithTag("library_resume_card").assertExists()

        openSearch()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("harbor")
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("library_resume_card").assertCountEquals(0)
    }

    private fun openSearch() {
        composeRule.onNodeWithTag(SEARCH_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
    }

    private fun setLibrary(
        resume: Boolean = false,
        folderName: String = "서재",
        onBack: (() -> Unit)? = null,
    ) {
        val covers = (root.books + root.folders.flatMap { it.books }).associate {
            bookKey(it.uri, it.size) to null
        }
        val states = if (resume) {
            mapOf(bookKey("doc:a", 1) to BookState(page = 3, pageCount = 10, openedAt = 100L))
        } else {
            emptyMap()
        }
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing(folderName, root.books, root),
                    states = states,
                    onOpen = {},
                    onSettings = {},
                    onBack = onBack,
                    covers = covers,
                    subfolders = subfolderItems(root, emptyList(), VisibilityRules(), peek = false),
                    resumeBooks = if (resume) root.books else null,
                )
            }
        }
    }
}
