package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
@Config(sdk = [37], qualifiers = "ko")
class BookProgressMenuComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val book = BookInfo("content://a", "A.pdf", 1)
    private val key = bookKey(book.uri, book.size)

    @Test
    fun mf7MenuReportsBothActions() {
        val finished = mutableListOf<BookInfo>()
        val started = mutableListOf<BookInfo>()
        setLibrary(BookState(page = 3, pageCount = 10, openedAt = 5), finished, started)
        openBookMenu()
        composeRule.onNodeWithText(context.getString(R.string.mark_finished)).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(book), finished)
        openBookMenu()
        composeRule.onNodeWithText(context.getString(R.string.start_over)).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(book), started)
    }

    @Test
    fun mf8FinishedBookOnlyOffersStartOver() {
        setLibrary(BookState(page = 10, pageCount = 10, openedAt = 5), mutableListOf(), mutableListOf())
        openBookMenu()
        composeRule.onAllNodesWithText(context.getString(R.string.mark_finished)).assertCountEquals(0)
        composeRule.onNodeWithText(context.getString(R.string.start_over)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.hide_item)).assertExists()
    }

    @Test
    fun mf9UnreadBookOnlyOffersMarkFinished() {
        setLibrary(BookState(page = 1, pageCount = 10), mutableListOf(), mutableListOf())
        openBookMenu()
        composeRule.onNodeWithText(context.getString(R.string.mark_finished)).assertExists()
        composeRule.onAllNodesWithText(context.getString(R.string.start_over)).assertCountEquals(0)
    }

    @Test
    fun mf10UndoRestoresMarkedFinishedBook() {
        val before = BookState(page = 3, pageCount = 10, openedAt = 5)
        val state = mutableStateOf(before)
        setLibraryWithUndo(state)
        openBookMenu()
        composeRule.onNodeWithText(context.getString(R.string.mark_finished)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.marked_finished_snackbar)).assertExists()
        assertEquals(10, state.value.page)
        composeRule.onNodeWithText(context.getString(R.string.action_undo)).performClick()
        composeRule.waitForIdle()
        assertEquals(before, state.value)
    }

    @Test
    fun mf11UndoRestoresStartedOverBook() {
        val before = BookState(page = 6, pageCount = 10, openedAt = 5)
        val state = mutableStateOf(before)
        setLibraryWithUndo(state)
        openBookMenu()
        composeRule.onNodeWithText(context.getString(R.string.start_over)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.progress_reset_snackbar)).assertExists()
        assertEquals(1, state.value.page)
        assertEquals(0L, state.value.openedAt)
        composeRule.onNodeWithText(context.getString(R.string.action_undo)).performClick()
        composeRule.waitForIdle()
        assertEquals(before, state.value)
    }

    private fun openBookMenu() {
        composeRule.onNodeWithTag("library_book").performTouchInput { longClick() }
        composeRule.waitForIdle()
    }

    private fun setLibraryWithUndo(state: MutableState<BookState>) {
        val message = mutableStateOf<String?>(null)
        val undo = mutableStateOf<BookState?>(null)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("Shelf", listOf(book)),
                    states = mapOf(key to state.value),
                    onOpen = {},
                    onSettings = {},
                    covers = mapOf(key to null),
                    resumeBooks = emptyList(),
                    snackbarMessage = message.value,
                    snackbarActionLabel = if (undo.value != null) {
                        context.getString(R.string.action_undo)
                    } else {
                        null
                    },
                    onSnackbarShown = { message.value = null },
                    onSnackbarAction = {
                        undo.value?.let { state.value = it }
                        undo.value = null
                    },
                    onMarkFinished = {
                        val previous = state.value
                        state.value = markFinished(previous)
                        undo.value = previous
                        message.value = context.getString(R.string.marked_finished_snackbar)
                    },
                    onStartOver = {
                        val previous = state.value
                        state.value = startOver(previous)
                        undo.value = previous
                        message.value = context.getString(R.string.progress_reset_snackbar)
                    },
                )
            }
        }
    }

    private fun setLibrary(
        state: BookState,
        finished: MutableList<BookInfo>,
        started: MutableList<BookInfo>,
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("Shelf", listOf(book)),
                    states = mapOf(key to state),
                    onOpen = {},
                    onSettings = {},
                    covers = mapOf(key to null),
                    resumeBooks = emptyList(),
                    onMarkFinished = { finished += it },
                    onStartOver = { started += it },
                )
            }
        }
    }
}
