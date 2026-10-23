package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class SortPersistComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sortChoiceSurvivesLeavingAndReturning() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saved = mutableMapOf<String, BookSort>()
        val sorts = mutableStateOf<Map<String, BookSort>>(emptyMap())
        val showLibrary = mutableStateOf(true)
        val folder = "content://shelf"
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                if (showLibrary.value) {
                    LibraryScreen(
                        listing = Listing("Shelf", listOf(BookInfo("content://a", "A.pdf", 1))),
                        states = emptyMap(),
                        onOpen = {},
                        onSettings = {},
                        sort = sortFor(sorts.value, folder),
                        onSortChange = { sort ->
                            saved[folder] = sort
                            sorts.value = saved.toMap()
                        },
                    )
                }
            }
        }
        // The folded bar shows the sort chip icon-only, so find it by its label
        // and read the chosen sort from its state description.
        val sortTitle = context.getString(R.string.sort_menu_title)
        val name = context.getString(R.string.sort_name)
        val showsName = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, name)
        composeRule.onNodeWithContentDescription(sortTitle).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(sortOptionTag(BookSort.Name)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(sortTitle).assert(showsName)
        showLibrary.value = false
        composeRule.waitForIdle()
        showLibrary.value = true
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(sortTitle).assert(showsName)
    }
}
