package org.parkjw.apps.pdfbook

import android.app.Application
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PrerenderReaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var pagerState: PagerState
    private val opened = mutableListOf<PageRenderer>()

    @After
    fun closeRenderers() {
        opened.forEach { it.close() }
    }

    @Test
    fun nextPageIsFinalFromFirstFrameAfterWaiting() {
        val renderer = renderer(8)
        setReader(renderer, spreads(8, twoPage = false, coverAlone = true), ReadingDirection.LTR)
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitUntil(5_000) { 2 in renderer.cache.pages() }
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.9f, height / 2f)) }
        repeat(40) {
            composeRule.mainClock.advanceTimeByFrame()
            assertNoPlaceholder(composeRule.onRoot().captureToImage().toPixelMap())
        }
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun prerenderedPageComposedFreshDrawsOnFirstFrame() {
        assertTrue(firstFrameShowsPage(useCache = true))
    }

    @Test
    fun withoutCacheLookupFirstFrameIsPlaceholder() {
        assertFalse(firstFrameShowsPage(useCache = false))
    }

    private fun firstFrameShowsPage(useCache: Boolean): Boolean {
        val renderer = renderer(4)
        renderer.prerender(emptyList(), listOf(PageRequest(2, 300, 400)))
        runBlocking { renderer.awaitPrerender() }
        val bitmap = renderer.cached(2, 300, 400)!!
        val expected = Color(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PageBitmap(StalledView(renderer, useCache), 2, 300, 400, Modifier.size(300.dp, 400.dp))
        }
        return isClose(expected, centerPixel())
    }

    @Test
    fun rtlTwoPageTargetsNextSpreadThenPrevious() {
        val recording = RecordingPageBitmaps(9)
        val pages = spreads(9, twoPage = true, coverAlone = true)
        setReader(recording, pages, ReadingDirection.RTL, twoPage = true, initialPage = 1)
        composeRule.waitForIdle()
        val first = recording.calls.last()
        assertEquals(listOf(2, 3), first.first.map { it.page })
        assertEquals(listOf(4, 5, 1), first.second.map { it.page })
        val half = first.second[0].maxWidth
        assertEquals(half, first.second[1].maxWidth)
        assertEquals(half * 2, first.second[2].maxWidth)
        composeRule.onRoot().performTouchInput { click(Offset(width * 0.1f, height / 2f)) }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        val next = recording.calls.last()
        assertEquals(listOf(4, 5), next.first.map { it.page })
        assertEquals(listOf(6, 7, 2, 3), next.second.map { it.page })
    }

    @Test
    fun trimMemoryLeavesOnlyCurrentPage() {
        val renderer = renderer(8)
        setReader(renderer, spreads(8, twoPage = false, coverAlone = true), ReadingDirection.LTR, initialPage = 2)
        composeRule.waitUntil(5_000) { renderer.cache.pages().containsAll(listOf(2, 3, 4)) }
        runBlocking { renderer.awaitPrerender() }
        ApplicationProvider.getApplicationContext<Application>()
            .onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertEquals(setOf(3), renderer.cache.pages())
        composeRule.waitForIdle()
        assertEquals(setOf(3), renderer.cache.pages())
    }

    private fun renderer(pageCount: Int): PageRenderer =
        PageRenderer(GeneratedImagePdf.write(folder.newFile(), pageCount).open()).also { opened += it }

    private fun assertNoPlaceholder(pixels: PixelMap) {
        val y = pixels.height / 2
        for (x in 0 until pixels.width) {
            assertTrue("placeholder at x=$x", pixels[x, y] != ReaderBackground)
        }
    }

    private fun centerPixel(): Color {
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    private fun isClose(a: Color, b: Color): Boolean =
        abs(a.red - b.red) < 0.05f && abs(a.green - b.green) < 0.05f

    private fun setReader(
        renderer: PageBitmaps,
        pages: List<List<Int>>,
        direction: ReadingDirection,
        twoPage: Boolean = false,
        initialPage: Int = 0,
    ) {
        composeRule.setContent {
            pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pages.size })
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = renderer,
                    pagerState = pagerState,
                    spreads = pages,
                    title = "t",
                    pageCount = renderer.pageCount,
                    currentPage = 1,
                    twoPage = twoPage,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                )
            }
        }
    }

    private class StalledView(
        private val delegate: PageBitmaps,
        private val useCache: Boolean,
    ) : PageBitmaps {
        override val pageCount: Int get() = delegate.pageCount

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? = awaitCancellation()

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            if (useCache) delegate.cached(page, maxWidth, maxHeight) else null
    }

    private class RecordingPageBitmaps(override val pageCount: Int) : PageBitmaps {
        val calls = mutableListOf<Pair<List<PageRequest>, List<PageRequest>>>()

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        override fun prerender(current: List<PageRequest>, targets: List<PageRequest>) {
            calls += current to targets
        }
    }
}
