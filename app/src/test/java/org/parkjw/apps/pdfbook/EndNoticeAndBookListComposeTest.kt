package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp")
class EndNoticeAndBookListComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val next = BookInfo("doc:2", "Vol 2.pdf", 10)

    @Test
    fun cardForNextBookShowsTitleAndOpenButton() {
        var opened = 0
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                EndNoticeCard(EndNotice.NextBook(next), onOpen = { opened++ })
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.reader_next_book, "Vol 2")).assertIsDisplayed()
        composeRule.onNodeWithTag("end_notice_open").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun cardForNoNextBookHasNoOpenButton() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) { EndNoticeCard(EndNotice.NoNextBook, onOpen = {}) }
        }
        composeRule.onNodeWithText(context.getString(R.string.reader_no_next_book)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("end_notice_open").assertCountEquals(0)
    }

    @Test
    fun hostShowsNothingWhenNoticeIsNull() {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                EndNoticeHost(
                    notice = null,
                    twoPage = false,
                    direction = ReadingDirection.LTR,
                    bottomInset = 0.dp,
                    onOpen = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onAllNodesWithTag("end_notice").assertCountEquals(0)
    }

    private fun setSheet(entries: List<BookListEntry>) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                BookListSheet(
                    folderName = "Series",
                    entries = entries,
                    onPick = {},
                    onDismiss = {},
                    covers = entries.associate { bookKey(it.book.uri, it.book.size) to null },
                )
            }
        }
    }

    @Test
    fun sheetShowsFolderNameCountAndPerRowProgress() {
        val a = BookInfo("doc:1", "Vol 1.pdf", 1)
        val b = BookInfo("doc:2", "Vol 2.pdf", 1)
        setSheet(listOf(BookListEntry(a, isCurrent = true, percent = 40), BookListEntry(b, isCurrent = false, percent = null)))
        composeRule.onNodeWithText("Series").assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.book_list_count, 2, 2), substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.book_list_progress, 40)).assertIsDisplayed()
        // a book with no saved state shows 0%
        composeRule.onNodeWithText(context.getString(R.string.book_list_progress, 0)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.book_list_reading)).assertIsDisplayed()
    }

    @Test
    fun sheetWithNoEntriesShowsHeaderAndNoRows() {
        setSheet(emptyList())
        composeRule.onNodeWithTag(BOOK_LIST_SHEET_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag("book_list_row").assertCountEquals(0)
    }
}
