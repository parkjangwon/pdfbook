package org.parkjw.apps.pdfbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.screenshot.SolidPageBitmaps
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.settings.TwoPageMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w841dp-h701dp-xxhdpi")
class SpreadShiftRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private var open by mutableStateOf(true)

    @Test
    fun savedShiftOpensTheShiftedPair() {
        val store = storeAt(page = 4, shift = 1)
        setReader(store, initialPage = 4)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_5").assertDoesNotExist()
    }

    @Test
    fun legacyStateWithoutShiftOpensThePlainPair() {
        val store = storeAt(page = 4, shift = 0)
        setReader(store, initialPage = 4)

        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_5").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertDoesNotExist()
    }

    @Test
    fun rowTapRepairsThePairAndKeepsThePageBeingRead() {
        val store = storeAt(page = 3, shift = 0)
        setReader(store, initialPage = 3)

        composeRule.onNodeWithTag("reader_page_2").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()

        openMenu()
        composeRule.onNodeWithTag("reader_more_spread_shift").performClick()
        awaitPage(4)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
        awaitSaved(store, shift = 1)
        assertEquals(3, savedPage(store))
    }

    @Test
    fun secondTapRestoresThePairAndKeepsThePage() {
        val store = storeAt(page = 12, shift = 0)
        setReader(store, initialPage = 12)
        assertPair(12, 13)

        openMenu()
        composeRule.onNodeWithTag("reader_more_spread_shift").performClick()
        awaitPage(11)
        assertPair(11, 12)
        composeRule.onNodeWithTag("reader_progress").assertTextEquals("12 / 30")
        awaitSaved(store, shift = 1)
        assertEquals(12, savedPage(store))

        composeRule.onNodeWithTag("reader_more_spread_shift").performClick()
        awaitPage(13)
        assertPair(12, 13)
        composeRule.onNodeWithTag("reader_progress").assertTextEquals("12 / 30")
        awaitSaved(store, shift = 0)
        assertEquals(12, savedPage(store))
    }

    @Test
    fun tapDuringATurnAppliesOnlyAfterItSettles() {
        val store = storeAt(page = 12, shift = 0)
        setReader(store, initialPage = 12)

        openMenu()
        composeRule.mainClock.autoAdvance = false
        clickZone("reader_zone_Next")
        composeRule.mainClock.advanceTimeBy(80)
        composeRule.onNodeWithTag("reader_more_spread_shift").performClick()
        composeRule.mainClock.advanceTimeBy(80)
        assertEquals(0, runBlocking { store.state(bookKey(BOOK.uri, BOOK.size))?.spreadShift })

        composeRule.mainClock.autoAdvance = true
        awaitPage(13)
        // The turn settled on 14; the deferred shift pairs it with 13.
        assertPair(13, 14)
        awaitSaved(store, shift = 1)
        assertEquals(14, savedPage(store))
    }

    @Test
    fun jumpBackAndReopenKeepThePageWithTheShiftOn() {
        val store = storeAt(page = 12, shift = 1)
        setReader(store, initialPage = 12)
        assertPair(11, 12)

        showOverlay()
        composeRule.onNodeWithTag("reader_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(23f) }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("12쪽으로 돌아가기").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("12쪽으로 돌아가기").performClick()
        composeRule.waitForIdle()
        awaitPage(11)
        assertPair(11, 12)
        awaitSaved(store, shift = 1, page = 12)
        composeRule.onNodeWithTag("reader_progress").assertTextEquals("12 / 30")

        open = false
        composeRule.waitForIdle()
        open = true
        composeRule.waitForIdle()
        awaitPage(11)
        assertPair(11, 12)
        showOverlay()
        composeRule.onNodeWithTag("reader_progress").assertTextEquals("12 / 30")
    }

    private fun setReader(store: BookStore, initialPage: Int) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                if (open) {
                    ReaderRoute(
                        book = BOOK,
                        store = store,
                        volumeDelta = emptyFlow(),
                        defaultReadingDirection = ReadingDirection.LTR,
                        onBack = {},
                        twoPageMode = TwoPageMode.AlwaysDouble,
                        coverAlone = true,
                        rendererOverride = SolidPageBitmaps(PAGE_COUNT, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                    )
                }
            }
        }
        awaitPage(initialPage)
    }

    private fun showOverlay() {
        clickZone("reader_zone_Menu")
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_overlay_top").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openMenu() {
        showOverlay()
        composeRule.onNodeWithTag("reader_more_button").performClick()
    }

    private fun clickZone(tag: String) {
        val width = composeRule.onRoot().fetchSemanticsNode().size.width
        val index = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
            .indexOfFirst { it.boundsInRoot.center.x in 1f..(width - 1f) }
        composeRule.onAllNodesWithTag(tag)[index].performClick()
    }

    private fun assertPair(first: Int, second: Int) {
        composeRule.onNodeWithTag("reader_page_$first").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_$second").assertIsDisplayed()
    }

    private fun awaitPage(page: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_page_$page").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitSaved(store: BookStore, shift: Int, page: Int? = null) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            val state = runBlocking { store.state(bookKey(BOOK.uri, BOOK.size)) }
            state?.spreadShift == shift && (page == null || state.page == page)
        }
    }

    private fun savedPage(store: BookStore): Int? =
        runBlocking { store.state(bookKey(BOOK.uri, BOOK.size))?.page }

    private fun storeAt(page: Int, shift: Int): BookStore {
        val store = newStore()
        runBlocking {
            store.save(
                bookKey(BOOK.uri, BOOK.size),
                BookState(page = page, pageCount = PAGE_COUNT, spreadShift = shift),
            )
        }
        return store
    }

    private fun newStore(): BookStore {
        val file = File(tmp.newFolder(), "book.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { file },
        )
        return BookStore(dataStore)
    }

    private companion object {
        const val PAGE_COUNT = 30
        val BOOK = BookInfo(uri = "content://test/book.pdf", name = "book.pdf", size = 123)
    }
}
