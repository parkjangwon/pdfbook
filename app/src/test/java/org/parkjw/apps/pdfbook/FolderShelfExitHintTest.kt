package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class FolderShelfExitHintTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exitHintTokenShowsConfirmText() {
        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.exit_confirm)
        composeRule.setContent {
            PdfBookTheme {
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
                    exitHintToken = 1,
                )
            }
        }
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }
}
