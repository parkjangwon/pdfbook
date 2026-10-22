package org.parkjw.apps.pdfbook

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class BookInfoComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val book = BookInfo("doc:night-garden", "밤의 정원.pdf", 13_000_000)

    private fun setLibrary(state: BookState?) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("서재", listOf(book)),
                    states = state?.let { mapOf(bookKey(book.uri, book.size) to it) } ?: emptyMap(),
                    onOpen = {},
                    onSettings = {},
                    covers = mapOf(bookKey(book.uri, book.size) to null),
                    resumeBooks = emptyList(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun openInfo() {
        composeRule.onNodeWithTag("library_book").performTouchInput { longClick() }
        composeRule.onNodeWithText("책 정보").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BOOK_INFO_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun menuItemOpensSheetWithBookValues() {
        setLibrary(BookState(page = 12, pageCount = 120, openedAt = 1_700_000_000_000L))
        openInfo()
        composeRule.onNodeWithText("파일 이름").assertIsDisplayed()
        composeRule.onNodeWithText("밤의 정원.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("120쪽").assertIsDisplayed()
        composeRule.onNodeWithText("12.4 MB").assertIsDisplayed()
        composeRule.onNodeWithText("12 / 120쪽 · 10%").assertIsDisplayed()
        composeRule.onNodeWithText("아직 읽지 않았어요").assertDoesNotExist()
    }

    @Test
    fun notReadBookShowsNotReadStateAndZeroProgress() {
        setLibrary(BookState(page = 1, pageCount = 120, openedAt = 0L))
        openInfo()
        composeRule.onNodeWithText("아직 읽지 않았어요").assertIsDisplayed()
        composeRule.onNodeWithText("0 / 120쪽 · 0%").assertIsDisplayed()
    }

    @Test
    fun pageCountShowsLoadingThenLoadedValue() {
        val pages = CompletableDeferred<Int?>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                BookInfoSheet(
                    book = BookInfo("doc:loading", "읽는 중.pdf", 100),
                    state = BookState(page = 0, pageCount = 0, openedAt = 0L),
                    onDismiss = {},
                    modifier = Modifier.fillMaxSize(),
                    pageCountLoader = { pages.await() },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("쪽 수를 확인하는 중…").assertIsDisplayed()
        pages.complete(7)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("7쪽").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("7쪽").assertIsDisplayed()
        composeRule.onNodeWithText("0 / 7쪽 · 0%").assertIsDisplayed()
        composeRule.onNodeWithText("쪽 수를 확인하는 중…").assertDoesNotExist()
    }

    @Test
    fun unreadableFileShowsUnreadablePageCount() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                BookInfoSheet(
                    book = BookInfo("doc:missing", "없는 책.pdf", 100),
                    state = null,
                    onDismiss = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("쪽 수를 읽을 수 없어요").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("쪽 수를 읽을 수 없어요").assertIsDisplayed()
    }
}
