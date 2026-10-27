package org.parkjw.apps.pdfbook

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation

class PageCurlTest {
    @Test
    fun directionMappingRtlLtrForwardBack() {
        assertEquals(CurlSide.Left, curlSide(ReadingDirection.RTL, forward = true))
        assertEquals(CurlSide.Right, curlSide(ReadingDirection.RTL, forward = false))
        assertEquals(CurlSide.Right, curlSide(ReadingDirection.LTR, forward = true))
        assertEquals(CurlSide.Left, curlSide(ReadingDirection.LTR, forward = false))
    }

    @Test
    fun progressFromDragAndReleaseThreshold() {
        assertEquals(0.5f, curlProgressFromDrag(100f, 200f, CurlSide.Left), 0.001f)
        assertEquals(0.5f, curlProgressFromDrag(-100f, 200f, CurlSide.Right), 0.001f)
        assertEquals(0f, curlProgressFromDrag(-50f, 200f, CurlSide.Left), 0.001f)
        assertEquals(1f, curlProgressFromDrag(400f, 200f, CurlSide.Left), 0.001f)
        assertFalse(shouldCompleteCurl(CURL_RELEASE_THRESHOLD - 0.01f))
        assertTrue(shouldCompleteCurl(CURL_RELEASE_THRESHOLD))
        assertTrue(shouldCompleteCurl(1f))
    }

    @Test
    fun twoPageDragUsesTheSpreadWidthNotThePane() {
        // A 2400px pane with two 763px pages: the drawn spread is 1526px, not the pane.
        val pane = 2400f
        val spread = 1526f
        val completes = { progress: Float ->
            curlReleaseCompletes(progress, alongVelocityPx = 0f, flingVelocityPx = 1000f)
        }
        val quarter = curlProgressFromDrag(-spread * 0.25f, spread, CurlSide.Right)
        assertEquals(0.25f, quarter, 0.001f)
        assertTrue(completes(quarter))
        assertFalse(completes(curlProgressFromDrag(-spread * 0.24f, spread, CurlSide.Right)))
        // The same finger travel against the pane width is only a 16% turn: it springs back.
        assertFalse(completes(curlProgressFromDrag(-spread * 0.25f, pane, CurlSide.Right)))
    }

    @Test
    fun geometryBuildsForMidProgress() {
        val geo = curlGeometry(400f, 600f, 0.4f, CurlSide.Right)
        assertNotNull(geo)
        assertTrue(geo!!.tip.x < 400f)
        assertNull(curlGeometry(400f, 600f, 0f, CurlSide.Left))
    }

    @Test
    fun frontPageShrinksMonotonicallyAndVanishesAtTheEnd() {
        val aspects = listOf(400f to 600f, 360f to 640f, 400f to 566f, 500f to 500f)
        for ((width, height) in aspects) {
            for (side in listOf(CurlSide.Left, CurlSide.Right)) {
                assertEquals(1f, curlFrontVisibleFraction(width, height, 0f, side), 0.001f)
                var previous = 1.01f
                var progress = 0f
                while (progress <= 1f) {
                    val fraction = curlFrontVisibleFraction(width, height, progress, side)
                    assertTrue(
                        "fraction rose at p=$progress ($fraction > $previous), $side $width x $height",
                        fraction <= previous + 1e-4f,
                    )
                    previous = fraction
                    progress += 0.01f
                }
                assertTrue(
                    "still visible at 0.99: ${curlFrontVisibleFraction(width, height, 0.99f, side)}",
                    curlFrontVisibleFraction(width, height, 0.99f, side) < 0.02f,
                )
                assertEquals(0f, curlFrontVisibleFraction(width, height, 1f, side), 0.001f)
            }
        }
    }

    @Test
    fun frontPageMatchesTheMockupFrames() {
        // Values from V070-MOCKUPS "접힌 면" for a portrait page.
        val width = 400f
        val height = 600f
        assertEquals(0.65f, curlFrontVisibleFraction(width, height, 0.5f, CurlSide.Right), 0.02f)
        assertEquals(0.26f, curlFrontVisibleFraction(width, height, 0.8f, CurlSide.Right), 0.02f)
        assertEquals(0.13f, curlFrontVisibleFraction(width, height, 0.9f, CurlSide.Right), 0.01f)
        assertEquals(0.06f, curlFrontVisibleFraction(width, height, 0.95f, CurlSide.Right), 0.01f)
        assertEquals(0.013f, curlFrontVisibleFraction(width, height, 0.99f, CurlSide.Right), 0.005f)
    }

