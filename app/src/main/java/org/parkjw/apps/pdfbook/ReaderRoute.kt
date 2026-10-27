package org.parkjw.apps.pdfbook

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
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
    oneFingerZoom: Boolean = false,
    volumes: ReaderVolumes = ReaderVolumes(),
    showTitleToast: Boolean = false,
    onTitleToastShown: () -> Unit = {},
    keepZoom: Boolean = false,
    twoPageMode: TwoPageMode = TwoPageMode.Auto,
    followSystemBrightness: Boolean = true,
    brightnessPercent: Int = DEFAULT_READER_BRIGHTNESS_PERCENT,
    onFollowSystemBrightnessChange: (Boolean) -> Unit = {},
    onBrightnessChange: (Int) -> Unit = {},
    pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.Slide,
    screenOrientation: ScreenOrientation = ScreenOrientation.Auto,
    fullScreenReading: Boolean = true,
    displayMode: DisplayMode = DisplayMode.Default,
    coverAlone: Boolean = true,
    rendererOverride: PageBitmaps? = null,
    keepScreenOn: Boolean = true,
) {
    OrientationLock(screenOrientation)
    ReaderSystemBarsScope()
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
        val renderer = openReaderRenderer(context, uri)
        value = if (renderer != null) PdfOpenResult.Ready(renderer) else PdfOpenResult.Failed
        awaitDispose { renderer?.close() }
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
            var spreadShift by rememberSaveable(key) { mutableStateOf(saved?.spreadShift == 1) }
            val spreadList = remember(renderer.pageCount, twoPage, coverAlone, spreadShift) {
                spreads(
                    renderer.pageCount,
                    twoPage = twoPage,
                    coverAlone = coverAlone,
                    shift = if (spreadShift) 1 else 0,
                )
            }
            var anchorPage by rememberSaveable(key) {
                mutableIntStateOf(state.page.coerceIn(1, renderer.pageCount.coerceAtLeast(1)))
            }
            var anchorSync by remember { mutableStateOf(ReadingAnchor(anchorPage)) }
            var direction by rememberSaveable(
                key,
                stateSaver = ReadingDirectionSaver,
            ) {
                mutableStateOf(initialDirection)
            }
            LaunchedEffect(initialDirection) { direction = initialDirection }
            var autoCrop by rememberSaveable(key) { mutableStateOf(saved?.autoCrop == true) }
            if (!hasReadableSpreads(spreadList)) {
                Box(Modifier.fillMaxSize())
            } else {
                val pagerState = rememberPagerState(
                    initialPage = spreadIndexOf(spreadList, anchorPage),
                    pageCount = { spreadList.size },
                )
                LaunchedEffect(spreadList) {
                    anchorSync = readingAnchorBeginSync(ReadingAnchor(anchorPage), spreadList)
                    val index = readingAnchorTargetIndex(spreadList, anchorSync)
                    if (pagerState.currentPage != index) {
                        pagerState.scrollToPage(index)
                    } else {
                        anchorSync = anchorSync.copy(pendingIndex = null)
                    }
                    snapshotFlow { pagerState.settledPage }.collectLatest { settled ->
                        anchorSync = readingAnchorAfterSettle(anchorSync, spreadList, settled)
                        if (anchorSync.page != anchorPage) anchorPage = anchorSync.page
                    }
                }
                LaunchedEffect(anchorPage, direction, autoCrop, spreadShift) {
                    store.save(
                        key,
                        state.copy(
                            page = anchorPage,
                            pageCount = renderer.pageCount,
                            direction = direction,
                            openedAt = System.currentTimeMillis(),
                            autoCrop = autoCrop,
                            spreadShift = if (spreadShift) 1 else 0,
                        ),
                    )
                }
                val pageCount = renderer.pageCount
                val bookmarkFlow = remember(store, key, pageCount) { store.bookmarks(key, pageCount) }
                val bookmarkPages by bookmarkFlow.collectAsState(null)
                val bookmarkScope = rememberCoroutineScope()
                val readerBookmarks = remember(bookmarkPages, key) {
                    bookmarkPages?.let { pages ->
                        ReaderBookmarks(pages) { page -> bookmarkScope.launch { store.toggleBookmark(key, page) } }
                    }
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
                    oneFingerZoom = oneFingerZoom,
                    volumes = volumes,
                    showTitleToast = showTitleToast,
                    onTitleToastShown = onTitleToastShown,
                    keepZoom = keepZoom,
                    autoCrop = autoCrop,
                    onAutoCropChange = { autoCrop = it },
                    followSystemBrightness = followSystemBrightness,
                    brightnessPercent = brightnessPercent,
                    onFollowSystemBrightnessChange = onFollowSystemBrightnessChange,
                    onBrightnessChange = onBrightnessChange,
                    bookmarks = readerBookmarks,
                    pageTurnAnimation = pageTurnAnimation,
                    fullScreenReading = fullScreenReading,
                    displayMode = displayMode,
                    keepScreenOn = keepScreenOn,
                    spreadShift = spreadShift,
                    onSpreadShiftChange = { spreadShift = it },
                    onRestorePage = { page ->
                        anchorPage = page
                        anchorSync = ReadingAnchor(
                            page = page,
                            pendingIndex = spreadIndexOf(spreadList, page),
                        )
                    },
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

internal suspend fun openReaderRenderer(
    context: Context,
    uri: Uri,
    open: (ParcelFileDescriptor) -> PageRenderer = { PageRenderer.open(it) },
): PageRenderer? = withContext(Dispatchers.IO) {
    val renderer = try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
        open(pfd)
    } catch (_: Exception) {
        return@withContext null
    }
    if (isActive) {
        renderer
    } else {
        renderer.close()
        null
    }
}
