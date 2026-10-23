package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class PageSearchTest {
    private class FakeSource : PageSource {
        val counts = mutableMapOf<Int, Int>()
        val visited = Collections.synchronizedList(mutableListOf<Int>())
        val threads = Collections.synchronizedList(mutableListOf<Thread>())
        var onPage: ((Int) -> Unit)? = null
        override val pageCount: Int = 4

        override fun draw(page: Int, maxWidth: Int, maxHeight: Int): Bitmap =
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        override fun searchText(page: Int, query: String): Int {
            visited += page
            threads += Thread.currentThread()
            onPage?.invoke(page)
            return counts[page] ?: 0
        }

        override fun close() {}
    }

    @Test
    fun collectsHitsPerPageInOrder() = runTest {
        val source = FakeSource().apply {
            counts[2] = 3
            counts[4] = 1
        }
        val renderer = PageRenderer(source, renderDispatcher = StandardTestDispatcher(testScheduler))
        val hits = mutableListOf<SearchPageHits>()
        renderer.search("단어") { hits += it }
        assertEquals(listOf(SearchPageHits(2, 3), SearchPageHits(4, 1)), hits)
        assertEquals(listOf(1, 2, 3, 4), source.visited)
    }

    @Test
    fun noTextLayerYieldsNoHits() = runTest {
        val source = FakeSource()
        val renderer = PageRenderer(source, renderDispatcher = StandardTestDispatcher(testScheduler))
        val hits = mutableListOf<SearchPageHits>()
        renderer.search("단어") { hits += it }
        assertEquals(emptyList<SearchPageHits>(), hits)
        assertEquals(listOf(1, 2, 3, 4), source.visited)
    }

    @Test
    fun cancelBetweenPagesStopsAndReleasesPageLock() = runTest {
        val source = FakeSource().apply {
            counts[1] = 1
            counts[3] = 2
        }
        val renderer = PageRenderer(source, renderDispatcher = StandardTestDispatcher(testScheduler))
        var job: Job? = null
        source.onPage = { page -> if (page == 2) job?.cancel() }
        val hits = mutableListOf<SearchPageHits>()
        job = launch { renderer.search("단어") { hits += it } }
        advanceUntilIdle()
        job.join()
        assertEquals(listOf(1, 2), source.visited)
        assertEquals(listOf(SearchPageHits(1, 1)), hits)
        val afterCancel = mutableListOf<SearchPageHits>()
        renderer.search("다시") { afterCancel += it }
        assertEquals(listOf(SearchPageHits(1, 1), SearchPageHits(3, 2)), afterCancel)
    }

    @Test
    fun searchRunsOffTheMainThread() = runTest {
        val source = FakeSource().apply { counts[1] = 1 }
        val renderer = PageRenderer(source, renderDispatcher = Dispatchers.IO.limitedParallelism(1))
        val hits = mutableListOf<SearchPageHits>()
        renderer.search("단어") { hits += it }
        assertEquals(listOf(SearchPageHits(1, 1)), hits)
        assertFalse(
            "searchText ran on the main looper",
            source.threads.any { it === Looper.getMainLooper().thread },
        )
    }
}