    @Test
    fun foldLineReachesTheFarEdgeAtTheEnd() {
        val rightEnd = curlGeometry(400f, 600f, 1f, CurlSide.Right)!!
        assertTrue(rightEnd.foldTop.x <= 1f)
        assertTrue(rightEnd.foldBottom.x <= 1f)
        val leftEnd = curlGeometry(400f, 600f, 1f, CurlSide.Left)!!
        assertTrue(leftEnd.foldTop.x >= 399f)
        assertTrue(leftEnd.foldBottom.x >= 399f)

        val rightNear = curlGeometry(400f, 600f, 0.99f, CurlSide.Right)!!
        assertTrue(rightNear.foldTop.x < 20f)
        assertTrue(rightNear.foldBottom.x < 20f)
        val leftNear = curlGeometry(400f, 600f, 0.99f, CurlSide.Left)!!
        assertTrue(leftNear.foldTop.x > 380f)
        assertTrue(leftNear.foldBottom.x > 380f)

        val endLeft = curlTip(400f, 600f, 1f, CurlSide.Left)
        assertEquals(800f, endLeft.x, 0.01f)
        assertEquals(600f, endLeft.y, 0.01f)
        val endRight = curlTip(400f, 600f, 1f, CurlSide.Right)
        assertEquals(-400f, endRight.x, 0.01f)
        assertEquals(600f, endRight.y, 0.01f)
    }

    @Test
    fun flapIsTheReflectedPeeledPartInsideThePage() {
        val width = 400f
        val height = 600f
        for (side in listOf(CurlSide.Left, CurlSide.Right)) {
            val shape = CurlShape()
            updateCurlShape(width, height, 0.5f, side, shape)
            assertTrue(shape.flapCount >= 3)
            assertTrue(polygonArea(shape.flap, shape.flapCount) > 1000f)
            for (i in 0 until shape.flapCount) {
                val x = shape.flap[i * 2]
                val y = shape.flap[i * 2 + 1]
                assertTrue("flap x outside page: $x", x >= -0.01f && x <= width + 0.01f)
                assertTrue("flap y outside page: $y", y >= -0.01f && y <= height + 0.01f)
            }
            val end = CurlShape()
            updateCurlShape(width, height, 1f, side, end)
            assertEquals(0f, polygonArea(end.flap, end.flapCount), 0.5f)
            assertEquals(0f, polygonArea(end.front, end.frontCount), 0.5f)
        }
    }

    @Test
    fun flapBackMatrixTurnsTheBackFaceIntoTheStillSpread() {
        val width = 400f
        val height = 600f
        val left = 120f
        val top = 40f
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        for (side in listOf(CurlSide.Left, CurlSide.Right)) {
            val shape = CurlShape()
            updateCurlShape(width, height, 1f, side, shape, spreadFlap = true)
            val values = FloatArray(9)
            flapBackMatrixValues(shape, left, top, width, side, values)
            for (i in identity.indices) {
                assertEquals("M[$i] at p=1, $side", identity[i], values[i], 1e-4f)
            }

            // At the half turn the tip sits on the spine and no flap has crossed it yet.
            val half = CurlShape()
            updateCurlShape(width, height, 0.5f, side, half, spreadFlap = true)
            val spine = if (side == CurlSide.Right) 0f else width
            assertEquals("tip at the spine, $side", spine, half.tipX, 0.01f)
            val box = boundsOf(half.flap, half.flapCount)
            val past = if (side == CurlSide.Right) box[0] else width - box[1]
            assertEquals("flap past the spine at p=0.5, $side", 0f, past, 0.01f)
        }
    }

    @Test
    fun releaseCompletesByDistanceOrFlingOnly() {
        // Slow drags: a quarter turn from the mockup completes.
        assertTrue(curlReleaseCompletes(0.25f, alongVelocityPx = 0f, flingVelocityPx = 1000f))
        assertFalse(curlReleaseCompletes(0.24f, alongVelocityPx = 0f, flingVelocityPx = 1000f))
        // A short fast fling completes at 1000 dp/s and not below it.
        assertTrue(curlReleaseCompletes(0.1f, alongVelocityPx = 1000f, flingVelocityPx = 1000f))
        assertFalse(curlReleaseCompletes(0.1f, alongVelocityPx = 999f, flingVelocityPx = 1000f))
        // A fast push back toward the current page reverts.
        assertFalse(curlReleaseCompletes(0.1f, alongVelocityPx = -5000f, flingVelocityPx = 1000f))
        // Distance rule for the slide/instant paths stays at 33%.
        assertFalse(shouldCompleteCurl(0.32f))
        assertTrue(shouldCompleteCurl(0.33f))
    }

