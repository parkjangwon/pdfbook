package org.parkjw.apps.pdfbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.provider.Settings
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import org.robolectric.annotation.Config

/**
 * 장원's real-device report (T172): a fully opened foldable in PORTRAIT (about 1840x2208 px,
 * widthDp >= 600), two-page Auto, cover alone on, curl turn — every turn ran as Slide.
 *
 * Drives the real [ReaderScreen] + real [PageRenderer] cache (keys are exact
 * `(page, maxWidth, maxHeight)`) at the device window size, so a prerender/curl key mismatch
 * or a rejected [twoPageCurlMeetsAtSpine] shows up here, unlike the 40x60 fakes.
 * Synthetic pages only.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w1840dp-h2208dp-mdpi")
class TwoPageCurlFlatReaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var pagerState: PagerState
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun animationsOn() {
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    @Test
    fun portraitFlatFoldableTwoPageSpreadCurls() {
        assertCurls(ReadingDirection.LTR)
    }

    @Test
    fun portraitFlatFoldableTwoPageSpreadCurlsInRtl() {
        assertCurls(ReadingDirection.RTL)
    }

    @Test
    @Config(qualifiers = "ko-w2208dp-h1840dp-mdpi")
    fun landscapeFlatFoldableTwoPageSpreadCurls() {
        assertCurls(ReadingDirection.LTR)
    }

    @Test
    fun portraitFlatFoldableTwoPageSpreadCurlsWithAutoCrop() {
        // Each page keeps its own outer crop margin (the v0.7.1 rule), so the drawn widths can
        // differ; the spine edges still meet and every page is drawn at its own rect, so the
        // turn curls instead of sliding.
        assertCurls(ReadingDirection.LTR, autoCrop = true)
    }

    @Test
    fun portraitFlatFoldableTwoPageSpreadCurlsWithAutoCropInRtl() {
        assertCurls(ReadingDirection.RTL, autoCrop = true)
    }

    @Test
    fun portraitFlatFoldableMixedPageSizesCurl() {
        // A real PDF's pages are a few points apart. Each page is drawn at its own rect, so
        // the turn still curls and neither the first nor the last frame jumps.
        assertCurls(ReadingDirection.LTR, source = MixedSizeSource(PAGE_COUNT))
    }

    @Test
    fun portraitFlatFoldableBookPageWidthsCurl() {
        // Page widths from a real 19-page book (743..747pt, all 1080pt tall): every facing
        // pair renders at a different height, which used to force every turn to Slide.
        assertCurls(ReadingDirection.LTR, source = BookWidthSource(BOOK_PAGE_WIDTHS))
    }

    private fun assertCurls(
        direction: ReadingDirection,
        autoCrop: Boolean = false,
        source: PageSource = UniformSource(PAGE_COUNT),
    ) {
        val renderer = PageRenderer(source, limitBytes = 64L * 1024 * 1024)
        try {
            setReader(renderer, direction, autoCrop)
            awaitReady(renderer, autoCrop)
            composeRule.mainClock.autoAdvance = false
            tapNext(direction)
            composeRule.mainClock.advanceTimeBy(100)
            composeRule.onNodeWithTag("reader_page_curl").assertExists()
            composeRule.mainClock.advanceTimeBy(2_000)
            composeRule.mainClock.autoAdvance = true
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("reader_page_curl").assertDoesNotExist()
            assertEquals(2, pagerState.currentPage)
        } finally {
            renderer.close()
            runBlocking { renderer.awaitClosed() }
        }
    }

    /** Prerender and crop detection are async; wait until the neighbour spread is ready. */
    private fun awaitReady(renderer: PageRenderer, autoCrop: Boolean) {
        val window = window()
        val slot = pageRenderSlot(window, FoldPosture.Flat, ReadingDirection.LTR, twoPage = true)
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            val cached = isSpreadCached(renderer, listOf(4, 5), slot.width, slot.height) &&
                isSpreadCached(renderer, listOf(2, 3), slot.width, slot.height)
            val cropped = !autoCrop || (2..5).all { renderer.cropMargins(it) != null }
            if (cached && cropped) return
            Thread.sleep(10)
        }
        assertTrue("prerender never cached the neighbour spread", false)
    }

    private fun window(): androidx.compose.ui.unit.IntSize =
        composeRule.onRoot().fetchSemanticsNode().size.let {
            androidx.compose.ui.unit.IntSize(it.width, it.height)
        }

    private fun setReader(renderer: PageBitmaps, direction: ReadingDirection, autoCrop: Boolean) {
        val pageSpreads = spreads(PAGE_COUNT, twoPage = true, coverAlone = true)
        composeRule.setContent {
            pagerState = rememberPagerState(initialPage = 1, pageCount = { pageSpreads.size })
            PdfBookTheme(themeMode = ThemeMode.Light) {
                ReaderScreen(
                    renderer = renderer,
                    pagerState = pagerState,
                    spreads = pageSpreads,
                    title = "책 1권",
                    pageCount = PAGE_COUNT,
                    currentPage = 2,
                    twoPage = true,
                    onBack = {},
                    onDirectionChange = {},
                    direction = direction,
                    volumeDelta = emptyFlow(),
                    showTapZoneGuide = false,
                    autoCrop = autoCrop,
                    pageTurnAnimation = PageTurnAnimation.Curl,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun tapNext(direction: ReadingDirection) {
        composeRule.onRoot().performTouchInput {
            val x = if (direction == ReadingDirection.RTL) width * 0.1f else width * 0.9f
            click(Offset(x, height / 2f))
        }
    }

    /** A uniform A4-aspect scan: the bitmap is fitted into the requested box, like PdfRenderer. */
    private open class UniformSource(override val pageCount: Int) : PageSource {
        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            val scale = minOf(maxWidth.toFloat() / PAGE_WIDTH, maxHeight.toFloat() / PAGE_HEIGHT)
            return syntheticPage(
                (PAGE_WIDTH * scale).toInt().coerceAtLeast(1),
                (PAGE_HEIGHT * scale).toInt().coerceAtLeast(1),
            )
        }

        override fun close() {}
    }

    /** The same scan with a few pixels of size difference per page, as a real scan can have. */
    private class MixedSizeSource(pageCount: Int) : UniformSource(pageCount) {
        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            val bitmap = super.draw(page, maxWidth, maxHeight)
            if (page % 3 == 0) {
                val shorter = Bitmap.createBitmap(bitmap.width, bitmap.height - 4, Bitmap.Config.ARGB_8888)
                Canvas(shorter).drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle()
                return shorter
            }
            return bitmap
        }
    }

    /** Pages with the exact point widths of 장원's book; all 1080pt tall. */
    private class BookWidthSource(private val widths: IntArray) : PageSource {
        override val pageCount = widths.size

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            val pageWidth = widths[page - 1]
            val scale = minOf(maxWidth.toFloat() / pageWidth, maxHeight.toFloat() / BOOK_PAGE_HEIGHT)
            return syntheticPage(
                (pageWidth * scale).toInt().coerceAtLeast(1),
                (BOOK_PAGE_HEIGHT * scale).toInt().coerceAtLeast(1),
            )
        }

        override fun close() {}
    }

    private companion object {
        const val PAGE_COUNT = 8
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val BOOK_PAGE_HEIGHT = 1080

        /** Pages 1..8 of the book in 장원's v0.7.2 report, in points. */
        val BOOK_PAGE_WIDTHS = intArrayOf(745, 743, 746, 745, 747, 744, 746, 746)
    }
}

/** Synthetic page: white paper, a black text block and a few gray lines. No real content. */
private fun syntheticPage(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.DKGRAY
    canvas.drawRect(width * 0.15f, height * 0.1f, width * 0.85f, height * 0.2f, paint)
    paint.color = Color.LTGRAY
    for (line in 0 until 8) {
        canvas.drawRect(
            width * 0.15f,
            height * (0.26f + line * 0.06f),
            width * 0.75f,
            height * (0.26f + line * 0.06f + 0.02f),
            paint,
        )
    }
    return bitmap
}
