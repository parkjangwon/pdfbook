package org.parkjw.apps.pdfbook

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class LargeBookPrerenderTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun fastFlipThroughSixHundredPagesStaysWithinLimit() {
        val pdf = GeneratedImagePdf.write(folder.newFile(), 600)
        assertEquals(600, pdf.open().use { it.pageCount })
        flip(pdf, twoPage = false, width = 1080, height = 2400, limit = cacheLimitBytes(256L * MiB))
        flip(pdf, twoPage = true, width = 2560, height = 1600, limit = 3L * 1280 * 1600 * 4)
    }

    @Test
    fun pageTurnTimingBeforeAndAfterPrerender() {
        val pdf = GeneratedImagePdf.write(folder.newFile(), 40, imageWidth = 1200, imageHeight = 1700)
        val width = 1080
        val height = 2400
        val pages = spreads(pdf.pageCount, twoPage = false, coverAlone = true)
        val cold = PageRenderer(pdf.open(), cacheLimitBytes(256L * MiB)).use { renderer ->
            runBlocking { renderer.render(1, width, height) }
            (2..31).map { page ->
                timed { assertNotNull(runBlocking { renderer.render(page, width, height) }) }
            }
        }
        val warm = PageRenderer(pdf.open(), cacheLimitBytes(256L * MiB)).use { renderer ->
            (2..31).map { page ->
                val index = page - 2
                runBlocking { renderer.render(page - 1, width, height) }
                renderer.prerender(
                    spreadRequests(pages[index], width, height),
                    prerenderTargets(pages, index, width, height),
                )
                runBlocking { renderer.awaitPrerender() }
                timed { assertNotNull(renderer.cached(page, width, height)) }
            }
        }
        val before = median(cold)
        val after = median(warm)
        println("PR-5 page turn to final image at ${width}x$height (JVM, n=${cold.size}):")
        println("PR-5 before median=%.2fms p90=%.2fms".format(before, p90(cold)))
        println("PR-5 after  median=%.4fms p90=%.4fms".format(after, p90(warm)))
        assertTrue(after < before)
    }

    private fun flip(pdf: GeneratedImagePdf, twoPage: Boolean, width: Int, height: Int, limit: Long) {
        val pages = spreads(pdf.pageCount, twoPage = twoPage, coverAlone = true)
        val source = pdf.open()
        PageRenderer(source, limit).use { renderer ->
            val sweep = pages.indices + pages.indices.reversed().take(40)
            sweep.forEachIndexed { step, index ->
                val current = pages[index]
                val visible = spreadRequests(current, width, height)
                visible.forEach { assertNotNull(runBlocking { renderer.render(it.page, it.maxWidth, it.maxHeight) }) }
                renderer.prerender(visible, prerenderTargets(pages, index, width, height))
                if (step % 25 == 0) runBlocking { renderer.awaitPrerender() }
                assertTrue(renderer.cache.sizeBytes <= limit)
            }
            runBlocking { renderer.awaitPrerender() }
            assertTrue(renderer.cache.peakBytes <= limit)
            assertTrue(renderer.cache.pages().isNotEmpty())
            println(
                "PR-3 twoPage=$twoPage steps=${sweep.size} draws=${source.draws} " +
                    "limit=${limit / MiB}MiB peak=${renderer.cache.peakBytes / MiB}MiB",
            )
        }
    }

    private inline fun timed(block: () -> Unit): Double {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000.0
    }

    private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

    private fun p90(values: List<Double>): Double = values.sorted()[(values.size * 9) / 10]

    private companion object {
        const val MiB = 1024L * 1024L
    }
}
