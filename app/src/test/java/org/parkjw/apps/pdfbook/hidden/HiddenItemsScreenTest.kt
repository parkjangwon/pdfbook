package org.parkjw.apps.pdfbook.hidden

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class HiddenItemsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun h5_unhideClickInvokesCallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item = HiddenItem(HiddenKind.Folder, "content://f", "Sample Folder", "content://f")
        val unhidden = mutableListOf<HiddenItem>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                HiddenItemsScreen(
                    items = listOf(item),
                    onBack = {},
                    onUnhide = { unhidden += it },
                )
            }
        }
        composeRule.onNodeWithText("Sample Folder").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.unhide_item)).performClick()
        assertEquals(listOf(item), unhidden)
    }

    @Test
    fun h5_emptyStateShowsCopy() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                HiddenItemsScreen(items = emptyList(), onBack = {}, onUnhide = {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.hidden_items_empty)).assertExists()
    }
}