    @Test
    fun velocityTrackerOfAQuickDragIsAboveTheFlingThreshold() {
        val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
        tracker.addPosition(0L, androidx.compose.ui.geometry.Offset(0f, 0f))
        tracker.addPosition(16L, androidx.compose.ui.geometry.Offset(-25f, 0f))
        tracker.addPosition(32L, androidx.compose.ui.geometry.Offset(-50f, 0f))
        tracker.addPosition(48L, androidx.compose.ui.geometry.Offset(-75f, 0f))
        val velocity = tracker.calculateVelocity().x
        assertTrue("velocity was $velocity", velocity <= -CURL_FLING_DP_PER_SECOND)
    }

    @Test
    fun flingVelocityMapsToTheTurnDirection() {
        assertEquals(1500f, curlAlongVelocity(1500f, CurlSide.Left), 0.001f)
        assertEquals(1500f, curlAlongVelocity(-1500f, CurlSide.Right), 0.001f)
        assertEquals(-1500f, curlAlongVelocity(-1500f, CurlSide.Left), 0.001f)
        assertEquals(-1500f, curlAlongVelocity(1500f, CurlSide.Right), 0.001f)
    }

    @Test
    fun shadowWidthShrinksOverTheLastTwentyPercent() {
        assertEquals(1f, curlShadowWidthFraction(0f), 0.001f)
        assertEquals(1f, curlShadowWidthFraction(0.8f), 0.001f)
        assertEquals(0.5f, curlShadowWidthFraction(0.9f), 0.001f)
        assertEquals(0f, curlShadowWidthFraction(1f), 0.001f)
    }

    @Test
    fun secondPointerSettlesToTheNearestState() {
        assertFalse(curlSettleCompletes(0f))
        assertFalse(curlSettleCompletes(0.49f))
        assertTrue(curlSettleCompletes(0.5f))
        assertTrue(curlSettleCompletes(1f))
        // Nearest-state differs from the distance rule for a half-turned page.
        assertFalse(curlSettleCompletes(0.35f))
        assertTrue(shouldCompleteCurl(0.35f))
    }

    @Test
    fun contentCropDefaultsToTheWholeBitmapAndClamps() {
        assertEquals(PixelCrop(0, 0, 400, 600), sourceCropPixels(400, 600, null))
        val cropped = sourceCropPixels(400, 600, Rect(0.1f, 0.2f, 0.9f, 0.8f))
        assertEquals(40, cropped.left)
        assertEquals(120, cropped.top)
        assertEquals(320, cropped.width)
        assertEquals(360, cropped.height)
        val clamped = sourceCropPixels(400, 600, Rect(-0.5f, -0.5f, 2f, 2f))
        assertEquals(PixelCrop(0, 0, 400, 600), clamped)
        val empty = sourceCropPixels(400, 600, Rect(0.5f, 0.5f, 0.5f, 0.5f))
        assertEquals(200, empty.left)
        assertEquals(300, empty.top)
        assertEquals(1, empty.width)
        assertEquals(1, empty.height)
        assertEquals(PixelCrop(0, 0, 0, 0), sourceCropPixels(0, 10, null))
    }

    @Test
    fun curlOverlayUsesTheSingleBookHalf() {
        val window = IntSize(800, 600)
        val book = FoldPosture.Book(IntRect(390, 0, 410, 600))
        assertEquals(
            IntRect(0, 0, 390, 600),
            curlOverlayBounds(window, book, ReadingDirection.LTR, twoPage = false),
        )
        assertEquals(
            IntRect(410, 0, 800, 600),
            curlOverlayBounds(window, book, ReadingDirection.RTL, twoPage = false),
        )
        assertNull(curlOverlayBounds(window, book, ReadingDirection.LTR, twoPage = true))
        assertNull(curlOverlayBounds(window, FoldPosture.Flat, ReadingDirection.LTR, twoPage = false))
        val tabletop = FoldPosture.Tabletop(IntRect(0, 280, 800, 320))
        assertNull(curlOverlayBounds(window, tabletop, ReadingDirection.LTR, twoPage = false))
    }

