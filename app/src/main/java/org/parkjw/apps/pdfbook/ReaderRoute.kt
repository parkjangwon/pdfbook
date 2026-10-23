package org.parkjw.apps.pdfbook

import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.parkjw.apps.pdfbook.settings.ScreenOrientation
import org.parkjw.apps.pdfbook.settings.TwoPageMode
import org.parkjw.apps.pdfbook.settings.toActivityOrientation

@Composable
fun OrientationLock(screenOrientation: ScreenOrientation) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(screenOrientation) {
        activity.requestedOrientation = screenOrientation.toActivityOrientation()
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

@Composable
fun ReaderRoute(
    book: BookInfo,
    store: BookStore,
    volumeDelta: Flow<Int>,
    defaultReadingDirection: ReadingDirection,
    onBack: () -> Unit,
    showTapZoneGuide: Boolean = true,
    tapZoneSidePercent: Int = DEFAULT_TAP_ZONE_SIDE_PERCENT,
    onTapZoneSidePercentChange: (Int) -> Unit = {},
    volumes: ReaderVolumes = ReaderVolumes(),
    showTitleToast: Boolean = false,
    onTitleToastShown: () -> Unit = {},
    keepZoom: Boolean = false,
    twoPageMode: TwoPageMode = TwoPageMode.Auto,
    screenOrientation: ScreenOrientation = ScreenOrientation.Auto,
    coverAlone: Boolean = true,
    rendererOverride: PageBitmaps? = null,
    keepScreenOn: Boolean = true,
) {
    OrientationLock(screenOrientation)
    val key = bookKey(book.uri, book.size)
    var loaded by remember(key) { mutableStateOf(false) }
    var saved by remember(key) { mutableStateOf<BookState?>(null) }
    LaunchedEffect(key) {
        saved = store.state(key)
        loaded = true
    }
    if (!loaded) return
    val initialDirection = resolveReadingDirection(saved?.direction, defaultReadingDirection)
    val state = openStateFor(saved, initialDirection)

    val context = LocalContext.current
    val uri = remember(key) { Uri.parse(book.uri) }
    val openResult by produceState<PdfOpenResult>(
        initialValue = rendererOverride?.let { PdfOpenResult.Ready(it) } ?: PdfOpenResult.Loading,
        uri,
        context,
    ) {
        if (rendererOverride != null) return@produceState
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
            val posture = rememberFoldPosture()
            val twoPage = rememberTwoPage(twoPageMode, posture)
            val spreadList = remember(renderer.pageCount, twoPage, coverAlone) {
                spreads(renderer.pageCount, twoPage = twoPage, coverAlone = coverAlone)
            }
            var anchorPage by rememberSaveable(key) {
                mutableIntStateOf(state.page.coerceIn(1, renderer.pageCount.coerceAtLeast(1)))
            }
            var direction by rememberSaveable(
                key,
                stateSaver = ReadingDirectionSaver,
            ) {
                mutableStateOf(initialDirection)
            }
            LaunchedEffect(initialDirection) { direction = initialDirection }
            if (!hasReadableSpreads(spreadList)) {
                Box(Modifier.fillMaxSize())
            } else {
                val pagerState = rememberPagerState(
                    initialPage = spreadIndexOf(spreadList, anchorPage),
                    pageCount = { spreadList.size },
                )
                LaunchedEffect(spreadList) {
                    var sync = readingAnchorBeginSync(ReadingAnchor(anchorPage), spreadList)
                    val index = readingAnchorTargetIndex(spreadList, sync)
                    if (pagerState.currentPage != index) {
                        pagerState.scrollToPage(index)
                    } else {
                        sync = sync.copy(pendingIndex = null)
                    }
                    snapshotFlow { pagerState.settledPage }.collectLatest { settled ->
                        sync = readingAnchorAfterSettle(sync, spreadList, settled)
                        if (sync.page != anchorPage) anchorPage = sync.page
                    }
                }
                LaunchedEffect(anchorPage, direction) {
                    store.save(
                        key,
                        state.copy(
                            page = anchorPage,
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
                    currentPage = anchorPage,
                    twoPage = twoPage,
                    posture = posture,
                    onBack = onBack,
                    onDirectionChange = { direction = it },
                    direction = direction,
                    volumeDelta = volumeDelta,
                    showTapZoneGuide = showTapZoneGuide,
                    tapZoneSidePercent = tapZoneSidePercent,
                    onTapZoneSidePercentChange = onTapZoneSidePercentChange,
                    volumes = volumes,
                    showTitleToast = showTitleToast,
                    onTitleToastShown = onTitleToastShown,
                    keepZoom = keepZoom,
                    keepScreenOn = keepScreenOn,
                )
            }
        }
    }
}

private sealed class PdfOpenResult {
    data object Loading : PdfOpenResult()
    data object Failed : PdfOpenResult()
    data class Ready(val renderer: PageBitmaps) : PdfOpenResult()
}
