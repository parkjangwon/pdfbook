package org.parkjw.apps.pdfbook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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

        val width = composeRule.onRoot().fetchSemanticsNode().size.width
        val middle = composeRule.onAllNodesWithTag("reader_zone_Menu").fetchSemanticsNodes()
            .indexOfFirst { it.boundsInRoot.center.x in 1f..(width - 1f) }
        composeRule.onAllNodesWithTag("reader_zone_Menu")[middle].performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_overlay_top").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("reader_more_button").performClick()
        composeRule.onNodeWithTag("reader_more_spread_shift").performClick()
        awaitPage(4)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { store.state(bookKey(BOOK.uri, BOOK.size))?.spreadShift == 1 }
        }
        assertEquals(1, runBlocking { store.state(bookKey(BOOK.uri, BOOK.size))?.spreadShift })
    }

    private fun setReader(store: BookStore, initialPage: Int) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
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
        awaitPage(initialPage)
    }

    private fun awaitPage(page: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_page_$page").fetchSemanticsNodes().isNotEmpty()
        }
    }

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
        const val PAGE_COUNT = 8
        val BOOK = BookInfo(uri = "content://test/book.pdf", name = "book.pdf", size = 123)
    }
}