    @Test
    fun singlePageFlapKeepsTheV070Geometry() {
        val width = 400f
        val height = 600f
        val shape = CurlShape()
        updateCurlShape(width, height, 0.4f, CurlSide.Right, shape)
        assertEquals(80f, shape.tipX, 0.01f)
        assertEquals(497.29f, shape.tipY, 0.02f)
        assertEquals(400f, shape.foldTopX, 0.01f)
        assertEquals(50.18f, shape.foldTopY, 0.05f)
        assertEquals(223.52f, shape.foldBottomX, 0.05f)
        assertEquals(600f, shape.foldBottomY, 0.01f)
        assertEquals(0.7978f, polygonArea(shape.front, shape.frontCount) / (width * height), 0.001f)
        assertEquals(0.2022f, polygonArea(shape.flap, shape.flapCount) / (width * height), 0.001f)

        val explicit = CurlShape()
        updateCurlShape(width, height, 0.4f, CurlSide.Right, explicit, spreadFlap = false)
        assertArrayEquals(shape.flap, explicit.flap, 0f)
        assertArrayEquals(shape.front, explicit.front, 0f)
        assertArrayEquals(shape.revealed, explicit.revealed, 0f)
    }

    @Test
    fun singlePageFlapNeverLeavesTheSheet() {
        val width = 400f
        val height = 600f
        for (side in listOf(CurlSide.Left, CurlSide.Right)) {
            var p = 0f
            while (p <= 1f) {
                val shape = CurlShape()
                updateCurlShape(width, height, p, side, shape)
                assertInside(shape.flap, shape.flapCount, 0f, 0f, width, height, "sheet p=$p $side")
                p += 0.01f
            }
        }
    }

    @Test
    fun spreadFlapCoversTheOppositePageAtTheEnd() {
        val width = 400f
        val height = 600f
        for (side in listOf(CurlSide.Left, CurlSide.Right)) {
            val shape = CurlShape()
            updateCurlShape(width, height, 1f, side, shape, spreadFlap = true)
            assertEquals(
                "flap area $side",
                width * height,
                polygonArea(shape.flap, shape.flapCount),
                1f,
            )
            val left = if (side == CurlSide.Right) -width else width
            val right = if (side == CurlSide.Right) 0f else 2f * width
            assertInside(shape.flap, shape.flapCount, left, 0f, right, height, "opposite page $side")
        }
    }

    @Test
    fun spreadFlapStaysInsideTheSpreadAndGrowsWithProgress() {
        val width = 400f
        val height = 600f
        for (side in listOf(CurlSide.Left, CurlSide.Right)) {
            val left = if (side == CurlSide.Right) -width else 0f
            val right = if (side == CurlSide.Right) width else 2f * width
            var previous = 0f
            var p = 0f
            while (p <= 1f) {
                val sheet = CurlShape()
                updateCurlShape(width, height, p, side, sheet)
                val spread = CurlShape()
                updateCurlShape(width, height, p, side, spread, spreadFlap = true)
                val spreadArea = polygonArea(spread.flap, spread.flapCount)
                assertInside(spread.flap, spread.flapCount, left, 0f, right, height, "spread p=$p $side")
                assertTrue(
                    "spread flap shrank at p=$p $side ($spreadArea < $previous)",
                    spreadArea >= previous - 0.5f,
                )
                assertTrue(
                    "spread flap smaller than the sheet flap at p=$p $side",
                    spreadArea >= polygonArea(sheet.flap, sheet.flapCount) - 0.5f,
                )
                previous = spreadArea
                p += 0.01f
            }
            assertEquals(width * height, previous, 1f)
        }
    }

    @Test
    fun leftAndRightSpreadFlapsAreMirrorImages() {
        val width = 400f
        val height = 600f
        for (p in listOf(0.25f, 0.5f, 0.75f, 1f)) {
            val right = CurlShape()
            updateCurlShape(width, height, p, CurlSide.Right, right, spreadFlap = true)
            val left = CurlShape()
            updateCurlShape(width, height, p, CurlSide.Left, left, spreadFlap = true)
            assertEquals(
                "flap area p=$p",
                polygonArea(right.flap, right.flapCount),
                polygonArea(left.flap, left.flapCount),
                0.5f,
            )
            val rightBox = boundsOf(right.flap, right.flapCount)
            val leftBox = boundsOf(left.flap, left.flapCount)
            assertEquals("mirrored left edge p=$p", width - rightBox[1], leftBox[0], 0.5f)
            assertEquals("mirrored right edge p=$p", width - rightBox[0], leftBox[1], 0.5f)
            assertEquals("top edge p=$p", rightBox[2], leftBox[2], 0.5f)
            assertEquals("bottom edge p=$p", rightBox[3], leftBox[3], 0.5f)
        }
    }

