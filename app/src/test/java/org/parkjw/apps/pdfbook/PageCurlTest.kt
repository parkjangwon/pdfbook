package org.parkjw.apps.pdfbook

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
        val current = CurlEffectState(0, 1, CurlSide.Left, progress = 0.4f, dragging = false)
        val action = interruptCurl(current, fromIndex = 1, toIndex = 2, side = CurlSide.Left)
        assertEquals(1, action.finishToIndex)
        assertEquals(1, action.next.fromIndex)
        assertEquals(2, action.next.toIndex)
        assertEquals(0f, action.next.progress, 0f)
        assertFalse(action.next.dragging)
        assertTrue(action.next.completing)

        val draggingPast = CurlEffectState(0, 1, CurlSide.Right, 0.5f, dragging = true)
        assertEquals(1, curlFinishIndex(draggingPast))
        val draggingShort = draggingPast.copy(progress = 0.1f)
        assertEquals(0, curlFinishIndex(draggingShort))
        val springBack = CurlEffectState(2, 3, CurlSide.Left, 0.2f, dragging = false, completing = false)
        assertEquals(2, curlFinishIndex(springBack))

        val fresh = interruptCurl(null, fromIndex = 0, toIndex = 1, side = CurlSide.Right)
        assertNull(fresh.finishToIndex)
        assertEquals(CurlSide.Right, fresh.next.side)
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
