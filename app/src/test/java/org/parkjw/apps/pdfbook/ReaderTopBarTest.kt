package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w940dp-h920dp")
class ReaderTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var direction = mutableStateOf(ReadingDirection.RTL)
    private var directionChanges = 0
    private var bookListOpened = 0

    @Before
    fun reset() {
        direction = mutableStateOf(ReadingDirection.RTL)
        directionChanges = 0
        bookListOpened = 0
    }

    private fun setBar(
        widthDp: Int,
        bookList: Boolean = true,
        showPageMode: Boolean = true,
        twoPage: Boolean = false,
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Dark) {
                Box(Modifier.fillMaxSize().background(ReaderBackground)) {
                    ReaderOverlayTop(
                        title = "바닷가 마을 이야기 3권",
                        twoPage = twoPage,
                        direction = direction.value,
                        onBack = {},
                        onDirectionChange = {
                            direction.value = it
                            directionChanges++
                        },
                        showPageMode = showPageMode,
                        modifier = Modifier.align(Alignment.TopStart).width(widthDp.dp),
                        windowWidthDp = widthDp,
                        onOpenBookList = if (bookList) ({ bookListOpened++ }) else null,
                        onOpenPageNav = {},
                        bookmarked = false,
                        onToggleBookmark = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun openMenu() {
        composeRule.onNodeWithTag("reader_more_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun compactKeepsBookmarkPagesAndMore() {
        setBar(320)
        composeRule.onNodeWithTag("reader_back").assertExists()
        composeRule.onNodeWithTag("reader_bookmark_toggle").assertExists()
        composeRule.onNodeWithTag("reader_page_nav_button").assertExists()
        composeRule.onNodeWithTag("reader_more_button").assertExists()
        composeRule.onNodeWithTag("reader_direction_toggle").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_book_list_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_mode", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun compactTitleKeepsAtLeast120dpAt320() {
        setBar(320)
        composeRule.onNodeWithTag("reader_title", useUnmergedTree = true).assertWidthIsAtLeast(120.dp)
    }

    @Test
    fun compactTitleKeepsAtLeast120dpAt360() {
        setBar(360)
        composeRule.onNodeWithTag("reader_title", useUnmergedTree = true).assertWidthIsAtLeast(120.dp)
    }

    @Test
    fun mediumKeepsDirectionInline() {
        setBar(412)
        composeRule.onNodeWithTag("reader_direction_toggle").assertExists()
        composeRule.onNodeWithTag("reader_more_button").assertExists()
        composeRule.onNodeWithTag("reader_book_list_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_mode", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag("reader_title", useUnmergedTree = true).assertWidthIsAtLeast(120.dp)
    }

    @Test
    fun expandedShowsEverythingAndNoMore() {
        setBar(600)
        composeRule.onNodeWithTag("reader_direction_toggle").assertExists()
        composeRule.onNodeWithTag("reader_book_list_button").assertExists()
        composeRule.onNodeWithTag("reader_page_mode", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("reader_more_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_title", useUnmergedTree = true).assertWidthIsAtLeast(120.dp)
    }

    @Test
    fun expandedWithoutSeriesHidesBookList() {
        setBar(600, bookList = false)
        composeRule.onNodeWithTag("reader_book_list_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_mode", useUnmergedTree = true).assertExists()
    }

    @Test
    fun compactMenuHoldsBookListDirectionAndStatus() {
        setBar(360)
        openMenu()
        composeRule.onNodeWithTag("reader_more_menu").assertExists()
        composeRule.onNodeWithTag("reader_more_book_list").assertExists()
        composeRule.onNodeWithTag("reader_more_direction").assertExists()
        composeRule.onNodeWithTag("reader_more_status").assertExists()
    }

    @Test
    fun mediumWithoutMenuItemsHidesMore() {
        setBar(412, bookList = false, showPageMode = false)
        composeRule.onNodeWithTag("reader_more_button").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_title", useUnmergedTree = true).assertWidthIsAtLeast(120.dp)
    }

    @Test
    fun mediumMenuHoldsBookListAndStatusButNoDirection() {
        setBar(412)
        openMenu()
        composeRule.onNodeWithTag("reader_more_book_list").assertExists()
        composeRule.onNodeWithTag("reader_more_direction").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_more_status").assertExists()
    }

    @Test
    fun directionRowTogglesAndClosesMenu() {
        setBar(360)
        openMenu()
        composeRule.onNodeWithTag("reader_more_direction").performClick()
        composeRule.waitForIdle()
        assertEquals(1, directionChanges)
        assertEquals(ReadingDirection.LTR, direction.value)
        composeRule.onNodeWithTag("reader_more_menu").assertDoesNotExist()
    }

    @Test
    fun bookListRowOpensListAndClosesMenu() {
        setBar(360)
        openMenu()
        composeRule.onNodeWithTag("reader_more_book_list").performClick()
        composeRule.waitForIdle()
        assertEquals(1, bookListOpened)
        composeRule.onNodeWithTag("reader_more_menu").assertDoesNotExist()
    }

    @Test
    fun statusRowIsNotClickable() {
        setBar(360)
        openMenu()
        composeRule.onNodeWithTag("reader_more_status").assertHasNoClickAction()
    }

    @Test
    fun tabletopHidesStatusRow() {
        setBar(360, showPageMode = false)
        openMenu()
        composeRule.onNodeWithTag("reader_more_status").assertDoesNotExist()
    }
}