    @Test
    fun spreadRectsMirrorForLtrRtlAndKeepTheHingeUncovered() {
        val window = IntSize(800, 600)
        val book = FoldPosture.Book(IntRect(390, 0, 410, 600))
        val forwardLtr = curlSpreadRects(window, book, ReadingDirection.LTR, curlSide(ReadingDirection.LTR, true), 2)!!
        assertEquals(IntRect(410, 0, 800, 600), forwardLtr.sheet)
        assertEquals(IntRect(0, 0, 800, 600), forwardLtr.spread)
        assertEquals(IntRect(390, 0, 410, 600), forwardLtr.gap)

        val forwardRtl = curlSpreadRects(window, book, ReadingDirection.RTL, curlSide(ReadingDirection.RTL, true), 2)!!
        assertEquals(IntRect(0, 0, 390, 600), forwardRtl.sheet)
        assertEquals(forwardLtr.spread, forwardRtl.spread)
        assertEquals(forwardLtr.gap, forwardRtl.gap)

        val backwardLtr = curlSpreadRects(window, book, ReadingDirection.LTR, curlSide(ReadingDirection.LTR, false), 2)!!
        assertEquals(forwardRtl.sheet, backwardLtr.sheet)
    }

    @Test
    fun onePageSpreadUsesTheSingleBookHalf() {
        val window = IntSize(800, 600)
        val book = FoldPosture.Book(IntRect(390, 0, 410, 600))
        val ltr = curlSpreadRects(window, book, ReadingDirection.LTR, CurlSide.Right, 1)!!
        assertEquals(IntRect(0, 0, 390, 600), ltr.sheet)
        assertEquals(ltr.sheet, ltr.spread)
        assertNull(ltr.gap)

        val rtl = curlSpreadRects(window, book, ReadingDirection.RTL, CurlSide.Left, 1)!!
        assertEquals(IntRect(410, 0, 800, 600), rtl.sheet)
        assertEquals(rtl.sheet, rtl.spread)
    }

    @Test
    fun tabletopAndFlatSpreadsUseTheirPageSlot() {
        val window = IntSize(800, 600)
        val tabletop = FoldPosture.Tabletop(IntRect(0, 280, 800, 320))
        val slot = tabletopHalves(window, tabletop.foldBounds).first
        val tableRects = curlSpreadRects(window, tabletop, ReadingDirection.LTR, CurlSide.Right, 2)!!
        assertEquals(slot, tableRects.spread)
        assertEquals(IntRect(400, 0, 800, 280), tableRects.sheet)
        assertNull(tableRects.gap)
        val tableSingle = curlSpreadRects(window, tabletop, ReadingDirection.LTR, CurlSide.Left, 1)!!
        assertEquals(slot, tableSingle.sheet)
        assertEquals(slot, tableSingle.spread)

        val flat = curlSpreadRects(window, FoldPosture.Flat, ReadingDirection.LTR, CurlSide.Left, 2)!!
        assertEquals(IntRect(0, 0, 800, 600), flat.spread)
        assertEquals(IntRect(0, 0, 400, 600), flat.sheet)
        assertNull(flat.gap)
        assertNull(curlSpreadRects(window, FoldPosture.Flat, ReadingDirection.LTR, CurlSide.Left, 0))
    }

    private fun assertInside(
        points: FloatArray,
        count: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        label: String,
    ) {
        if (count < 3) return
        for (i in 0 until count) {
            val x = points[i * 2]
            val y = points[i * 2 + 1]
            assertTrue("$label: x $x outside $left..$right", x >= left - 0.01f && x <= right + 0.01f)
            assertTrue("$label: y $y outside $top..$bottom", y >= top - 0.01f && y <= bottom + 0.01f)
        }
    }

