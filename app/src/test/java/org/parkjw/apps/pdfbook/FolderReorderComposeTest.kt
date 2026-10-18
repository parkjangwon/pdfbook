package org.parkjw.apps.pdfbook

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class FolderReorderComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun card(uri: String) = FolderCardUi(
        uri = uri,
        displayName = "Folder $uri",
        pathTail = uri,
        bookCount = 1,
        coverKeys = emptyList(),
        resume = null,
        access = FolderAccess.Ok,
    )

    private fun setShelf(
        cards: List<FolderCardUi>,
        onReorder: (List<String>) -> Unit,
        onOpen: (String) -> Unit = {},
    ) {
        val shelf = mutableStateOf(FolderShelfUi(cards = cards, globalResume = null))
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                FolderShelfScreen(
                    shelf = shelf.value,
                    covers = emptyMap(),
                    snackbarMessage = null,
                    scrollToUri = null,
                    showRenameFor = null,
                    onSnackbarShown = {},
                    onScrollHandled = {},
                    onOpenFolder = onOpen,
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
                    onReorder = { order ->
                        onReorder(order)
                        val byUri = shelf.value.cards.associateBy { it.uri }
                        shelf.value = shelf.value.copy(cards = order.map { byUri.getValue(it) })
                    },
                )
            }
        }
    }

    private fun handles() =
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.folder_reorder_handle))

    private fun handleNodes() = handles().fetchSemanticsNodes().sortedBy { it.positionInRoot.y }

    private fun customAction(label: String) = SemanticsMatcher("has action $label") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)?.any { it.label == label } == true
    }

    @Test
    fun everyCardHasHandleWithMoveActionsOnlyWhereMovable() {
        setShelf(listOf(card("a"), card("b"), card("c")), onReorder = {})
        val up = context.getString(R.string.folder_move_up)
        val down = context.getString(R.string.folder_move_down)
        handles().assertCountEquals(3)
        val labels = handleNodes().map { node ->
            node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }
        }
        assertEquals(listOf(listOf(down), listOf(up, down), listOf(up)), labels)
        handles()[1].assert(customAction(up)).assert(customAction(down))
    }

    @Test
    fun accessibilityMoveUpAndDownReorder() {
        val saved = mutableListOf<List<String>>()
        setShelf(listOf(card("a"), card("b"), card("c")), onReorder = { saved += it })
        val up = context.getString(R.string.folder_move_up)
        val down = context.getString(R.string.folder_move_down)
        runCustomAction(1, up)
        assertEquals(listOf("b", "a", "c"), saved.last())
        runCustomAction(1, down)
        assertEquals(listOf("b", "c", "a"), saved.last())
        val first = handleNodes()[0].config[SemanticsActions.CustomActions].map { it.label }
        assertEquals(listOf(down), first)
    }

    @Test
    fun dragHandleAfterLongPressMovesCardDown() {
        val saved = mutableListOf<List<String>>()
        setShelf(listOf(card("a"), card("b"), card("c")), onReorder = { saved += it })
        val nodes = handleNodes()
        val step = nodes[1].positionInRoot.y - nodes[0].positionInRoot.y
        assertTrue(step > 0f)
        handles()[0].performTouchInput {
            down(center)
            advanceEventTime(1_000)
            moveBy(androidx.compose.ui.geometry.Offset(0f, step * 0.4f))
            advanceEventTime(50)
            moveBy(androidx.compose.ui.geometry.Offset(0f, step * 0.3f))
            advanceEventTime(50)
            up()
        }
        composeRule.waitForIdle()
        assertEquals(listOf("b", "a", "c"), saved.single())
    }

    @Test
    fun cardBodyLongPressStillOpensMenuAndTapOpens() {
        val opened = mutableListOf<String>()
        val saved = mutableListOf<List<String>>()
        setShelf(listOf(card("a"), card("b")), onReorder = { saved += it }, onOpen = { opened += it })
        composeRule.onNodeWithText("Folder a").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("a"), opened)
        handles()[0].performTouchInput { click(androidx.compose.ui.geometry.Offset(width / 2f, height + 24f)) }
        composeRule.waitForIdle()
        assertEquals(listOf("a", "a"), opened)
        val handleA = handleNodes().first().boundsInRoot
        composeRule.onNodeWithText("Folder a").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.folder_rename)).assertExists()
        assertTrue(saved.isEmpty())
        assertTrue(handleA.height < handleA.width * 1.5f)
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h920dp-xxhdpi")
    fun lastHandleScrollsClearOfAddFolderButtonOnFoldedScreen() {
        val cards = (1..8).map { card("f$it") }
        setShelf(cards, onReorder = {})
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(cards.lastIndex)
        repeat(3) {
            composeRule.onNode(hasScrollToIndexAction()).performTouchInput { swipeUp() }
            composeRule.waitForIdle()
        }
        val lastHandle = handleNodes().last()
        val fabText = composeRule.onNodeWithText(context.getString(R.string.folders_add), useUnmergedTree = true)
            .fetchSemanticsNode()
        val fab = generateSequence(fabText) { it.parent }.first { it.config.contains(SemanticsActions.OnClick) }
        val handleBottom = lastHandle.boundsInRoot.bottom
        val lastTitleTop = composeRule.onNodeWithText("Folder f8").fetchSemanticsNode().boundsInRoot.top
        assertTrue("handle ${lastHandle.boundsInRoot} title $lastTitleTop", lastHandle.boundsInRoot.top > lastTitleTop)
        assertTrue(
            "handle bottom $handleBottom must be above add button top ${fab.boundsInRoot.top}",
            handleBottom <= fab.boundsInRoot.top,
        )
    }

    private fun runCustomAction(index: Int, label: String) {
        val node = handleNodes()[index]
        val action = node.config[SemanticsActions.CustomActions].first { it.label == label }
        composeRule.runOnUiThread { action.action() }
        composeRule.waitForIdle()
    }
}
