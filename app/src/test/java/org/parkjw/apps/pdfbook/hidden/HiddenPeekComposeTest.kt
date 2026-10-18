package org.parkjw.apps.pdfbook.hidden

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.FolderCardUi
import org.parkjw.apps.pdfbook.FolderShelfScreen
import org.parkjw.apps.pdfbook.FolderShelfUi
import org.parkjw.apps.pdfbook.LibraryScreen
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class HiddenPeekComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetPeek() {
        HiddenPeek.reset()
    }

    @Test
    fun h8_peekMenuAndBannerToggle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            var peek by remember { mutableStateOf(false) }
            PdfBookTheme(themeMode = ThemeMode.Light) {
                FolderShelfScreen(
                    shelf = FolderShelfUi(cards = emptyList(), globalResume = null),
                    covers = emptyMap(),
                    snackbarMessage = null,
                    scrollToUri = null,
                    showRenameFor = null,
                    onSnackbarShown = {},
                    onScrollHandled = {},
                    onOpenFolder = {},
                    onOpenResume = {},
                    onAddFolder = {},
                    onSettings = {},
                    onRenameRequest = {},
                    onRenameConfirm = { _, _ -> },
                    onRenameDismiss = {},
                    onRepick = {},
                    onRemove = {},
                    onLostFolder = {},
                    modifier = Modifier.fillMaxSize(),
                    peekEnabled = peek,
                    peekMenuEnabled = true,
                    onPeekToggle = { peek = !peek },
                    showPeekMenu = true,
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_on)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_banner)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_stop)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_banner)).assertDoesNotExist()
    }

    @Test
    fun h9_longPressHiddenFolderUnhidesImmediately() {
        val unhidden = mutableListOf<String>()
        val card = FolderCardUi(
            uri = "content://hidden",
            displayName = "Sample Folder",
            pathTail = "hidden",
            bookCount = 0,
            coverKeys = emptyList(),
            resume = null,
            access = FolderAccess.Ok,
            isHidden = true,
        )
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                FolderShelfScreen(
                    shelf = FolderShelfUi(cards = listOf(card), globalResume = null),
                    covers = emptyMap(),
                    snackbarMessage = null,
                    scrollToUri = null,
                    showRenameFor = null,
                    onSnackbarShown = {},
                    onScrollHandled = {},
                    onOpenFolder = {},
                    onOpenResume = {},
                    onAddFolder = {},
                    onSettings = {},
                    onRenameRequest = {},
                    onRenameConfirm = { _, _ -> },
                    onRenameDismiss = {},
                    onRepick = {},
                    onRemove = {},
                    onLostFolder = {},
                    modifier = Modifier.fillMaxSize(),
                    peekEnabled = true,
                    peekMenuEnabled = true,
                    onUnhide = { unhidden += it },
                )
            }
        }
        composeRule.onNodeWithText("Sample Folder").performTouchInput { longClick() }
        assertEquals(listOf("content://hidden"), unhidden)
    }

    @Test
    fun h13_peekMenuItemDisabledWhenEmpty() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                FolderShelfScreen(
                    shelf = FolderShelfUi(cards = emptyList(), globalResume = null),
                    covers = emptyMap(),
                    snackbarMessage = null,
                    scrollToUri = null,
                    showRenameFor = null,
                    onSnackbarShown = {},
                    onScrollHandled = {},
                    onOpenFolder = {},
                    onOpenResume = {},
                    onAddFolder = {},
                    onSettings = {},
                    onRenameRequest = {},
                    onRenameConfirm = { _, _ -> },
                    onRenameDismiss = {},
                    onRepick = {},
                    onRemove = {},
                    onLostFolder = {},
                    modifier = Modifier.fillMaxSize(),
                    peekMenuEnabled = false,
                    showPeekMenu = true,
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_on)).assertIsNotEnabled()
    }

    @Test
    fun h8_libraryPeekMenuLabelFollowsState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("Shelf", listOf(BookInfo("content://a", "A.pdf", 1))),
                    states = emptyMap(),
                    onOpen = {},
                    onSettings = {},
                    peekEnabled = true,
                    peekMenuEnabled = true,
                    showPeekMenu = true,
                    peekHiddenBookIds = emptySet(),
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_off)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.peek_hidden_banner)).assertExists()
    }
}