    private fun boundsOf(points: FloatArray, count: Int): FloatArray {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until count) {
            val x = points[i * 2]
            val y = points[i * 2 + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        return floatArrayOf(minX, maxX, minY, maxY)
    }

    @Test
    fun fittedPageRectCentersContent() {
        val fit = fittedPageRect(100, 200, 400f, 400f)!!
        assertEquals(200f, fit.width, 0.01f)
        assertEquals(400f, fit.height, 0.01f)
        assertEquals(100f, fit.left, 0.01f)
        assertEquals(0f, fit.top, 0.01f)
        assertNull(fittedPageRect(0, 10, 100f, 100f))
    }

    @Test
    fun fallbackDecision() {
        fun mode(
            preference: PageTurnAnimation = PageTurnAnimation.Curl,
            twoPage: Boolean = false,
            zoomed: Boolean = false,
            cached: Boolean = true,
            animOff: Boolean = false,
            endNotice: Boolean = false,
            fromPages: Int = 1,
            toPages: Int = 1,
        ) = resolvePageTurn(
            preference,
            twoPage,
            zoomed,
            cached,
            animOff,
            endNotice,
            fromSpreadPages = fromPages,
            toSpreadPages = toPages,
        )

        assertEquals(ResolvedPageTurn.Curl, mode())
        assertEquals(ResolvedPageTurn.Slide, mode(zoomed = true))
        assertEquals(ResolvedPageTurn.Slide, mode(cached = false))
        assertEquals(ResolvedPageTurn.Instant, mode(animOff = true))
        assertEquals(ResolvedPageTurn.Instant, mode(endNotice = true))
        assertEquals(ResolvedPageTurn.Instant, mode(preference = PageTurnAnimation.None))
        assertEquals(ResolvedPageTurn.Slide, mode(preference = PageTurnAnimation.Slide))
        assertEquals(ResolvedPageTurn.Instant, mode(preference = PageTurnAnimation.Slide, animOff = true))
    }

    @Test
    fun twoPageTurnCurtsACachedSpreadAndFallsBackToSlideAtTheEnds() {
        fun mode(
            twoPage: Boolean = true,
            zoomed: Boolean = false,
            cached: Boolean = true,
            animOff: Boolean = false,
            fromPages: Int = 2,
            toPages: Int = 2,
        ) = resolvePageTurn(
            PageTurnAnimation.Curl,
            twoPage,
            zoomed,
            cached,
            animOff,
            endNoticeOnly = false,
            fromSpreadPages = fromPages,
            toSpreadPages = toPages,
        )

        // Two pages onto two pages (LTR and RTL) curls.
        assertEquals(ResolvedPageTurn.Curl, mode())
        // Cover alone forward (1 -> 2) curls with the single-page sheet.
        assertEquals(ResolvedPageTurn.Curl, mode(fromPages = 1, toPages = 2))
        // Last odd page reached backward (1 -> 2) curls too.
        assertEquals(ResolvedPageTurn.Curl, mode(fromPages = 1, toPages = 2))
        // Two pages onto a lone page (cover / last odd) has no facing page: Slide.
        assertEquals(ResolvedPageTurn.Slide, mode(fromPages = 2, toPages = 1))
        // A one-page spread onto a lone page still curls (the single sheet turns).
        assertEquals(ResolvedPageTurn.Curl, mode(fromPages = 1, toPages = 1))
        // Zoomed or an uncached target spread always slides.
        assertEquals(ResolvedPageTurn.Slide, mode(zoomed = true))
        assertEquals(ResolvedPageTurn.Slide, mode(cached = false))
        assertEquals(ResolvedPageTurn.Instant, mode(animOff = true))
        // Single-page view is unchanged: the sizes are always 1.
        assertEquals(
            ResolvedPageTurn.Curl,
            mode(twoPage = false, fromPages = 1, toPages = 1),
        )
    }

    @Test
    fun interruptOnNewInputFinishesCurrentThenStartsNext() {
        val draggingPast = CurlEffectState(0, 1, CurlSide.Right, 0.5f, dragging = true)
        assertEquals(1, curlFinishIndex(draggingPast))
        val draggingShort = draggingPast.copy(progress = 0.1f)
        assertEquals(0, curlFinishIndex(draggingShort))
        val springBack = CurlEffectState(2, 3, CurlSide.Left, 0.2f, dragging = false, completing = false)
        assertEquals(2, curlFinishIndex(springBack))
        val settling = CurlEffectState(0, 1, CurlSide.Left, progress = 0.4f, dragging = false)
        assertEquals(1, curlFinishIndex(settling))
    }

    @Test
    fun durationFollowsAnimatorScaleAndRemainingTravel() {
        assertEquals(0L, curlDurationMs(0f))
        assertTrue(curlDurationMs(1f) > 0L)
        assertEquals(curlDurationMs(1f) * 2, curlDurationMs(2f))
        assertTrue(animationsOff(0f))
        assertFalse(animationsOff(1f))
        val full = curlDurationMs(1f)
        assertEquals(full / 2, curlRemainingDurationMs(1f, fromProgress = 0.5f, animateForward = true))
        assertEquals(full / 2, curlRemainingDurationMs(1f, fromProgress = 0.5f, animateForward = false))
        // A short release keeps the 150ms floor instead of snapping back in a few frames.
        assertEquals(150L, curlRemainingDurationMs(1f, fromProgress = 0.1f, animateForward = false))
        assertEquals(75L, curlRemainingDurationMs(0.5f, fromProgress = 0.1f, animateForward = false))
        assertEquals(0L, curlRemainingDurationMs(0f, 0.5f, animateForward = true))
    }

    @Test
    fun slideOffsetFollowsFingerWithinPagerBounds() {
        assertEquals(0.2f, curlSlideOffset(0.4f, forward = true), 0.001f)
        assertEquals(-0.2f, curlSlideOffset(0.4f, forward = false), 0.001f)
        assertEquals(0.5f, curlSlideOffset(1f, forward = true), 0.001f)
        assertEquals(-0.5f, curlSlideOffset(1f, forward = false), 0.001f)
        assertEquals(0f, curlSlideOffset(-1f, forward = true), 0.001f)
    }

    @Test
    fun followProgressCompensatesForAppliedSlideOffset() {
        assertEquals(0.4f, curlFollowProgress(0.2f, 0.2f), 0.001f)
        assertEquals(0.4f, curlFollowProgress(0.2f, -0.2f), 0.001f)
        assertEquals(1f, curlFollowProgress(0.9f, 0.5f), 0.001f)
        assertEquals(0.3f, curlFollowProgress(0.3f, 0f), 0.001f)
    }

    @Test
    fun pageRenderSlotMatchesTabletopAndBookHalves() {
        val window = IntSize(800, 600)
        val tabletop = FoldPosture.Tabletop(IntRect(0, 280, 800, 320))
        val tableSlot = pageRenderSlot(window, tabletop, ReadingDirection.LTR, twoPage = false)
        val expectedTable = tabletopHalves(window, tabletop.foldBounds).first
        assertEquals(expectedTable.width, tableSlot.width)
        assertEquals(expectedTable.height, tableSlot.height)

        val book = FoldPosture.Book(IntRect(390, 0, 410, 600))
        val bookSlot = pageRenderSlot(window, book, ReadingDirection.RTL, twoPage = false)
        val expectedBook = bookSingleHalf(bookHalves(window, book.foldBounds), ReadingDirection.RTL)
        assertEquals(expectedBook.width, bookSlot.width)
        assertEquals(expectedBook.height, bookSlot.height)

        val flat = pageRenderSlot(window, FoldPosture.Flat, ReadingDirection.LTR, twoPage = false)
        assertEquals(800, flat.width)
        assertEquals(600, flat.height)
    }

    @Test
    fun pageDrawRectFitsAndAlignsLikeCroppedPage() {
        val slot = IntRect(0, 0, 400, 600)
        val full = CropMargins()
        // Same aspect as the slot: the page fills it.
        val fill = pageDrawRect(200, 300, full, slot, SlotAlignment.Start, sharedScale = null)
        assertEquals(0f, fill.left, 0.01f)
        assertEquals(0f, fill.top, 0.01f)
        assertEquals(400f, fill.width, 0.01f)
        assertEquals(600f, fill.height, 0.01f)

        // A narrow page keeps its aspect and hugs the requested edge.
        val narrowStart = pageDrawRect(100, 300, full, slot, SlotAlignment.Start, null)
        assertEquals(0f, narrowStart.left, 0.01f)
        assertEquals(200f, narrowStart.width, 0.01f)
        val narrowEnd = pageDrawRect(100, 300, full, slot, SlotAlignment.End, null)
        assertEquals(200f, narrowEnd.left, 0.01f)
        assertEquals(400f, narrowEnd.right, 0.01f)

        // A shared scale shrinks both pages equally and centers vertically.
        val scaled = pageDrawRect(200, 300, full, slot, SlotAlignment.Center, sharedScale = 1f)
        assertEquals(100f, scaled.left, 0.01f)
        assertEquals(150f, scaled.top, 0.01f)
        assertEquals(200f, scaled.width, 0.01f)
        assertEquals(300f, scaled.height, 0.01f)

        // A crop moves the content rect, matching PageBitmap's source crop.
        val cropped = pageDrawRect(
            400, 600, CropMargins(left = 0.25f, top = 0f, right = 0f, bottom = 0f),
            slot, SlotAlignment.Start, sharedScale = null,
        )
        assertEquals(0f, cropped.left, 0.01f)
        assertEquals(300f, cropped.width, 1f)
        assertEquals(600f, cropped.height, 1f)
    }

    @Test
    fun curlCanvasUsesThePagePaneAndHingeHalves() {
        val window = IntSize(800, 600)
        val flat = curlCanvas(window, FoldPosture.Flat)!!
        assertEquals(IntSize(800, 600), flat.size)
        assertEquals(IntRect(0, 0, 400, 600), flat.left)
        assertEquals(IntRect(400, 0, 800, 600), flat.right)

        val tabletop = FoldPosture.Tabletop(IntRect(0, 280, 800, 320))
        val table = curlCanvas(window, tabletop)!!
        assertEquals(IntSize(800, 280), table.size)
        assertEquals(IntRect(0, 0, 400, 280), table.left)

        val book = FoldPosture.Book(IntRect(390, 0, 410, 600))
        val bookCanvas = curlCanvas(window, book)!!
        assertEquals(IntSize(800, 600), bookCanvas.size)
        assertEquals(IntRect(0, 0, 390, 600), bookCanvas.left)
        assertEquals(IntRect(410, 0, 800, 600), bookCanvas.right)
        assertNull(curlCanvas(IntSize(0, 600), FoldPosture.Flat))
    }

    @Test
    fun twoPageSheetMustLandOnTheOppositePageWithinOnePixel() {
        val sheet = Rect(400f, 0f, 800f, 600f)
        val backFace = Rect(0f, 0f, 400f, 600f)
        val still = Rect(0f, 0f, 400f, 600f)
        assertTrue(sheetLandsOnSpread(sheet, sheet, backFace, CurlSide.Right, still))
        // One pixel of difference is tolerated; two is not.
        assertTrue(sheetLandsOnSpread(sheet, sheet.translate(1f, 0f), backFace, CurlSide.Right, still))
        assertFalse(sheetLandsOnSpread(sheet, sheet.translate(2f, 0f), backFace, CurlSide.Right, still))
        // The back face must be the same size and sit on the other side of the spine.
        assertFalse(sheetLandsOnSpread(sheet, sheet, Rect(0f, 0f, 390f, 600f), CurlSide.Right, still))
        assertFalse(sheetLandsOnSpread(sheet, sheet, Rect(10f, 0f, 410f, 600f), CurlSide.Right, still))
        // The current still page must land exactly where the back face does.
        assertTrue(sheetLandsOnSpread(sheet, sheet, backFace, CurlSide.Right, still.translate(1f, 0f)))
        assertFalse(sheetLandsOnSpread(sheet, sheet, backFace, CurlSide.Right, still.translate(2f, 0f)))
        // Mirrored side: the left sheet lands on the right page.
        val left = Rect(0f, 0f, 400f, 600f)
        assertTrue(sheetLandsOnSpread(left, left, Rect(400f, 0f, 800f, 600f), CurlSide.Left, sheet))
    }

    @Test
    fun settlingTurnKeepsItsProgressAcrossReleaseAndCancel() {
        // Released past the settle threshold: the effect finishes forward from the live progress.
        val released = CurlEffectState(0, 1, CurlSide.Right, 0.4f, dragging = true)
        assertEquals(1, curlFinishIndex(released, liveProgress = 0.4f))
        // Released short: it reverts from the same live progress, never snapping to 0 first.
        val reverted = released.copy(progress = 0.2f)
        assertEquals(0, curlFinishIndex(reverted, liveProgress = 0.2f))
        // A settling turn keeps its target; an interrupting input resumes from that page.
        val settling = CurlEffectState(0, 1, CurlSide.Left, 0.6f, dragging = false)
        assertEquals(1, curlFinishIndex(settling))
        val reverting = settling.copy(completing = false)
        assertEquals(0, curlFinishIndex(reverting))
    }
}
