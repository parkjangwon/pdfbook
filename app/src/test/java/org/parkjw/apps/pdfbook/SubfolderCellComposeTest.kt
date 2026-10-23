package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
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
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w411dp-h891dp")
class SubfolderCellComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun item(hidden: Boolean, books: Int = 3) = SubfolderItem(
        node = FolderNode("s1", "doc:s1", "Series One", emptyList(), emptyList()),
        path = listOf("s1"),
        bookCount = books,
        coverBooks = emptyList(),
        isHidden = hidden,
    )

    private class Calls {
        var open = 0
        var hide = 0
        var unhide = 0
    }

    private fun setCell(item: SubfolderItem, calls: Calls) {
        composeRule.setContent {
            var menu by remember { mutableStateOf(false) }
            PdfBookTheme(themeMode = ThemeMode.Light) {
                SubfolderCell(
                    item = item,
                    covers = emptyMap(),
                    onOpen = { calls.open++ },
                    menuExpanded = menu,
                    onMenuExpand = { menu = true },
                    onMenuDismiss = { menu = false },
                    onHide = {
                        menu = false
                        calls.hide++
                    },
                    onUnhide = { calls.unhide++ },
                )
            }
        }
    }

    @Test
    fun showsNameAndBookCountAndOpensOnTap() {
        val calls = Calls()
        setCell(item(hidden = false, books = 3), calls)
        composeRule.onNodeWithText("Series One").assertIsDisplayed()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.library_subfolder_count, 3, 3),
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("library_subfolder").performClick()
        assertEquals(1, calls.open)
    }

    @Test
    fun longPressOpensHideMenuAndHideInvokesCallback() {
        val calls = Calls()
        setCell(item(hidden = false), calls)
        composeRule.onNodeWithText(context.getString(R.string.hide_item)).assertDoesNotExist()
        composeRule.onNodeWithText("Series One").performTouchInput { longClick() }
        composeRule.onNodeWithText(context.getString(R.string.hide_item)).performClick()
        composeRule.waitForIdle()
        assertEquals(1, calls.hide)
        assertEquals(0, calls.unhide)
        assertEquals(0, calls.open)
    }

    @Test
    fun hiddenSubfolderLongPressUnhidesWithoutMenu() {
        val calls = Calls()
        setCell(item(hidden = true), calls)
        composeRule.onNodeWithText("Series One").performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertEquals(1, calls.unhide)
        composeRule.onNodeWithText(context.getString(R.string.hide_item)).assertDoesNotExist()
    }
}
