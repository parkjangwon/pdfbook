package org.parkjw.apps.pdfbook

import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w400dp-h800dp-mdpi")
class PageCurlResumeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var pagerState: PagerState
    private val app: Context = ApplicationProvider.getApplicationContext()
    private val owner = TestLifecycleOwner()

    @Before
    fun animationsOn() {
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    @Test
    fun firstTurnAfterResumeCurls() {
        val renderer = CachePages(PAGE_COUNT)
        setReader(renderer)
        composeRule.runOnIdle {
            // onTrimMemory(UI_HIDDEN) keeps only the current page while backgrounded.
            renderer.trimToCurrent()
            owner.background()
            owner.foreground()
        }
        composeRule.mainClock.autoAdvance = false
        tapNext()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag("reader_page_curl").assertExists()
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        assertEquals(1, pagerState.currentPage)
    }

    @Test
    fun uncachedNextPageAfterResumeStillFallsBackToSlide() {
        setReader(UncachedPages(PAGE_COUNT))
        composeRule.runOnIdle {
            owner.background()
            owner.foreground()
        }
        composeRule.mainClock.autoAdvance = false
        tapNext()
        repeat(20) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
        }
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        assertEquals(1, pagerState.currentPage)
    }

    private fun setReader(renderer: PageBitmaps) {
        val pageSpreads = spreads(PAGE_COUNT, twoPage = false, coverAlone = true)
        composeRule.setContent {
            pagerState = rememberPagerState(initialPage = 0, pageCount = { pageSpreads.size })
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                PdfBookTheme(themeMode = ThemeMode.Light) {
                    ReaderScreen(
                        renderer = renderer,
                        pagerState = pagerState,
                        spreads = pageSpreads,
                        title = "책 1권",
                        pageCount = PAGE_COUNT,
                        currentPage = 1,
                        twoPage = false,
                        onBack = {},
                        onDirectionChange = {},
                        direction = ReadingDirection.LTR,
                        volumeDelta = emptyFlow(),
                        showTapZoneGuide = false,
                        pageTurnAnimation = PageTurnAnimation.Curl,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        owner.foreground()
        composeRule.waitForIdle()
    }

    private fun tapNext() {
        composeRule.onRoot().performTouchInput {
            click(Offset(width * 0.9f, height / 2f))
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry

        fun foreground() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun background() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    /** Mirrors PageRenderer: prerender fills the cache, trimToCurrent keeps only the visible pages. */
    private class CachePages(override val pageCount: Int) : PageBitmaps {
        private val pages = (1..pageCount).associateWith { page ->
            Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (page % 2 == 0) 0xFFE8F0F7.toInt() else 0xFFF7F2E8.toInt())
            }
        }
        private val available = mutableSetOf<Int>()
        private var currentPages = emptySet<Int>()

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            pages[page]?.takeIf { page in available }

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            cached(page, maxWidth, maxHeight)

        override fun prerender(current: List<PageRequest>, targets: List<PageRequest>) {
            currentPages = current.mapTo(mutableSetOf()) { it.page }
            available += currentPages
            available += targets.map { it.page }
        }

        override fun trimToCurrent() {
            available.retainAll(currentPages)
        }
    }

    private class UncachedPages(override val pageCount: Int) : PageBitmaps {
        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? = null
    }

    private companion object {
        const val PAGE_COUNT = 5
    }
}
