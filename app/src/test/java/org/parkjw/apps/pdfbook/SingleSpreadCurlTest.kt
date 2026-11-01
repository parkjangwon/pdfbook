package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The lone-page turn (#173): the cover alone, or a lone last page, peels over its own drawn
 * page rect. Pins the sheet rect, the 1px containment fallback and the Book hinge fallback
 * against the real [buildSingleSpreadCurl]. Synthetic bitmaps only.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class SingleSpreadCurlTest {
    private val portrait = IntSize(WINDOW_WIDTH, WINDOW_HEIGHT)
    private val landscape = IntSize(WINDOW_HEIGHT, WINDOW_WIDTH)

    @Test
    fun coverForwardCurlsOverItsOwnPageRect() {
        val curl = build(COVER, listOf(2, 3), ReadingDirection.LTR, CurlSide.Right)
        assertTrue("cover forward must curl", curl != null)
        // 1080x1528 drawn into the 1080x2544 pane: full width, letterboxed top and bottom.
        val pageRect = curl!!.frontRect!!
        assertEquals(Rect(0f, 508f, 1080f, 2036f), pageRect)
        curl.underlay.forEach { page ->
            assertTrue("${page.dest} must be inside $pageRect", pageRect.containsWithin(page.dest))
        }
    }

    @Test
    fun theFoldRunsOnThePageRectNotThePane() {
        val curl = build(COVER, listOf(2, 3), ReadingDirection.LTR, CurlSide.Right)!!
        val rect = curl.frontRect!!
        val width = rect.width
        val height = rect.height
        // Page-local corner point: (w(1-2p), h - 0.18h sin(pi p)); the overlay only offsets it
        // by the page rect origin, so the painted tip lands inside the page.
        val shape = CurlShape()
        for (p in listOf(0.5f, 0.95f)) {
            updateCurlShape(width, height, p, CurlSide.Right, shape)
            assertEquals(
                "tip x at $p",
                (width * (1f - 2f * p)).toDouble(),
                shape.tipX.toDouble(),
                0.01,
            )
            assertEquals(
                "tip y at $p",
                (height - 0.18f * height * sin(PI.toFloat() * p)).toDouble(),
                shape.tipY.toDouble(),
                0.01,
            )
        }
    }

    @Test
    fun coverForwardCurlsInRtlWithTheTargetOrderReversed() {
        val ltr = build(COVER, listOf(2, 3), ReadingDirection.LTR, CurlSide.Right)!!
        val rtl = build(COVER, listOf(2, 3), ReadingDirection.RTL, CurlSide.Left)
        assertTrue("rtl cover forward must curl", rtl != null)
        assertEquals(ltr.frontRect, rtl!!.frontRect)
        // RTL reads 3 · 2, so the left underlay page is 3 and the right one is 2.
        assertTrue(rtl.underlay[0].dest.left < rtl.underlay[1].dest.left)
        assertTrue(rtl.underlay[0].dest.width < rtl.frontRect!!.width)
    }

    @Test
    fun loneLastPageCurlsBackward() {
        val curl = build(19, listOf(17, 18), ReadingDirection.LTR, CurlSide.Left)
        assertTrue("lone last page must curl backward", curl != null)
        assertEquals(Rect(0f, 508f, 1080f, 2036f), curl!!.frontRect)
    }

    @Test
    fun aSinglePageTargetFitsAndCurls() {
        val curl = build(COVER, listOf(2), ReadingDirection.LTR, CurlSide.Right)
        assertTrue("a lone page onto a lone page must curl", curl != null)
        assertEquals(1, curl!!.underlay.size)
        assertEquals(curl.frontRect, curl.underlay.first().dest)
    }

    @Test
    fun aTargetSpreadWiderThanTheLonePageSlides() {
        // Landscape: the page is 763 wide but the two-page spread spans 1526. Its last frame
        // would stick out of the sheet, so the turn must slide.
        assertNull(build(COVER, listOf(2, 3), ReadingDirection.LTR, CurlSide.Right, window = landscape))
    }

    @Test
    fun bookPostureWithAHingeGapSlides() {
        // #172 owns the hinge-cut curl; until then the flap must not cross the gap.
        val gap = FoldPosture.Book(IntRect(WINDOW_WIDTH / 2 - 4, 0, WINDOW_WIDTH / 2 + 4, WINDOW_HEIGHT))
        assertNull(build(COVER, listOf(2, 3), ReadingDirection.LTR, CurlSide.Right, posture = gap))
    }

    @Test
    fun aCacheMissSlides() {
        val renderer = FakeRenderer(pages = mapOf(COVER to page(1080, 1528)), keys = emptySet())
        assertNull(
            buildSingleSpreadCurl(
                renderer = renderer,
                currentPage = COVER,
                targetSpread = listOf(2, 3),
                direction = ReadingDirection.LTR,
                side = CurlSide.Right,
                posture = FoldPosture.Flat,
                window = portrait,
                autoCrop = false,
            ),
        )
    }

    private fun build(
        current: Int,
        target: List<Int>,
        direction: ReadingDirection,
        side: CurlSide,
        window: IntSize = portrait,
        posture: FoldPosture = FoldPosture.Flat,
    ): TwoPageCurl? {
        // Bitmaps keep a portrait page's 600:849 aspect inside the prerender box.
        val portraitWindow = window.width < window.height
        val pages = mutableMapOf(current to if (portraitWindow) page(1080, 1528) else page(763, 1080))
        val targetBitmap = if (portraitWindow && target.size == 2) page(540, 764) else page(1080, 1528)
        val singleTargetBitmap = if (portraitWindow) page(1080, 1528) else page(763, 1080)
        val targetRequestWidth = if (target.size == 2) window.width / 2 else window.width
        target.forEach { pages[it] = if (target.size == 2) targetBitmap else singleTargetBitmap }
        val renderer = FakeRenderer(
            pages = pages,
            keys = buildSet {
                add(Triple(current, window.width, window.height))
                target.forEach { add(Triple(it, targetRequestWidth, window.height)) }
            },
        )
        return buildSingleSpreadCurl(
            renderer = renderer,
            currentPage = current,
            targetSpread = target,
            direction = direction,
            side = side,
            posture = posture,
            window = window,
            autoCrop = false,
        )
    }

    /** Cache keys are (page, maxWidth, maxHeight); only the exact prerender key is served. */
    private class FakeRenderer(
        private val pages: Map<Int, Bitmap>,
        private val keys: Set<Triple<Int, Int, Int>>,
    ) : PageBitmaps {
        override val pageCount = 100

        override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            cached(page, maxWidth, maxHeight)

        override fun cached(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
            pages[page]?.takeIf { Triple(page, maxWidth, maxHeight) in keys }
    }

    private fun page(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    private companion object {
        const val COVER = 1
        const val WINDOW_WIDTH = 1080
        const val WINDOW_HEIGHT = 2544
    }
}
