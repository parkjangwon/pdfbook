package org.parkjw.apps.pdfbook

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
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
        ) = resolvePageTurn(preference, twoPage, zoomed, cached, animOff, endNotice)

        assertEquals(ResolvedPageTurn.Curl, mode())
        assertEquals(ResolvedPageTurn.Slide, mode(twoPage = true))
        assertEquals(ResolvedPageTurn.Slide, mode(zoomed = true))
        assertEquals(ResolvedPageTurn.Slide, mode(cached = false))
        assertEquals(ResolvedPageTurn.Instant, mode(animOff = true))
        assertEquals(ResolvedPageTurn.Instant, mode(endNotice = true))
        assertEquals(ResolvedPageTurn.Instant, mode(preference = PageTurnAnimation.None))
        assertEquals(ResolvedPageTurn.Slide, mode(preference = PageTurnAnimation.Slide))
        assertEquals(ResolvedPageTurn.Instant, mode(preference = PageTurnAnimation.Slide, animOff = true))
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
}
