package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The FLAT table for T172: why a two-page turn on a fully opened foldable (posture Flat) can
 * still choose Slide. Drives the real [buildTwoPageCurl] with the window class from 장원's video
 * — 1840x2208 px portrait (5:6, widthDp >= 600) — and A4 pages rendered at the
 * `viewportWidth / 2` cache key that prerender uses.
 *
 * Cover alone on: spreads are `[1] | [2,3] | [4,5]`, so `[2,3] -> [4,5]` is a real 2->2 turn.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class TwoPageCurlFlatFoldTest {
    private val portrait = IntSize(WINDOW_WIDTH, WINDOW_HEIGHT)
    private val landscape = IntSize(WINDOW_HEIGHT, WINDOW_WIDTH)
    private val page = bitmap(PAGE_WIDTH, PAGE_HEIGHT) // A4 fitted into the half slot

    @Test
    fun portraitFlatTwoPageTurnsCurlBothWays() {
        val cases = listOf(
            Case("ltr forward", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            Case("ltr backward", listOf(4, 5), listOf(2, 3), ReadingDirection.LTR, CurlSide.Left),
            Case("rtl forward", listOf(2, 3), listOf(4, 5), ReadingDirection.RTL, CurlSide.Left),
            Case("rtl backward", listOf(4, 5), listOf(2, 3), ReadingDirection.RTL, CurlSide.Right),
        )
        cases.forEach { case ->
            val built = build(case, window = portrait)
            assertTrue("${case.name} must curl, was ${built.fallback}", built.curl != null)
        }
    }

    @Test
    fun portraitFlatAutoCropSpreadsCurlWithOneSharedOuterMargin() {
        // A real scan: both facing pages are the same size but their outer margins differ a little
        // (left 0.1402, right 0.1380). One shared outer margin keeps the halves the same size, so
        // the 1px landing rule holds instead of sliding every two-page turn.
        val crops = mapOf(
            2 to CropMargins(left = 0.1402f, top = 0.0899f, right = 0.1380f, bottom = 0.25f),
            3 to CropMargins(left = 0.1402f, top = 0.0899f, right = 0.1380f, bottom = 0.25f),
            4 to CropMargins(left = 0.1402f, top = 0.0899f, right = 0.1380f, bottom = 0.25f),
            5 to CropMargins(left = 0.1402f, top = 0.0899f, right = 0.1380f, bottom = 0.25f),
        )
        for (direction in listOf(ReadingDirection.LTR, ReadingDirection.RTL)) {
            val side = if (direction == ReadingDirection.LTR) CurlSide.Right else CurlSide.Left
            val built = build(
                Case("autoCrop $direction", listOf(2, 3), listOf(4, 5), direction, side),
                window = portrait,
                crops = crops,
                autoCrop = true,
            )
            assertTrue("autoCrop $direction must curl, was ${built.fallback}", built.curl != null)
        }
    }

    @Test
    fun landscapeFlatTwoPageTurnsCurl() {
        val built = build(
            Case("landscape", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            window = landscape,
        )
        assertTrue("landscape must curl, was ${built.fallback}", built.curl != null)
    }

    @Test
    fun oddWindowWidthStillHitsThePrerenderKey() {
        // 1841 / 2 = 920 in both spreadRequests() and the curl lookup.
        val built = build(
            Case("odd width", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            window = IntSize(WINDOW_WIDTH + 1, WINDOW_HEIGHT),
            keys = setOf(
                Triple(2, 920, WINDOW_HEIGHT), Triple(3, 920, WINDOW_HEIGHT),
                Triple(4, 920, WINDOW_HEIGHT), Triple(5, 920, WINDOW_HEIGHT),
            ),
        )
        assertTrue("odd width must curl, was ${built.fallback}", built.curl != null)
    }

    @Test
    fun aCacheKeyPrerenderNeverUsedFallsBackToSlide() {
        // If the cache only holds the page at another width (a half slot), the turn cannot start.
        val built = build(
            Case("cache miss", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            keys = setOf(Triple(4, 916, WINDOW_HEIGHT), Triple(5, 916, WINDOW_HEIGHT)),
        )
        assertNull(built.curl)
        assertEquals(TwoPageCurlFallback.CacheMiss, built.fallback)
    }

    @Test
    fun pagesDrawnAtDifferentSizesFallBackToSlide() {
        // Documented fallback (V071-TWOPAGE.md §4): a page 4 px shorter cannot land exactly on the
        // settled target page, so the turn slides instead of jumping at the end.
        val shorter = bitmap(PAGE_WIDTH, PAGE_HEIGHT - 4)
        val built = build(
            Case("mixed sizes", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            extra = mapOf(3 to shorter),
        )
        assertNull(built.curl)
        assertEquals(TwoPageCurlFallback.SheetLands, built.fallback)
    }

    @Test
    fun singlePageSpreadsDoNotBuildATwoPageCurl() {
        val cover = build(
            Case("cover", listOf(1), listOf(2, 3), ReadingDirection.LTR, CurlSide.Right),
        )
        assertEquals(TwoPageCurlFallback.SpreadPages, cover.fallback)
        val empty = build(
            Case("empty window", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            window = IntSize(0, 0),
        )
        assertEquals(TwoPageCurlFallback.NoCanvas, empty.fallback)
    }

    @Test
    fun bookPostureStillSlidesOnARealHingeGap() {
        // #172: the hinge gap keeps the Slide fallback until the hinge-cut curl lands.
        val gap = build(
            Case("book gap", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            posture = FoldPosture.Book(
                IntRect(WINDOW_WIDTH / 2 - 4, 0, WINDOW_WIDTH / 2 + 4, WINDOW_HEIGHT),
            ),
        )
        assertNull(gap.curl)
        assertEquals(TwoPageCurlFallback.SheetLands, gap.fallback)
    }

    private fun build(
        case: Case,
        window: IntSize = portrait,
        keys: Set<Triple<Int, Int, Int>>? = null,
        crops: Map<Int, CropMargins> = emptyMap(),
        autoCrop: Boolean = false,
        extra: Map<Int, Bitmap> = emptyMap(),
        posture: FoldPosture = FoldPosture.Flat,
    ): TwoPageCurlBuild {
        val pages = (case.current + case.target).associateWith { page } + extra
        val renderer = FakeRenderer(pages, keys, crops, autoCrop)
        return buildTwoPageCurl(
            renderer = renderer,
            currentSpread = case.current,
            targetSpread = case.target,
            direction = case.direction,
            side = case.side,
            posture = posture,
            window = window,
            autoCrop = autoCrop,
        )
    }

    private data class Case(
        val name: String,
        val current: List<Int>,
        val target: List<Int>,
        val direction: ReadingDirection,
        val side: CurlSide,
    )

    private class FakeRenderer(
        private val pages: Map<Int, Bitmap>,
        private val keys: Set<Triple<Int, Int, Int>>?,
        private val crops: Map<Int, CropMargins>,
        private val autoCrop: Boolean,
    ) : PageBitmaps {
        override val pageCount = 100

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            cached(page, maxWidth, maxHeight)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            pages[page]?.takeIf { keys == null || Triple(page, maxWidth, maxHeight) in keys }

        override fun cropMargins(page: Int): CropMargins? = if (autoCrop) crops[page] else null
    }

    private fun bitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private companion object {
        const val WINDOW_WIDTH = 1840
        const val WINDOW_HEIGHT = 2208
        const val PAGE_WIDTH = 920
        const val PAGE_HEIGHT = 1301
    }
}
