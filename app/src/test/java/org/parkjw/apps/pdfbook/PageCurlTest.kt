package org.parkjw.apps.pdfbook

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

        val fresh = interruptCurl(null, fromIndex = 0, toIndex = 1, side = CurlSide.Right)
        assertNull(fresh.finishToIndex)
        assertEquals(CurlSide.Right, fresh.next.side)
    }

    @Test
    fun durationFollowsAnimatorScale() {
        assertEquals(0L, curlDurationMs(0f))
        assertTrue(curlDurationMs(1f) > 0L)
        assertEquals(curlDurationMs(1f) * 2, curlDurationMs(2f))
        assertTrue(animationsOff(0f))
        assertFalse(animationsOff(1f))
    }
}
