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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w411dp-h891dp")
class FolderShelfStatesComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun card(uri: String, name: String, access: FolderAccess = FolderAccess.Ok) = FolderCardUi(
        uri = uri,
        displayName = name,
        pathTail = "tail",
        bookCount = 2,
        coverKeys = emptyList(),
        resume = null,
        access = access,
    )

    private class Calls {
        val opened = mutableListOf<String>()
        val lost = mutableListOf<String>()
        val removed = mutableListOf<String>()
        var snackbarShown = 0
        var snackbarAction = 0
        var added = 0
    }

    private fun setShelf(
        cards: List<FolderCardUi>,
        calls: Calls,
        snackbarMessage: String? = null,
        snackbarActionLabel: String? = null,
        showRemoveConfirmFor: String? = null,
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                FolderShelfScreen(
                    shelf = FolderShelfUi(cards = cards, globalResume = null),
                    covers = emptyMap(),
                    snackbarMessage = snackbarMessage,
                    scrollToUri = null,
                    showRenameFor = null,
                    onSnackbarShown = { calls.snackbarShown++ },
                    onScrollHandled = {},
                    onOpenFolder = { calls.opened += it },
                    onOpenResume = {},
                    onAddFolder = { calls.added++ },
                    onSettings = {},
                    onRenameRequest = {},
                    onRenameConfirm = { _, _ -> },
                    onRenameDismiss = {},
                    onRepick = {},
                    onRemove = { calls.removed += it },
                    onLostFolder = { calls.lost += it },
                    modifier = Modifier.fillMaxSize(),
                    showRemoveConfirmFor = showRemoveConfirmFor,
                    snackbarActionLabel = snackbarActionLabel,
                    onSnackbarAction = { calls.snackbarAction++ },
                )
            }
        }
    }

    @Test
    fun emptyShelfShowsOnlyTitleAndAddButton() {
        val calls = Calls()
        setShelf(emptyList(), calls)
        composeRule.onNodeWithText(context.getString(R.string.folders_title)).assertIsDisplayed()
        composeRule.onAllNodesWithTag("folder_card").assertCountEquals(0)
        composeRule.onAllNodesWithTag("folder_shelf_resume").assertCountEquals(0)
        composeRule.onNodeWithTag("folder_shelf_add").performClick()
        assertEquals(1, calls.added)
    }

    @Test
    fun lostFolderCardShowsBadgeAndTapRoutesToLostHandler() {
        val calls = Calls()
        setShelf(
            listOf(card("doc:ok", "Fine"), card("doc:lost", "Gone", FolderAccess.Lost)),
            calls,
        )
        composeRule.onNodeWithText(context.getString(R.string.folder_no_access_badge)).assertIsDisplayed()
        composeRule.onNodeWithText("Gone").performClick()
        composeRule.onNodeWithText("Fine").performClick()
        assertEquals(listOf("doc:lost"), calls.lost)
        assertEquals(listOf("doc:ok"), calls.opened)
    }

    @Test
    fun removeConfirmDialogConfirmsAndCancels() {
        val calls = Calls()
        setShelf(listOf(card("doc:a", "A")), calls, showRemoveConfirmFor = "doc:a")
        composeRule.onNodeWithText(context.getString(R.string.folder_remove_confirm_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.folder_remove_confirm_title)).assertDoesNotExist()
        assertEquals(emptyList<String>(), calls.removed)
    }

    @Test
    fun removeConfirmButtonRemovesFolder() {
        val calls = Calls()
        setShelf(listOf(card("doc:a", "A")), calls, showRemoveConfirmFor = "doc:a")
        composeRule.onNodeWithText(context.getString(R.string.folder_remove)).performClick()
        assertEquals(listOf("doc:a"), calls.removed)
        composeRule.onNodeWithText(context.getString(R.string.folder_remove_confirm_title)).assertDoesNotExist()
    }

    @Test
    fun snackbarShowsMessageActionAndReportsShown() {
        val calls = Calls()
        setShelf(emptyList(), calls, snackbarMessage = "Folder removed", snackbarActionLabel = "Undo")
        composeRule.onNodeWithText("Folder removed").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()
        assertEquals(1, calls.snackbarAction)
        assertEquals(1, calls.snackbarShown)
    }
}
