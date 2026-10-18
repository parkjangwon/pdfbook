package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
class SortCheckMenuComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val root = "content://shelf"

    @Test
    fun sc1OnlyCurrentSortIsChecked() {
        setLibrary(mapOf(sortKey(root) to BookSort.Name))
        openMenu()
        assertChecked(BookSort.Name)
    }

    @Test
    fun sc2PickingOtherSortClosesMenuAndMovesCheck() {
        val picked = mutableListOf<BookSort>()
        val sorts = setLibrary(mapOf(sortKey(root) to BookSort.Name), onPick = { picked += it })
        openMenu()
        composeRule.onNodeWithTag(sortOptionTag(BookSort.Recent)).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(BookSort.Recent), picked)
        assertEquals(BookSort.Recent, sortFor(sorts(), root))
        composeRule.onAllNodesWithTag(sortOptionTag(BookSort.Recent)).assertCountEquals(0)
        assertButton(BookSort.Recent)
        openMenu()
        assertChecked(BookSort.Recent)
    }

    @Test
    fun sc2PickingCurrentSortOnlyClosesMenu() {
        val picked = mutableListOf<BookSort>()
        setLibrary(mapOf(sortKey(root) to BookSort.Name), onPick = { picked += it })
        openMenu()
        composeRule.onNodeWithTag(sortOptionTag(BookSort.Name)).performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<BookSort>(), picked)
        composeRule.onAllNodesWithTag(sortOptionTag(BookSort.Name)).assertCountEquals(0)
    }

    @Test
    fun sc3UnsetSubfolderChecksInheritedSort() {
        setLibrary(mapOf(sortKey(root) to BookSort.Name), path = listOf("sub"))
        openMenu()
        assertChecked(BookSort.Name)
    }

    @Test
    fun neverPickedFolderChecksRecent() {
        setLibrary(emptyMap())
        openMenu()
        assertChecked(BookSort.Recent)
    }

    @Test
    fun sc4CheckedRowIsSelectedAndButtonIsNamed() {
        setLibrary(mapOf(sortKey(root) to BookSort.Name))
        assertButton(BookSort.Name)
        openMenu()
        composeRule.onNodeWithTag(sortOptionTag(BookSort.Name)).assertIsSelected()
        composeRule.onNodeWithTag(sortOptionTag(BookSort.Recent)).assertIsNotSelected()
        BookSort.entries.forEach { option ->
            composeRule.onNodeWithTag(sortOptionTag(option))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
        composeRule.onAllNodesWithText(context.getString(R.string.sort_menu_title)).assertCountEquals(0)
    }

    private fun assertButton(sort: BookSort) {
        val button = composeRule.onNodeWithTag(SORT_BUTTON_TAG)
        button.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf(context.getString(R.string.sort_menu_title)),
            ),
        )
        button.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(sortLabelRes(sort)),
            ),
        )
    }

    private fun assertChecked(sort: BookSort) {
        BookSort.entries.forEach { option ->
            val node = composeRule.onNodeWithTag(sortOptionTag(option))
            if (option == sort) node.assertIsSelected() else node.assertIsNotSelected()
        }
        composeRule.onAllNodesWithTag(SORT_CHECK_TAG, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNode(
            hasTestTag(SORT_CHECK_TAG) and hasAnyAncestor(hasTestTag(sortOptionTag(sort))),
            useUnmergedTree = true,
        ).assertExists()
    }

    private fun openMenu() {
        composeRule.onNodeWithTag(SORT_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
    }

    private fun setLibrary(
        initial: Map<String, BookSort>,
        path: List<String> = emptyList(),
        onPick: (BookSort) -> Unit = {},
    ): () -> Map<String, BookSort> {
        var sorts by mutableStateOf(initial)
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                LibraryScreen(
                    listing = Listing("Shelf", listOf(BookInfo("content://a", "A.pdf", 1))),
                    states = emptyMap(),
                    onOpen = {},
                    onSettings = {},
                    sort = sortFor(sorts, root, path),
                    onSortChange = { sort ->
                        onPick(sort)
                        sorts = sorts + (sortKey(root, path) to sort)
                    },
                )
            }
        }
        return { sorts }
    }
}
