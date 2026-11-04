package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation

/**
 * A lone page turning onto another lone page (1 -> 1, which includes a wide page shown alone) must
 * not curl across a real hinge gap: the flap would have to cross the hinge (#172). It slides.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [37], qualifiers = "ko-w841dp-h701dp-xxhdpi")
class WidePageAloneHingeCurlTest {
    private val window = IntSize(400, 800)

    @Test
    fun lonePageTurnOnAHingeFallsBackToSlide() {
        val build = buildSingleSpreadCurlBuild(
            renderer = Pages(),
            currentPage = 3,
            targetSpread = listOf(4),
            direction = ReadingDirection.LTR,
            side = CurlSide.Right,
            posture = FoldPosture.Book(IntRect(192, 0, 208, 800)),
            window = window,
            autoCrop = false,
        )
        assertEquals(TwoPageCurlFallback.HingeGap, build.fallback)
        assertEquals(null, build.curl)
    }

    @Test
    fun aFlushBookStillCurvesItsLonePage() {
        val build = buildSingleSpreadCurlBuild(
            renderer = Pages(),
            currentPage = 3,
            targetSpread = listOf(4),
            direction = ReadingDirection.LTR,
            side = CurlSide.Right,
            posture = FoldPosture.Book(IntRect(200, 0, 200, 800)),
            window = window,
            autoCrop = false,
        )
        assertNotEquals(TwoPageCurlFallback.HingeGap, build.fallback)
    }

    @Test
    fun hingeGapSlidesEveryTurnThatTouchesALoneSpread() {
        fun mode(from: Int, to: Int) = resolvePageTurn(
            preference = PageTurnAnimation.Curl,
            twoPage = true,
            zoomed = false,
            nextPageCached = true,
            animationsOff = false,
            endNoticeOnly = false,
            fromSpreadPages = from,
            toSpreadPages = to,
            hingeGap = true,
        )
        assertEquals(ResolvedPageTurn.Slide, mode(1, 1))
        assertEquals(ResolvedPageTurn.Slide, mode(1, 2))
        assertEquals(ResolvedPageTurn.Slide, mode(2, 1))
        assertEquals(ResolvedPageTurn.Curl, mode(2, 2))
    }

    @Test
    fun withoutAHingeALonePageStillCurls() {
        assertEquals(
            ResolvedPageTurn.Curl,
            resolvePageTurn(
                preference = PageTurnAnimation.Curl,
                twoPage = true,
                zoomed = false,
                nextPageCached = true,
                animationsOff = false,
                endNoticeOnly = false,
                fromSpreadPages = 1,
                toSpreadPages = 1,
            ),
        )
    }

    @Test
    fun theVirtualSpreadIsTheRenderSlotOnlyForARealGap() {
        val window = IntSize(400, 800)
        val wide = acrossPagePasses(window, IntRect(192, 0, 208, 800), ReadingDirection.LTR)
        assertEquals(2, wide.size)
        assertEquals(IntRect(0, 0, 192, 800), wide[0].clip)
        assertEquals(0, wide[0].dx)
        assertEquals(IntRect(208, 0, 400, 800), wide[1].clip)
        assertEquals(16, wide[1].dx)
        assertEquals(384, wide[0].slotWidth)
        assertEquals(384, wide[1].slotWidth)

        // A 1px seam keeps window coordinates: one pass, the window slot.
        val seam = acrossPagePasses(window, IntRect(200, 0, 201, 800), ReadingDirection.LTR)
        assertEquals(1, seam.size)
        assertEquals(IntRect(0, 0, 400, 800), seam[0].clip)
        assertEquals(0, seam[0].dx)
        assertEquals(400, seam[0].slotWidth)
    }

    private class Pages : PageBitmaps {
        override val pageCount = 8
        private val bitmaps = HashMap<Int, Bitmap>()

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap =
            bitmaps.getOrPut(page) {
                Bitmap.createBitmap(
                    maxWidth.coerceAtLeast(1),
                    maxHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                ).apply { eraseColor(0xFFF7F2E8.toInt()) }
            }

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap =
            cached(page, maxWidth, maxHeight)
    }
}
