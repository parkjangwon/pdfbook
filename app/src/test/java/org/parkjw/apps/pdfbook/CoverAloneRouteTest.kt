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
class CoverAloneRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun coverAloneOffPairsCoverWithPageTwo() {
        setReader(coverAlone = { false }, initialPage = 1)

        composeRule.onNodeWithTag("reader_page_1").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertIsDisplayed()
    }

    @Test
    fun coverAloneOnShowsCoverByItself() {
        setReader(coverAlone = { true }, initialPage = 1)

        composeRule.onNodeWithTag("reader_page_1").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_2").assertDoesNotExist()
    }

    @Test
    fun togglingWhileOpenKeepsAnchorPage() {
        val store = storeAtPage(3)
        var coverAlone by mutableStateOf(true)
        setReader(coverAlone = { coverAlone }, initialPage = 3, store = store)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertDoesNotExist()

        coverAlone = false
        awaitPage(4)

        composeRule.onNodeWithTag("reader_page_3").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_4").assertIsDisplayed()
        composeRule.onNodeWithTag("reader_page_1").assertDoesNotExist()
    }

    private fun setReader(coverAlone: () -> Boolean, initialPage: Int, store: BookStore = newStore()) {
        composeRule.setContent {
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderRoute(
                    book = BOOK,
                    store = store,
                    volumeDelta = emptyFlow(),
                    defaultReadingDirection = ReadingDirection.LTR,
                    onBack = {},
                    twoPageMode = TwoPageMode.AlwaysDouble,
                    coverAlone = coverAlone(),
                    rendererOverride = SolidPageBitmaps(PAGE_COUNT, 0xFFF7F2E8.toInt(), 0xFF3D5A80.toInt()),
                )
            }
        }
        awaitPage(initialPage)
    }

    // ReaderRoute hides its content until store.state() finishes reading the DataStore on
    // its own dispatcher, which the Compose idle check does not track; wait for the real page.
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
        val BOOK = BookInfo(uri = "content://test/book.pdf", name = "book.pdf", size = 123)
    }
}
