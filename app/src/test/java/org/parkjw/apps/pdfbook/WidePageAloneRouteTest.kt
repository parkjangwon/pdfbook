package org.parkjw.apps.pdfbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
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
        setReader(widePageAlone = { widePageAlone }, initialPage = 3)

        composeRule.onNodeWithTag("reader_page_2").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()

        widePageAlone = true
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("reader_page_2").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()
    }

    @Test
    fun singlePageViewIsUnaffected() {
        setReader(widePageAlone = { true }, initialPage = 3, mode = TwoPageMode.AlwaysSingle)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()
    }

    private fun setReader(
        widePageAlone: () -> Boolean,
        initialPage: Int,
        mode: TwoPageMode = TwoPageMode.AlwaysDouble,
    ) {
        val store = storeAtPage(initialPage)
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
                    rendererOverride = SolidPageBitmaps(
                        PAGE_COUNT,
                        0xFFF7F2E8.toInt(),
                        0xFF3D5A80.toInt(),
                        widePageSet = setOf(WIDE_PAGE),
                    ),
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

    private fun storeAtPage(page: Int): BookStore {
        val store = newStore()
        runBlocking { store.save(bookKey(BOOK.uri, BOOK.size), BookState(page = page, pageCount = PAGE_COUNT)) }
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
        const val WIDE_PAGE = 3
        val BOOK = BookInfo(uri = "content://test/book.pdf", name = "book.pdf", size = 123)
    }
}
