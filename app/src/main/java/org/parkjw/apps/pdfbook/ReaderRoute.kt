package org.parkjw.apps.pdfbook

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow

@Composable
fun ReaderRoute(
    book: BookInfo,
    store: BookStore,
    volumeDelta: Flow<Int>,
    onBack: () -> Unit,
) {
    val key = bookKey(book.uri, book.size)
    var saved by remember(key) { mutableStateOf<BookState?>(null) }
    LaunchedEffect(key) { saved = store.state(key) }
    val state = saved ?: return

    val context = LocalContext.current
    val uri = remember(key) { Uri.parse(book.uri) }
    val openResult by produceState<PdfOpenResult>(PdfOpenResult.Loading, uri, context) {
        value = PdfOpenResult.Loading
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            value = PdfOpenResult.Failed
            return@produceState
        }
        var opened: PageRenderer? = null
        value = try {
            PageRenderer.open(pfd).also { opened = it }.let { PdfOpenResult.Ready(it) }
        } catch (_: Exception) {
            pfd.close()
            PdfOpenResult.Failed
        }
        awaitDispose { opened?.close() }
    }

    when (val result = openResult) {
        PdfOpenResult.Loading -> Box(Modifier.fillMaxSize())
        PdfOpenResult.Failed -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.open_failed))
        }
        is PdfOpenResult.Ready -> {
            val renderer = result.renderer
            val twoPage = rememberTwoPage()
            val spreadList = remember(renderer.pageCount, twoPage) {
                spreads(renderer.pageCount, twoPage = twoPage, coverAlone = true)
            }
            var keptPage by rememberSaveable(key) {
                mutableIntStateOf(state.page.coerceIn(1, renderer.pageCount.coerceAtLeast(1)))
            }
            var direction by rememberSaveable(
                key,
                stateSaver = ReadingDirectionSaver,
            ) {
                mutableStateOf(state.direction)
            }
            LaunchedEffect(state.direction) { direction = state.direction }
            if (!hasReadableSpreads(spreadList)) {
                Box(Modifier.fillMaxSize())
            } else {
                val pagerState = rememberPagerState(
                    initialPage = spreadIndexOf(spreadList, keptPage),
                    pageCount = { spreadList.size },
                )
                LaunchedEffect(pagerState.settledPage) {
                    keptPageFromSettle(spreadList, pagerState.settledPage)?.let {
                        keptPage = it
                    }
                }
                LaunchedEffect(spreadList) {
                    val index = spreadIndexOf(spreadList, keptPage)
                    if (pagerState.currentPage != index) {
                        pagerState.scrollToPage(index)
                    }
                }
                LaunchedEffect(keptPage, direction) {
                    store.save(
                        key,
                        state.copy(
                            page = keptPage,
                            pageCount = renderer.pageCount,
                            direction = direction,
                            openedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                ReaderScreen(
                    renderer = renderer,
                    pagerState = pagerState,
                    spreads = spreadList,
                    title = titleOf(book.name),
                    pageCount = renderer.pageCount,
                    currentPage = keptPage,
                    twoPage = twoPage,
                    onBack = onBack,
                    onDirectionChange = { direction = it },
                    direction = direction,
                    volumeDelta = volumeDelta,
                )
            }
        }
    }
}

private sealed class PdfOpenResult {
    data object Loading : PdfOpenResult()
    data object Failed : PdfOpenResult()
    data class Ready(val renderer: PageRenderer) : PdfOpenResult()
}
