package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class PageNavTest {
    private val single = spreads(120, twoPage = false, coverAlone = true)

    @Test
    fun outlineFlattensWithDepthAndDropsBadEntries() {
        val nodes = listOf(
            OutlineNode("Part 1", 1, listOf(OutlineNode("Chapter 1", 3), OutlineNode("  ", 4), OutlineNode("Gone", 999))),
            OutlineNode("Part 2", 50, listOf(OutlineNode("Chapter 2", 0))),
        )
        assertEquals(
            listOf(OutlineEntry("Part 1", 1, 0), OutlineEntry("Chapter 1", 3, 1), OutlineEntry("Part 2", 50, 0)),
            outlineEntries(nodes, pageCount = 120),
        )
    }

    @Test
    fun currentOutlineEntryIsLastOneAtOrBeforePage() {
        val entries = listOf(OutlineEntry("a", 1, 0), OutlineEntry("b", 10, 0), OutlineEntry("c", 30, 0))
        assertEquals(-1, currentOutlineIndex(emptyList(), 5))
        assertEquals(0, currentOutlineIndex(entries, 9))
        assertEquals(1, currentOutlineIndex(entries, 10))
        assertEquals(2, currentOutlineIndex(entries, 120))
    }

    @Test
    fun rendererMapsSourceOutlineAndSurvivesSourceFailure() = runBlocking {
        val ok = PageRenderer(FakeSource(outline = listOf(OutlineNode("Intro", 2), OutlineNode("Bad", 9))))
        assertEquals(listOf(OutlineEntry("Intro", 2, 0)), ok.outline())
        ok.close()
        val broken = PageRenderer(FakeSource(failOutline = true))
        assertEquals(emptyList<OutlineEntry>(), broken.outline())
        broken.close()
    }

    @Test
    fun jumpFarRemembersOriginAndReturnComesBack() {
        val jump = pageJump(single, fromIndex = 4, page = 80)
        assertEquals(79, jump.targetIndex)
        assertEquals(JumpBack(page = 5, shownAt = 79), jump.jumpBack)
        val back = jumpBackStep(single, jump.jumpBack!!, 79)
        assertEquals(4, back.targetIndex)
        assertEquals(JumpBack(page = 80, shownAt = 4), back.next)
    }

    @Test
    fun jumpNearOffersNoReturn() {
        assertNull(pageJump(single, fromIndex = 4, page = 6).jumpBack)
        assertEquals(5, pageJump(single, fromIndex = 4, page = 6).targetIndex)
    }

    @Test
    fun thumbRequestRoundsUpToBucketAndIsStablePerPage() {
        assertEquals(PageRequest(7, 80, 112), thumbRequest(7, 1f))
        assertEquals(PageRequest(7, 160, 224), thumbRequest(7, 2.2f))
        val request = thumbRequest(3, 2.625f)
        assertEquals(0, request.maxWidth % THUMB_BUCKET_PX)
        assertEquals(0, request.maxHeight % THUMB_BUCKET_PX)
        assertEquals(request, thumbRequest(3, 2.625f))
    }

    @Test
    fun thumbnailsAreCachedAndEvictedWithinLimit() = runBlocking {
        val pdf = GeneratedImagePdf.write(File.createTempFile("thumbs", ".pdf").apply { deleteOnExit() }, pageCount = 5)
        val source = pdf.open()
        val request = thumbRequest(1, 1f)
        val oneThumb = request.maxWidth * request.maxHeight * 4L
        PageRenderer(source, thumbLimitBytes = oneThumb * 2).use { renderer ->
            val first = renderer.thumbnail(request)
            assertNotNull(first)
            assertSame(first, renderer.thumbnail(request))
            assertEquals(1, source.draws)
            renderer.thumbnail(thumbRequest(2, 1f))
            renderer.thumbnail(thumbRequest(3, 1f))
            assertEquals(3, source.draws)
            renderer.thumbnail(request) // page 1 was least recently used, so it renders again
            assertEquals(4, source.draws)
            assertNull(renderer.thumbnail(thumbRequest(6, 1f)))
            assertNull(renderer.thumbnail(thumbRequest(0, 1f)))
        }
    }

    private class FakeSource(
        private val outline: List<OutlineNode> = emptyList(),
        private val failOutline: Boolean = false,
    ) : PageSource {
        override val pageCount = 5
        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap =
            Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        override fun outline(): List<OutlineNode> = if (failOutline) error("no outline") else outline
        override fun close() {}
    }
}
