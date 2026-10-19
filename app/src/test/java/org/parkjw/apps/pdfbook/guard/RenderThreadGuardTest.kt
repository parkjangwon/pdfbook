package org.parkjw.apps.pdfbook.guard

import android.graphics.Bitmap
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.PageRenderer
import org.parkjw.apps.pdfbook.PageRequest
import org.parkjw.apps.pdfbook.PageSource
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class RenderThreadGuardTest {
    @Test
    fun pdfPageCallsOnlyInsidePdfPageSource() {
        val offenders = SourceFiles.kotlin().flatMap { file ->
            findPdfPageCallsOutsidePageSource(file.name, file.readText()).map { "${SourceFiles.rel(file)}:$it" }
        }
        assertEquals("PdfRenderer page calls outside PdfPageSource:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun pageDrawOnlyInsideWithContext() {
        val source = File(SourceFiles.javaRoot, "PageRenderer.kt").readText()
        assertTrue("PageRenderer.kt draws no pages", Regex("""\bsource\.draw\s*\(""").containsMatchIn(source))
        val offenders = findDrawCallsOutsideWithContext(source).map { "PageRenderer.kt:$it" }
        assertEquals("PageSource.draw outside withContext:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun renderAndPrerenderNeverDrawOnMainThread() {
        val source = ThreadRecordingSource()
        PageRenderer(source).use { renderer ->
            runBlocking {
                renderer.render(1, 10, 10)
                renderer.prerender(listOf(PageRequest(1, 10, 10)), listOf(PageRequest(2, 10, 10)))
                renderer.awaitPrerender()
            }
        }
        assertEquals(listOf(1, 2), source.pages)
        assertFalse("draw ran on the calling thread", source.threads.contains(Thread.currentThread()))
        assertFalse("draw ran on the main looper", source.onMain.any { it })
        assertFalse("close ran on the main looper", source.closedOnMain)
    }

    @Test
    fun guardsDetectSampleViolations() {
        val inside = "class PdfPageSource() {\n fun draw() { pdf.openPage(0).use { it.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) } }\n}\n"
        assertEquals(emptyList<String>(), findPdfPageCallsOutsidePageSource("PageRenderer.kt", inside))
        assertEquals(1, findPdfPageCallsOutsidePageSource("PageRenderer.kt", inside + "fun x() { pdf.openPage(1) }").size)
        assertEquals(2, findPdfPageCallsOutsidePageSource("ReaderScreen.kt", inside).size)
        assertEquals(emptyList<String>(), findDrawCallsOutsideWithContext("withContext(d) { source.draw(1, 2, 3) }"))
        assertEquals(1, findDrawCallsOutsideWithContext("fun f() { source.draw(1, 2, 3) }").size)
    }

    private class ThreadRecordingSource : PageSource {
        val pages = mutableListOf<Int>()
        val threads = mutableListOf<Thread>()
        val onMain = mutableListOf<Boolean>()
        @Volatile var closedOnMain = false

        override val pageCount: Int = 3

        @Synchronized
        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            pages += page
            threads += Thread.currentThread()
            onMain += Looper.getMainLooper().isCurrentThread
            return Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        }

        override fun close() {
            closedOnMain = Looper.getMainLooper().isCurrentThread
        }
    }

    companion object {
        private val pdfPageCall = Regex("""\.openPage\s*\(|\bRENDER_MODE_\w+""")
        private val drawCall = Regex("""\bsource\.draw\s*\(""")

        fun findPdfPageCallsOutsidePageSource(fileName: String, source: String): List<String> {
            val allowed = if (fileName == "PageRenderer.kt") {
                SourceFiles.blocks(source, Regex("""\bclass\s+PdfPageSource\b[^{]*"""))
            } else {
                emptyList()
            }
            return pdfPageCall.findAll(source)
                .filter { hit -> allowed.none { hit.range.first in it } }
                .map { "${SourceFiles.lineOf(source, it.range.first)}: ${it.value}" }
                .toList()
        }

        fun findDrawCallsOutsideWithContext(source: String): List<String> {
            val allowed = SourceFiles.blocks(source, Regex("""\bwithContext\s*\([^)]*\)\s*"""))
            return drawCall.findAll(source)
                .filter { hit -> allowed.none { hit.range.first in it } }
                .map { "${SourceFiles.lineOf(source, it.range.first)}: ${it.value}" }
                .toList()
        }
    }
}
