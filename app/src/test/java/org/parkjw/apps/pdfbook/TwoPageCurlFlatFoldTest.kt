package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
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
    fun portraitFlatAutoCropSpreadsCurlWithUnequalOuterMargins() {
        // A real scan: both facing pages are the same size but their outer margins differ a little
        // (left 0.1402, right 0.1380). Each page keeps its own outer margin (the v0.7.1 rule), so
        // the drawn halves differ in width; the spine edges still meet, so the turn curls.
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
    fun pagesDrawnAtDifferentSizesStillCurl() {
        // A page 4 px shorter is drawn at its own rect and the revealed page at its own, so
        // both the first and the last frame match the settled spreads: no jump, no Slide.
        val shorter = bitmap(PAGE_WIDTH, PAGE_HEIGHT - 4)
        val built = build(
            Case("mixed sizes", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            extra = mapOf(3 to shorter),
        )
        assertTrue("mixed sizes must curl, was ${built.fallback}", built.curl != null)
    }

    @Test
    fun everyTurnOfARealBookWithPageWidthsOffByAFewPointsCurls() {
        // 장원's book: 19 pages, all 1080pt tall, widths 743..747pt (1-4pt apart). PdfRenderer
        // renders each page at the width-limited half slot, so the heights differ by a few px.
        // Every two-page turn in that book has to curl, forward and backward, not only the ones
        // whose facing pages happen to render at the same size.
        val renderer = BookRenderer(BOOK_PAGE_WIDTHS)
        val bookSpreads = spreads(BOOK_PAGE_WIDTHS.size, twoPage = true, coverAlone = true)
        for (window in listOf(portrait, landscape)) {
            for (direction in listOf(ReadingDirection.LTR, ReadingDirection.RTL)) {
                for (forward in listOf(true, false)) {
                    val side = curlSide(direction, forward)
                    for (i in 1 until bookSpreads.size - 1) {
                        val from = if (forward) bookSpreads[i] else bookSpreads[i + 1]
                        val to = if (forward) bookSpreads[i + 1] else bookSpreads[i]
                        if (from.size != 2 || to.size != 2) continue
                        val built = buildTwoPageCurl(
                            renderer = renderer,
                            currentSpread = from,
                            targetSpread = to,
                            direction = direction,
                            side = side,
                            posture = FoldPosture.Flat,
                            window = window,
                            autoCrop = false,
                        )
                        assertTrue(
                            "$from -> $to $direction ${window.width}x${window.height} " +
                                "forward=$forward must curl, was ${built.fallback}",
                            built.curl != null,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun mixedHeightPairLandsEachPageOnItsOwnRectAtBothEnds() {
        // [6,7] -> [8,9] in 장원's book: the turning page renders 4px taller than the back face
        // and 3px taller than the revealed page. The fold runs over the union of the four rects
        // and every page is clipped to its own rect, so p=1 equals the settled target spread
        // (no flap-color hem, no clipped rows) and p=0 equals the current spread.
        val renderer = BookRenderer(BOOK_PAGE_WIDTHS)
        val bookSpreads = spreads(BOOK_PAGE_WIDTHS.size, twoPage = true, coverAlone = true)
        val from = bookSpreads.first { it == listOf(6, 7) }
        val to = bookSpreads.first { it == listOf(8, 9) }
        var sawUnevenRect = false
        for (window in listOf(portrait, landscape)) {
            for (direction in listOf(ReadingDirection.LTR, ReadingDirection.RTL)) {
                for (forward in listOf(true, false)) {
                    val side = curlSide(direction, forward)
                    val built = buildTwoPageCurl(
                        renderer = renderer,
                        currentSpread = if (forward) from else to,
                        targetSpread = if (forward) to else from,
                        direction = direction,
                        side = side,
                        posture = FoldPosture.Flat,
                        window = window,
                        autoCrop = false,
                    )
                    val curl = built.curl ?: error("$from -> $to $direction must curl: ${built.fallback}")
                    val layout = curl.layout
                    if (layout.backFace.height != layout.sheet.height ||
                        layout.landing.height != layout.sheet.height
                    ) {
                        sawUnevenRect = true
                    }
                    assertTwoPageEndFrame(layout, curl.front, side)
                }
            }
        }
        assertTrue("the pair must exercise unequal page heights", sawUnevenRect)
    }

    /**
     * Rebuilds the overlay's fold bounds (union of the four page rects, in the sheet's frame) and
     * asserts the end frames: at p=1 the flap covers the whole back face and the revealed polygon
     * covers the whole landing page; at p=0 the front polygon covers the sheet. The back-face
     * matrix must be the identity at p=1, so the back face is drawn exactly at its own rect.
     */
    private fun assertTwoPageEndFrame(layout: CurlSheetLayout, front: Bitmap, side: CurlSide) {
        val sheet = layout.sheet
        val scale = minOf(sheet.width / front.width, sheet.height / front.height)
        val w = front.width * scale
        val h = front.height * scale
        val left = sheet.left + (sheet.width - w) / 2f
        val top = sheet.top + (sheet.height - h) / 2f
        val union = curlFoldUnion(layout)
        val bounds = floatArrayOf(
            union.left - left,
            union.top - top,
            union.right - left,
            union.bottom - top,
        )
        val shape = CurlShape()
        updateCurlShape(w, h, 1f, side, shape, spreadFlap = true, bounds = bounds)
        assertContains(
            shape.flap,
            shape.flapCount,
            layout.backFace,
            left,
            top,
            "flap at p=1 must cover the back face",
        )
        assertContains(
            shape.revealed,
            shape.revealedCount,
            layout.landing,
            left,
            top,
            "revealed page at p=1 must cover the landing page",
        )
        val matrix = FloatArray(9)
        flapBackMatrixValues(shape, left, top, w, side, matrix)
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        for (i in identity.indices) {
            assertEquals("back-face matrix[$i] at p=1", identity[i], matrix[i], 0.001f)
        }

        updateCurlShape(w, h, 0f, side, shape, spreadFlap = true, bounds = bounds)
        assertContains(
            shape.front,
            shape.frontCount,
            sheet,
            left,
            top,
            "front at p=0 must cover the sheet",
        )
    }

    private fun assertContains(
        polygon: FloatArray,
        count: Int,
        rect: Rect,
        left: Float,
        top: Float,
        message: String,
    ) {
        assertTrue("$message: polygon had $count points", count >= 3)
        // Probe just inside each corner so an edge-exact point cannot be misread.
        val inset = 0.5f
        val probes = listOf(
            rect.left + inset to rect.top + inset,
            rect.right - inset to rect.top + inset,
            rect.right - inset to rect.bottom - inset,
            rect.left + inset to rect.bottom - inset,
        )
        probes.forEach { (x, y) ->
            assertTrue(
                "$message: (${x.toInt()}, ${y.toInt()}) is outside the polygon",
                polygonContains(polygon, count, x - left, y - top),
            )
        }
    }

    private fun polygonContains(points: FloatArray, count: Int, x: Float, y: Float): Boolean {
        var inside = false
        var j = count - 1
        for (i in 0 until count) {
            val xi = points[i * 2]
            val yi = points[i * 2 + 1]
            val xj = points[j * 2]
            val yj = points[j * 2 + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside
            }
            j = i
        }
        return inside
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
    fun bookPostureHingeGapCurlsOnTheVirtualSpread() {
        // #172: a real hinge gap now curls with the hinge cut instead of sliding. The layout is
        // measured on the virtual spread (halves butted together) and carries the gap.
        val fold = FoldPosture.Book(
            IntRect(WINDOW_WIDTH / 2 - 8, 0, WINDOW_WIDTH / 2 + 8, WINDOW_HEIGHT),
        )
        val cases = listOf(
            Case("ltr forward", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            Case("ltr backward", listOf(4, 5), listOf(2, 3), ReadingDirection.LTR, CurlSide.Left),
            Case("rtl forward", listOf(2, 3), listOf(4, 5), ReadingDirection.RTL, CurlSide.Left),
            Case("rtl backward", listOf(4, 5), listOf(2, 3), ReadingDirection.RTL, CurlSide.Right),
        )
        cases.forEach { case ->
            val built = build(case, posture = fold)
            val curl = built.curl
            assertTrue("${case.name} must curl, was ${built.fallback}", curl != null)
            val layout = curl!!.layout
            val hinge = layout.hinge!!
            assertEquals("${case.name} hinge gap", 16f, hinge.gap, 0.001f)
            // The virtual spread is the two halves butted: nothing reaches past its width.
            val virtualWidth = WINDOW_WIDTH - 16f
            assertTrue("${case.name} sheet inside the spread", layout.sheet.right <= virtualWidth + 1f)
            assertTrue("${case.name} back face inside the spread", layout.backFace.right <= virtualWidth + 1f)
            assertTrue("${case.name} landing inside the spread", layout.landing.right <= virtualWidth + 1f)
            // The cut is on the virtual spread: the spine is the left half's width and, shifted
            // back by the gap, it is exactly the fold's right edge where BookHalfPage starts the
            // right page. So the virtual rects land on the real half rects, no jump.
            val halves = bookHalves(portrait, fold.foldBounds)
            assertEquals("${case.name} spine is the left half", halves.first.width.toFloat(), layout.spineX, 0.001f)
            assertEquals(
                "${case.name} spine + gap is the fold's right edge",
                fold.foldBounds.right.toFloat(),
                layout.spineX + hinge.gap,
                0.001f,
            )
            val rects = listOf(
                "sheet" to layout.sheet,
                "landing" to layout.landing,
                "back face" to layout.backFace,
                "still" to layout.still,
            )
            rects.forEach { (name, rect) ->
                // Every page's inner edge lies on the spine, half a turn apart on screen.
                val inner = minOf(abs(rect.left - layout.spineX), abs(rect.right - layout.spineX))
                assertTrue("${case.name} $name inner edge $inner off the spine", inner <= 1f)
            }
            val rightRects = if (case.side == CurlSide.Right) {
                listOf("sheet" to layout.sheet, "landing" to layout.landing)
            } else {
                listOf("back face" to layout.backFace, "still" to layout.still)
            }
            rightRects.forEach { (name, rect) ->
                assertEquals(
                    "${case.name} $name left + gap must be the right half",
                    fold.foldBounds.right.toFloat(),
                    rect.left + hinge.gap,
                    1f,
                )
            }
        }
    }

    @Test
    fun aOnePixelGapKeepsTheFlushGeometry() {
        // Not a real hinge gap: the 1px landing rule already holds, so it stays on the old path
        // and is not cut (there is nothing to show through a 1px seam).
        val fold = FoldPosture.Book(IntRect(WINDOW_WIDTH / 2, 0, WINDOW_WIDTH / 2 + 1, WINDOW_HEIGHT))
        val built = build(
            Case("book 1px", listOf(2, 3), listOf(4, 5), ReadingDirection.LTR, CurlSide.Right),
            posture = fold,
        )
        val curl = built.curl
        assertTrue("1px gap must still curl on the flush path, was ${built.fallback}", curl != null)
        assertNull("1px gap must not be cut", curl!!.layout.hinge)
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

    /** Renders like [PdfPageSource.draw]: each page keeps its own aspect inside the box. */
    private class BookRenderer(private val widths: IntArray) : PageBitmaps {
        override val pageCount = widths.size

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            cached(page, maxWidth, maxHeight)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
            val pageWidth = widths.getOrNull(page - 1) ?: return null
            val scale = minOf(
                maxWidth.toFloat() / pageWidth,
                maxHeight.toFloat() / BOOK_PAGE_HEIGHT,
            )
            return Bitmap.createBitmap(
                (pageWidth * scale).toInt().coerceAtLeast(1),
                (BOOK_PAGE_HEIGHT * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
        }
    }

    private fun bitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private companion object {
        const val WINDOW_WIDTH = 1840
        const val WINDOW_HEIGHT = 2208
        const val PAGE_WIDTH = 920
        const val PAGE_HEIGHT = 1301
        const val BOOK_PAGE_HEIGHT = 1080

        /** Pages 1..19 of the book in 장원's v0.7.2 report, in points. */
        val BOOK_PAGE_WIDTHS = intArrayOf(
            745, 743, 746, 743, 746, 746, 743, 747, 745, 745,
            744, 743, 746, 746, 745, 745, 745, 744, 745,
        )
    }
}
