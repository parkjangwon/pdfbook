package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w400dp-h800dp-mdpi")
class PageRendererFailureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failingPageRendersAsNullAndOtherPagesStillDraw() {
        val source = FailingSource(failing = setOf(2))
        val renderer = PageRenderer(source)
        try {
            assertNull(runBlocking { renderer.render(2, 40, 40) })
            assertNotNull(runBlocking { renderer.render(1, 40, 40) })
            assertNotNull(runBlocking { renderer.render(3, 40, 40) })
        } finally {
            close(renderer)
        }
    }

    @Test
    fun prerenderSkipsAPageThatAlreadyFailed() {
        val source = FailingSource(failing = setOf(2))
        val renderer = PageRenderer(source)
        try {
            renderer.prerender(
                listOf(PageRequest(1, 40, 40), PageRequest(2, 40, 40), PageRequest(3, 40, 40)),
                emptyList(),
            )
            runBlocking { renderer.awaitPrerender() }
            assertEquals(1, source.draws.count { it == 2 })
            assertEquals(setOf(1, 3), renderer.cache.pages())

            renderer.prerender(listOf(PageRequest(2, 40, 40)), emptyList())
            runBlocking { renderer.awaitPrerender() }
            assertEquals("failed page was drawn again", 1, source.draws.count { it == 2 })
        } finally {
            close(renderer)
        }
    }

    @Test
    fun pageWithAFailingDrawShowsTheBlankPlaceholder() {
        val source = FailingSource(failing = setOf(2))
        val renderer = PageRenderer(source)
        try {
            composeRule.setContent {
                Box(Modifier.size(300.dp, 400.dp).background(ReaderBackground)) {
                    PageBitmap(renderer, 2, 300, 400, Modifier.fillMaxSize())
                }
            }
            composeRule.waitForIdle()
            val pixels = composeRule.onRoot().captureToImage().toPixelMap()
            assertEquals(ReaderBackground.toArgb(), pixels[pixels.width / 2, pixels.height / 2].toArgb())
        } finally {
            close(renderer)
        }
    }

    @Test
    fun pageThatDrawsFineIsShown() {
        val source = FailingSource(failing = emptySet())
        val renderer = PageRenderer(source)
        try {
            composeRule.setContent {
                Box(Modifier.size(300.dp, 400.dp).background(ReaderBackground)) {
                    PageBitmap(renderer, 1, 300, 400, Modifier.fillMaxSize())
                }
            }
            composeRule.waitForIdle()
            val pixels = composeRule.onRoot().captureToImage().toPixelMap()
            assertEquals(Color.RED, pixels[pixels.width / 2, pixels.height / 2].toArgb())
        } finally {
            close(renderer)
        }
    }

    private fun close(renderer: PageRenderer) {
        renderer.close()
        runBlocking { renderer.awaitClosed() }
    }

    private class FailingSource(private val failing: Set<Int>) : PageSource {
        override val pageCount: Int = 4
        val draws = CopyOnWriteArrayList<Int>()

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap {
            draws += page
            if (page in failing) error("synthetic page $page cannot be decoded")
            return Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
            }
        }

        override fun close() = Unit
    }
}
