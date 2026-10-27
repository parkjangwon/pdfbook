package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import org.junit.Before
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
class WidePageAloneRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    @Before
    fun clearScanCache() {
        WidePageScanCache.clear()
    }

    @Test
    fun widePageIsShownAloneWhenTheSettingIsOn() {
        setReader(widePageAlone = { true }, initialPage = 3)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()
    }

    @Test
    fun widePagePairsWithItsNeighbourWhenTheSettingIsOff() {
        setReader(widePageAlone = { false }, initialPage = 2)

        composeRule.onNodeWithTag("reader_page_2").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
    }

    @Test
    fun pairingContinuesAroundTheWidePage() {
        setReader(widePageAlone = { true }, initialPage = 4)

        // Pages 1..8 with the cover alone and page 3 wide: [1] [2] [3] [4·5] [6·7] [8].
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_5").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_6").assertDoesNotExist()
    }

    @Test
    fun turningTheSettingOnWhileOpenMovesTheWidePageOutOfItsPair() {
        var widePageAlone by mutableStateOf(false)
        val store = storeAtPage(3)
        setReader(widePageAlone = { widePageAlone }, initialPage = 3, store = store)

        composeRule.onNodeWithTag("reader_page_2").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()

        widePageAlone = true
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_page_2").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()
        clickZone("reader_zone_Menu")
        composeRule.onNodeWithTag("reader_progress").assertTextEquals("3 / 8")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { store.state(bookKey(BOOK.uri, BOOK.size))?.page } == 3
        }
    }

    @Test
    fun singlePageViewIsUnaffected() {
        setReader(widePageAlone = { true }, initialPage = 3, mode = TwoPageMode.AlwaysSingle)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()
    }

    @Test
    fun singlePageViewNeverScansForWidePages() {
        val renderer = CountingBitmaps(PAGE_COUNT, WIDE_PAGE)
        setReader(
            widePageAlone = { true },
            initialPage = 3,
            mode = TwoPageMode.AlwaysSingle,
            renderer = renderer,
        )
        composeRule.waitForIdle()
        assertEquals(0, renderer.scanCalls)
    }

    @Test
    fun theSwitchOffNeverScansForWidePages() {
        val renderer = CountingBitmaps(PAGE_COUNT, WIDE_PAGE)
        setReader(
            widePageAlone = { false },
            initialPage = 3,
            mode = TwoPageMode.AlwaysDouble,
            renderer = renderer,
        )
        composeRule.waitForIdle()
        assertEquals(0, renderer.scanCalls)
    }

    @Test
    fun twoPageViewScansOnceFromTheSavedPage() {
        val renderer = CountingBitmaps(PAGE_COUNT, WIDE_PAGE)
        setReader(widePageAlone = { true }, initialPage = 5, renderer = renderer)
        composeRule.waitForIdle()

        assertEquals(1, renderer.scanCalls)
        assertEquals(5, renderer.lastAnchor)
    }

    @Test
    fun turningOntoAndOffTheWidePageLandsOnTheNextSpread() {
        setReader(widePageAlone = { true }, initialPage = 2)

        clickZone("reader_zone_Next")
        awaitPage(3)
        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()

        clickZone("reader_zone_Next")
        awaitPage(4)
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_5").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertDoesNotExist()
    }

    @Test
    fun shiftAndWidePageMakeTwoLoneSpreadsInARow() {
        setReader(
            widePageAlone = { true },
            initialPage = 3,
            widePage = 4,
            shift = 1,
        )

        // Cover alone, shift, page 4 wide: [1] [2] [3] [4] [5·6] [7·8].
        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()

        clickZone("reader_zone_Next")
        awaitPage(4)
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_5").assertDoesNotExist()
    }

    private fun setReader(
        widePageAlone: () -> Boolean,
        initialPage: Int,
        mode: TwoPageMode = TwoPageMode.AlwaysDouble,
        renderer: PageBitmaps? = null,
        widePage: Int = WIDE_PAGE,
        shift: Int = 0,
        store: BookStore = storeAtPage(initialPage, shift),
    ) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderRoute(
                    book = BOOK,
                    store = store,
                    volumeDelta = emptyFlow(),
                    defaultReadingDirection = ReadingDirection.LTR,
                    onBack = {},
                    twoPageMode = mode,
                    coverAlone = true,
                    widePageAlone = widePageAlone(),
                    rendererOverride = renderer ?: SolidPageBitmaps(
                        PAGE_COUNT,
                        0xFFF7F2E8.toInt(),
                        0xFF3D5A80.toInt(),
                        widePageSet = setOf(widePage),
                    ),
                )
            }
        }
        awaitPage(initialPage)
    }

    private fun clickZone(tag: String) {
        val width = composeRule.onRoot().fetchSemanticsNode().size.width
        val index = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
            .indexOfFirst { it.boundsInRoot.center.x in 1f..(width - 1f) }
        composeRule.onAllNodesWithTag(tag)[index].performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
    }

    private fun awaitPage(page: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_page_$page").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun storeAtPage(page: Int, shift: Int = 0): BookStore {
        val store = newStore()
        runBlocking {
            store.save(
                bookKey(BOOK.uri, BOOK.size),
                BookState(page = page, pageCount = PAGE_COUNT, spreadShift = shift),
            )
        }
        return store
    }

    private class CountingBitmaps(
        override val pageCount: Int,
        widePage: Int,
    ) : PageBitmaps {
        private val delegate = SolidPageBitmaps(
            pageCount,
            0xFFF7F2E8.toInt(),
            0xFF3D5A80.toInt(),
            widePageSet = setOf(widePage),
        )
        var scanCalls = 0
            private set
        var lastAnchor = 0
            private set

        override suspend fun widePages(anchor: Int): Set<Int> {
            scanCalls += 1
            lastAnchor = anchor
            return delegate.widePages(anchor)
        }

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            delegate.render(page, maxWidth, maxHeight)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            delegate.cached(page, maxWidth, maxHeight)
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
        const val WIDE_PAGE = 3
        val BOOK = BookInfo(uri = "content://test/book.pdf", name = "book.pdf", size = 123)
    }
}
