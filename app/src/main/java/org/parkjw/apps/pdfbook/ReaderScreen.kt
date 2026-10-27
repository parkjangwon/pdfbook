package org.parkjw.apps.pdfbook

import android.database.ContentObserver
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import org.parkjw.apps.pdfbook.settings.PageTurnAnimation
import androidx.compose.ui.platform.LocalView
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookReaderSlider
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderScrim
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingTopBarPadding
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    renderer: PageBitmaps,
    pagerState: PagerState,
    spreads: List<List<Int>>,
    title: String,
    pageCount: Int,
    currentPage: Int,
    twoPage: Boolean,
    onBack: () -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    modifier: Modifier = Modifier,
    direction: ReadingDirection = ReadingDirection.RTL,
    volumeDelta: Flow<Int>? = null,
    overlayVisible: Boolean = false,
    showTapZoneGuide: Boolean = true,
    tapZoneSidePercent: Int = DEFAULT_TAP_ZONE_SIDE_PERCENT,
    onTapZoneSidePercentChange: (Int) -> Unit = {},
    tapZoneWidthSheetVisible: Boolean = false,
    volumes: ReaderVolumes = ReaderVolumes(),
    bookListVisible: Boolean = false,
    showTitleToast: Boolean = false,
    onTitleToastShown: () -> Unit = {},
    endNotice: EndNotice? = null,
    jumpBack: JumpBack? = null,
    keepZoom: Boolean = false,
    followSystemBrightness: Boolean = true,
    brightnessPercent: Int = DEFAULT_READER_BRIGHTNESS_PERCENT,
    onFollowSystemBrightnessChange: (Boolean) -> Unit = {},
    onBrightnessChange: (Int) -> Unit = {},
    brightnessSheetVisible: Boolean = false,
    bookmarkSheetVisible: Boolean = false,
    posture: FoldPosture = FoldPosture.Flat,
    bookmarks: ReaderBookmarks? = null,
    pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.Slide,
    fullScreenReading: Boolean = true,
    keepScreenOn: Boolean = true,
) {
    ReaderBrightnessEffect(followSystemBrightness, brightnessPercent)
    val view = LocalView.current
    DisposableEffect(view, keepScreenOn) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = previous }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ReaderScreenContent(
            renderer = renderer,
            pagerState = pagerState,
            spreads = spreads,
            title = title,
            pageCount = pageCount,
            currentPage = currentPage,
            twoPage = twoPage,
            onBack = onBack,
            onDirectionChange = onDirectionChange,
            modifier = modifier,
            direction = direction,
            volumeDelta = volumeDelta,
            initialOverlayVisible = overlayVisible,
            showTapZoneGuide = showTapZoneGuide,
            tapZoneSidePercent = tapZoneSidePercent,
            onTapZoneSidePercentChange = onTapZoneSidePercentChange,
            initialTapZoneWidthSheetVisible = tapZoneWidthSheetVisible,
            volumes = volumes,
            initialBookListVisible = bookListVisible,
            showTitleToast = showTitleToast,
            onTitleToastShown = onTitleToastShown,
            initialEndNotice = endNotice,
            initialJumpBack = jumpBack,
            keepZoom = keepZoom,
            followSystemBrightness = followSystemBrightness,
            brightnessPercent = brightnessPercent,
            onFollowSystemBrightnessChange = onFollowSystemBrightnessChange,
            onBrightnessChange = onBrightnessChange,
            initialBrightnessSheetVisible = brightnessSheetVisible,
            initialBookmarkSheetVisible = bookmarkSheetVisible,
            posture = posture,
            bookmarks = bookmarks,
            pageTurnAnimation = pageTurnAnimation,
            fullScreenReading = fullScreenReading,
        )
    }
}

@Composable
private fun ReaderScreenContent(
    renderer: PageBitmaps,
    pagerState: PagerState,
    spreads: List<List<Int>>,
    title: String,
    pageCount: Int,
    currentPage: Int,
    twoPage: Boolean,
    onBack: () -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    modifier: Modifier,
    direction: ReadingDirection,
    volumeDelta: Flow<Int>?,
    initialOverlayVisible: Boolean,
    showTapZoneGuide: Boolean,
    tapZoneSidePercent: Int,
    onTapZoneSidePercentChange: (Int) -> Unit,
    initialTapZoneWidthSheetVisible: Boolean,
    volumes: ReaderVolumes,
    initialBookListVisible: Boolean,
    showTitleToast: Boolean,
    onTitleToastShown: () -> Unit,
    initialEndNotice: EndNotice?,
    initialJumpBack: JumpBack?,
    keepZoom: Boolean,
    followSystemBrightness: Boolean,
    brightnessPercent: Int,
    onFollowSystemBrightnessChange: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    initialBrightnessSheetVisible: Boolean,
    initialBookmarkSheetVisible: Boolean,
    posture: FoldPosture,
    bookmarks: ReaderBookmarks?,
    pageTurnAnimation: PageTurnAnimation,
    fullScreenReading: Boolean,
) {
    var bookmarkSheetVisible by rememberSaveable { mutableStateOf(initialBookmarkSheetVisible) }
    var bookListVisible by rememberSaveable { mutableStateOf(initialBookListVisible) }
    val currentSpreads by rememberUpdatedState(spreads)
    val rtl = direction == ReadingDirection.RTL
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(initialOverlayVisible) }
    var widthSheetVisible by remember { mutableStateOf(initialTapZoneWidthSheetVisible) }
    var brightnessSheetVisible by remember { mutableStateOf(initialBrightnessSheetVisible) }
    var sidePercent by remember(tapZoneSidePercent) {
        mutableIntStateOf(normalizeTapZoneSidePercent(tapZoneSidePercent))
    }
    val sideFraction = tapZoneSideFraction(sidePercent)
    var zoom by remember { mutableStateOf(ZoomState()) }
    val zoomRef = rememberUpdatedState(zoom)
    val zoomed = !zoom.allowsPageTurn
    val keepZoomRef = rememberUpdatedState(keepZoom)
    val directionRef = rememberUpdatedState(direction)
    var viewport by remember { mutableStateOf(Size.Zero) }
    val context = LocalContext.current
    fun readAnimatorScale(): Float = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    var animatorScale by remember { mutableFloatStateOf(readAnimatorScale()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animatorScale = readAnimatorScale()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val animOff = animationsOff(animatorScale)
    val instantSwipes = animOff || pageTurnAnimation == PageTurnAnimation.None
    val curlGestures =
        pageTurnAnimation == PageTurnAnimation.Curl && !twoPage && !zoomed && !animOff

    var endNotice by remember { mutableStateOf(initialEndNotice) }
    var endNoticeToken by remember { mutableIntStateOf(0) }
    val currentVolumes by rememberUpdatedState(volumes)
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    var jumpBack by remember(renderer) { mutableStateOf(initialJumpBack) }
    var seekOrigin by remember(renderer) { mutableStateOf<Int?>(null) }
    var seekTarget by remember(renderer) { mutableIntStateOf(0) }
    var pageNavVisible by rememberSaveable { mutableStateOf(false) }
    val outline by produceState(emptyList<OutlineEntry>(), renderer) { value = renderer.outline() }
    var goToPageVisible by remember { mutableStateOf(false) }
    val sheetOpen = widthSheetVisible || pageNavVisible || goToPageVisible || brightnessSheetVisible ||
        (bookListVisible && volumes.bookList != null) ||
        (bookmarkSheetVisible && bookmarks != null)
    val jumpBackShown = jumpBackVisible(jumpBack, endNotice, sheetOpen)

    var curlFront by remember { mutableStateOf<Bitmap?>(null) }
    var curlBack by remember { mutableStateOf<Bitmap?>(null) }
    var curlSideState by remember { mutableStateOf(CurlSide.Right) }
    var curlProgress by remember { mutableFloatStateOf(0f) }
    var curlActive by remember { mutableStateOf(false) }
    var curlEffect by remember { mutableStateOf<CurlEffectState?>(null) }
    var curlJob by remember { mutableStateOf<Job?>(null) }
    val curlProgressAnim = remember { Animatable(0f) }
    var renderSlot by remember { mutableStateOf(IntSize(1, 1)) }
    ReaderSystemBars(
        fullScreenReading = fullScreenReading,
        overlayVisible = overlayVisible || sheetOpen,
    )

    TrimCacheOnMemoryPressure(renderer)

    val postureKey = when (posture) {
        FoldPosture.Flat -> "flat"
        is FoldPosture.Book -> "book:${posture.foldBounds}"
        is FoldPosture.Tabletop -> "tabletop:${posture.foldBounds}"
    }
    LaunchedEffect(postureKey) {
        zoom = ZoomState()
    }

    // Remap shownAt so a posture/spread remount does not dismiss the pill (ZF-3 / SPEC-4).
    LaunchedEffect(spreads) {
        val jb = jumpBack ?: return@LaunchedEffect
        jumpBack = jb.copy(shownAt = spreadIndexOf(spreads, currentPage))
    }

    LaunchedEffect(pagerState.settledPage) {
        zoom = zoom.afterPageTurn(keepZoomRef.value, directionRef.value, viewport.width, viewport.height)
        if (jumpBackHiddenBySettle(jumpBack, pagerState.settledPage)) jumpBack = null
    }

    LaunchedEffect(jumpBack, jumpBackShown) {
        if (!jumpBackShown) return@LaunchedEffect
        delay(JUMP_BACK_MILLIS)
        jumpBack = null
    }

    LaunchedEffect(endNotice, endNoticeToken) {
        val notice = endNotice ?: return@LaunchedEffect
        delay(endNoticeMillis(notice))
        endNotice = null
    }

    fun nextAtEnd() {
        curlJob?.cancel()
        curlActive = false
        curlEffect = null
        curlFront = null
        curlBack = null
        when (val step = endStep(endNotice, currentVolumes.nextBook)) {
            is EndStep.Show -> {
                endNotice = step.notice
                endNoticeToken++
            }
            is EndStep.Open -> {
                endNotice = null
                currentVolumes.onNext?.invoke()
            }
        }
    }

    fun openNextBook() {
        endNotice = null
        currentVolumes.onNext?.invoke()
    }

    fun clearCurlOverlay() {
        curlActive = false
        curlEffect = null
        curlFront = null
        curlBack = null
        curlProgress = 0f
    }

    fun beginCurl(effect: CurlEffectState, front: Bitmap, back: Bitmap) {
        curlSideState = effect.side
        curlFront = front
        curlBack = back
        curlActive = true
        curlProgress = effect.progress
        curlEffect = effect
    }

    fun settlePage(index: Int, mode: ResolvedPageTurn) {
        val off = animationsOff(animatorScale)
        scope.launch {
            when (mode) {
                ResolvedPageTurn.Instant -> pagerState.scrollToPage(index)
                ResolvedPageTurn.Slide -> {
                    if (off) pagerState.scrollToPage(index) else pagerState.animateScrollToPage(index)
                }
                ResolvedPageTurn.Curl -> pagerState.scrollToPage(index)
            }
        }
    }

    suspend fun runCurl(
        fromIndex: Int,
        toIndex: Int,
        side: CurlSide,
        front: Bitmap,
        back: Bitmap,
        fromProgress: Float = 0f,
        animateForward: Boolean = true,
    ) {
        beginCurl(
            CurlEffectState(
                fromIndex = fromIndex,
                toIndex = toIndex,
                side = side,
                progress = fromProgress,
                dragging = false,
                completing = animateForward,
            ),
            front,
            back,
        )
        // Scroll first so TalkBack page announcements are not delayed by the effect.
        if (animateForward) pagerState.scrollToPage(toIndex)
        val duration = curlRemainingDurationMs(animatorScale, fromProgress, animateForward)
        if (duration <= 0L) {
            if (!animateForward) pagerState.scrollToPage(fromIndex)
            clearCurlOverlay()
            return
        }
        curlProgressAnim.snapTo(fromProgress)
        val animTarget = if (animateForward) CURL_CLEAR_PROGRESS else 0f
        curlProgressAnim.animateTo(
            animTarget,
            tween(durationMillis = duration.toInt(), easing = CurlEasing),
        ) {
            curlProgress = value
        }
        if (!animateForward) pagerState.scrollToPage(fromIndex)
        clearCurlOverlay()
    }

    fun startCurl(
        fromIndex: Int,
        toIndex: Int,
        side: CurlSide,
        front: Bitmap,
        back: Bitmap,
        fromProgress: Float = 0f,
        animateForward: Boolean = true,
    ) {
        curlJob?.cancel()
        curlJob = scope.launch {
            runCurl(fromIndex, toIndex, side, front, back, fromProgress, animateForward)
        }
    }

    fun goTo(delta: Int) {
        if (delta < 0) endNotice = null
        curlJob?.cancel()
        curlJob = scope.launch {
            val interrupted = curlEffect
            val liveProgress = curlProgress
            clearCurlOverlay()
            val from = interrupted?.let { curlFinishIndex(it, liveProgress) } ?: pagerState.targetPage
            if (interrupted != null && pagerState.targetPage != from) {
                pagerState.scrollToPage(from)
            }
            val target = pagerTargetIndex(from, delta, pagerState.pageCount)
            if (target == from) {
                if (isTurnPastEnd(from, delta, pagerState.pageCount)) nextAtEnd()
                return@launch
            }
            jumpBack = null
            val forward = delta > 0
            val side = curlSide(directionRef.value, forward)
            val slot = renderSlot
            val slotW = slot.width
            val slotH = slot.height
            val nextSpread = currentSpreads.getOrNull(target).orEmpty()
            val off = animationsOff(animatorScale)
            val cached = isSpreadCached(renderer, nextSpread, slotW, slotH)
            val mode = resolvePageTurn(
                preference = pageTurnAnimation,
                twoPage = twoPage,
                zoomed = zoomRef.value.allowsPageTurn.not(),
                nextPageCached = cached,
                animationsOff = off,
                endNoticeOnly = false,
            )
            if (mode != ResolvedPageTurn.Curl) {
                when (mode) {
                    ResolvedPageTurn.Instant -> pagerState.scrollToPage(target)
                    ResolvedPageTurn.Slide -> {
                        if (off) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
                    }
                    ResolvedPageTurn.Curl -> pagerState.scrollToPage(target)
                }
                return@launch
            }
            val fromSpread = currentSpreads.getOrNull(from).orEmpty()
            val front = cachedSpreadBitmaps(renderer, fromSpread, slotW, slotH)
            val back = cachedSpreadBitmaps(renderer, nextSpread, slotW, slotH)
            if (front == null || back == null) {
                if (off) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
                return@launch
            }
            runCurl(from, target, side, front, back)
        }
    }

    LaunchedEffect(volumeDelta, pagerState) {
        volumeDelta?.collect { delta ->
            goTo(delta)
        }
    }

    fun scrollTo(index: Int) {
        curlJob?.cancel()
        clearCurlOverlay()
        if (pagerState.targetPage != index) {
            scope.launch { pagerState.scrollToPage(index) }
        }
    }

    fun seekPage(page: Int) {
        if (seekOrigin == null) {
            seekOrigin = pagerState.targetPage
            jumpBack = null
        }
        val index = spreadIndexOf(currentSpreads, page)
        seekTarget = index
        scrollTo(index)
    }

    fun seekFinished() {
        val origin = seekOrigin ?: return
        seekOrigin = null
        jumpBack = jumpBackAfterSeek(currentSpreads, origin, seekTarget)
    }

    fun jumpToPage(page: Int) {
        pageNavVisible = false
        val jump = pageJump(currentSpreads, pagerState.targetPage, page)
        jumpBack = jump.jumpBack
        scrollTo(jump.targetIndex)
    }

    fun goToPage(page: Int) {
        goToPageVisible = false
        seekPage(page)
        seekFinished()
    }

    fun jumpBackNow() {
        if (!jumpBackShown) return
        val current = jumpBack ?: return
        val step = jumpBackStep(currentSpreads, current, pagerState.targetPage)
        jumpBack = step.next
        scrollTo(step.targetIndex)
    }

    fun toggleZoom() {
        zoom = zoom.toggleDoubleTap()
    }

    val density = LocalDensity.current
    val foldExclusionPx = with(density) { FOLD_EXCLUSION_DP.dp.roundToPx() }
    val tabletop = posture as? FoldPosture.Tabletop
    val book = posture as? FoldPosture.Book
    val bottomBarShown = overlayVisible && !sheetOpen && tabletop == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_screen")
            .semantics { this[ReaderZoomKey] = zoom }
            .onSizeChanged { viewport = Size(it.width.toFloat(), it.height.toFloat()) }
            .background(ReaderBackground),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val window = IntSize(constraints.maxWidth, constraints.maxHeight)
            val tabletopSplit = tabletop?.let { tabletopHalves(window, it.foldBounds) }
            val pageArea = tabletopSplit?.first
            val padArea = tabletopSplit?.second
            val pagerWidth = (pageArea?.width ?: window.width).toFloat()
            val pagerHeight = (pageArea?.height ?: window.height).toFloat()
            val slot = pageRenderSlot(window, posture, direction, twoPage)
            val curlBounds = curlOverlayBounds(window, posture, direction, twoPage)
            val prerenderWidth = slot.width
            val prerenderHeight = slot.height
            renderSlot = slot
            LaunchedEffect(renderer, pagerState.settledPage, currentSpreads, prerenderWidth, prerenderHeight) {
                renderer.prerender(
                    spreadRequests(
                        currentSpreads.getOrNull(pagerState.settledPage).orEmpty(),
                        prerenderWidth,
                        prerenderHeight,
                    ),
                    prerenderTargets(currentSpreads, pagerState.settledPage, prerenderWidth, prerenderHeight),
                )
            }
            Box(
                Modifier
                    .then(
                        if (pageArea != null) {
                            Modifier
                                .offset { IntOffset(pageArea.left, pageArea.top) }
                                .width(with(density) { pageArea.width.toDp() })
                                .height(with(density) { pageArea.height.toDp() })
                        } else {
                            Modifier.fillMaxSize()
                        },
                    )
                    .testTag("reader_page_area"),
            ) {
                HorizontalPager(
                    state = pagerState,
                    reverseLayout = rtl,
                    // Curl / None / animator-off own the swipe; pager slide only for Slide+animations.
                    userScrollEnabled = !zoomed && !curlGestures && !instantSwipes && !curlActive,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { pageIndex ->
                    val spread = currentSpreads.getOrNull(pageIndex) ?: return@HorizontalPager
                    val layout = spreadLayout(spread, direction)
                    val activePage = pageIndex == pagerState.settledPage
                    val lastActive = activePage && pageIndex == currentSpreads.lastIndex
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(pagerWidth, pagerHeight, activePage) {
                                if (!activePage) return@pointerInput
                                detectReaderZoomGestures(
                                    isZoomed = { !zoom.allowsPageTurn },
                                    onGesture = { centroid, zoomChange, pan ->
                                        zoom = zoom.applyGesture(
                                            zoomChange,
                                            centroid.x,
                                            centroid.y,
                                            pan.x,
                                            pan.y,
                                            pagerWidth,
                                            pagerHeight,
                                        )
                                    },
                                )
                            }
                            .pointerInput(lastActive, direction, curlGestures, instantSwipes) {
                                if (!lastActive || curlGestures || instantSwipes) return@pointerInput
                                detectSwipePastEnd(
                                    direction = direction,
                                    isZoomed = { !zoomRef.value.allowsPageTurn },
                                    onSwipe = { if (pagerState.targetPage == pageIndex) nextAtEnd() },
                                )
                            }
                            .pointerInput(activePage, instantSwipes, direction, pagerWidth) {
                                if (!activePage || !instantSwipes || zoomed) return@pointerInput
                                detectCurlSwipe(
                                    direction = direction,
                                    width = pagerWidth,
                                    currentIndex = { pagerState.targetPage },
                                    pageCount = { pagerState.pageCount },
                                    onDragStart = { _, forward ->
                                        if (!forward) endNotice = null
                                        false
                                    },
                                    onDrag = { _, _ -> },
                                    onRelease = { _, from, targetIndex, _, _, forward, completes ->
                                        if (!forward) endNotice = null
                                        if (completes) {
                                            if (targetIndex != from) {
                                                scope.launch { pagerState.scrollToPage(targetIndex) }
                                            } else if (forward &&
                                                isTurnPastEnd(from, 1, pagerState.pageCount)
                                            ) {
                                                nextAtEnd()
                                            }
                                        }
                                    },
                                    onSwipePastEnd = { nextAtEnd() },
                                )
                            }
                            .pointerInput(activePage, curlGestures, direction, pagerWidth, prerenderWidth, prerenderHeight) {
                                if (!activePage || !curlGestures) return@pointerInput
                                detectCurlSwipe(
                                    direction = direction,
                                    width = pagerWidth,
                                    currentIndex = { pagerState.targetPage },
                                    pageCount = { pagerState.pageCount },
                                    onDragStart = { targetIndex, forward ->
                                        if (!forward) endNotice = null
                                        val slotW = renderSlot.width
                                        val slotH = renderSlot.height
                                        val from = pagerState.targetPage
                                        val fromBmp = cachedSpreadBitmaps(
                                            renderer,
                                            currentSpreads.getOrNull(from).orEmpty(),
                                            slotW,
                                            slotH,
                                        )
                                        val toBmp = cachedSpreadBitmaps(
                                            renderer,
                                            currentSpreads.getOrNull(targetIndex).orEmpty(),
                                            slotW,
                                            slotH,
                                        )
                                        if (fromBmp == null || toBmp == null) {
                                            false
                                        } else {
                                            curlJob?.cancel()
                                            beginCurl(
                                                CurlEffectState(
                                                    fromIndex = from,
                                                    toIndex = targetIndex,
                                                    side = curlSide(direction, forward),
                                                    progress = 0f,
                                                    dragging = true,
                                                    completing = true,
                                                ),
                                                fromBmp,
                                                toBmp,
                                            )
                                            jumpBack = null
                                            true
                                        }
                                    },
                                    onDrag = { progress, side ->
                                        curlSideState = side
                                        curlProgress = progress
                                    },
                                    onRelease = { progress, from, targetIndex, side, startedCurl, forward, completes ->
                                        if (!forward) endNotice = null
                                        if (!startedCurl) {
                                            // Uncached: finger-follow via pager, then settle.
                                            if (pagerState.targetPage != from) {
                                                return@detectCurlSwipe
                                            }
                                            if (completes) {
                                                if (targetIndex != from) {
                                                    settlePage(targetIndex, ResolvedPageTurn.Slide)
                                                } else if (forward &&
                                                    isTurnPastEnd(from, 1, pagerState.pageCount)
                                                ) {
                                                    nextAtEnd()
                                                }
                                            } else if (targetIndex != from) {
                                                settlePage(from, ResolvedPageTurn.Slide)
                                            }
                                            return@detectCurlSwipe
                                        }
                                        // An interrupt already finished this effect: drop the stale release.
                                        val effect = curlEffect
                                        if (effect == null || !effect.dragging ||
                                            effect.fromIndex != from || effect.toIndex != targetIndex
                                        ) {
                                            return@detectCurlSwipe
                                        }
                                        val front = curlFront
                                        val back = curlBack
                                        if (front == null || back == null) {
                                            clearCurlOverlay()
                                            if (pagerState.targetPage == from &&
                                                completes &&
                                                targetIndex != from
                                            ) {
                                                settlePage(targetIndex, ResolvedPageTurn.Slide)
                                            }
                                            return@detectCurlSwipe
                                        }
                                        startCurl(
                                            fromIndex = from,
                                            toIndex = targetIndex,
                                            side = side,
                                            front = front,
                                            back = back,
                                            fromProgress = progress,
                                            animateForward = completes,
                                        )
                                    },
                                    onSwipePastEnd = { nextAtEnd() },
                                    onSlideFollow = { from, _, progress, forward ->
                                        val offset = curlSlideOffset(progress, forward)
                                        scope.launch {
                                            pagerState.scrollToPage(from, pageOffsetFraction = offset)
                                        }
                                    },
                                    fling = true,
                                )
                            },
                    ) {
                        BoxWithConstraints(
                            Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .graphicsLayer {
                                    if (activePage) {
                                        scaleX = zoom.scale
                                        scaleY = zoom.scale
                                        translationX = zoom.offsetX
                                        translationY = zoom.offsetY
                                    }
                                },
                        ) {
                            SpreadPageLayout(
                                renderer = renderer,
                                layout = layout,
                                posture = posture,
                                direction = direction,
                                window = IntSize(constraints.maxWidth, constraints.maxHeight),
                                tagged = activePage,
                            )
                        }
                        Row(Modifier.fillMaxSize()) {
                            tapZoneSpans(direction, sideFraction).forEach { span ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val onTap = {
                                    if (span.zone == TapZone.Menu) {
                                        overlayVisible = !overlayVisible
                                    } else {
                                        goTo(span.zone.pageDelta)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(span.width)
                                        .fillMaxHeight()
                                        // Offscreen pages are pre-composed but invisible; letting them
                                        // take focus makes bringIntoView scroll away when the go-to-page
                                        // dialog releases focus.
                                        .focusProperties { canFocus = activePage }
                                        .testTag("reader_zone_" + span.zone.name)
                                        .then(
                                            if (zoomed || span.zone.acceptsDoubleTapAtBaseScale) {
                                                Modifier.combinedClickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                                    onClick = onTap,
                                                    onDoubleClick = { toggleZoom() },
                                                )
                                            } else {
                                                Modifier.clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                                    onClick = onTap,
                                                )
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }
                val frontBmp = curlFront
                val backBmp = curlBack
                if (curlActive && frontBmp != null && backBmp != null) {
                    // Book single-page draws into one half; the overlay must cover
                    // that same rect or the page jumps across the hinge mid-turn.
                    val overlayModifier = if (curlBounds != null) {
                        Modifier
                            .offset { IntOffset(curlBounds.left, curlBounds.top) }
                            .width(with(density) { curlBounds.width.toDp() })
                            .height(with(density) { curlBounds.height.toDp() })
                    } else {
                        Modifier.fillMaxSize()
                    }
                    PageCurlOverlay(
                        front = frontBmp,
                        back = backBmp,
                        progress = { curlProgress },
                        side = curlSideState,
                        modifier = overlayModifier.testTag("reader_page_curl"),
                    )
                }
            }
            if (padArea != null) {
                TabletopPads(
                    direction = direction,
                    currentPage = currentPage,
                    pageCount = pageCount,
                    onPrevious = { goTo(TapZone.Previous.pageDelta) },
                    onMenu = { overlayVisible = !overlayVisible },
                    onNext = { goTo(TapZone.Next.pageDelta) },
                    slider = if (overlayVisible && !sheetOpen) {
                        {
                            TabletopSlider(
                                pageCount = pageCount,
                                currentPage = currentPage,
                                direction = direction,
                                onSeekPage = ::seekPage,
                                onSeekFinished = ::seekFinished,
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .offset { IntOffset(padArea.left, padArea.top) }
                        .width(with(density) { padArea.width.toDp() })
                        .height(with(density) { padArea.height.toDp() }),
                )
            }
        }

        if (widthSheetVisible || (overlayVisible && showTapZoneGuide && !zoomed && tabletop == null)) {
            TapZoneGuide(
                direction = direction,
                sideFraction = sideFraction,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (widthSheetVisible) {
            BackHandler { widthSheetVisible = false }
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { widthSheetVisible = false },
                    ),
            )
            TapZoneWidthSheet(
                percent = sidePercent,
                onPercentChange = { value ->
                    if (value != sidePercent) {
                        sidePercent = value
                        onTapZoneSidePercentChange(value)
                    }
                },
                onDone = { widthSheetVisible = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (pageNavVisible) {
            BackHandler { pageNavVisible = false }
            PageNavSheet(
                renderer = renderer,
                outline = outline,
                pageCount = pageCount,
                currentPage = currentPage,
                onPick = ::jumpToPage,
                onDismiss = { pageNavVisible = false },
            )
        } else if (brightnessSheetVisible) {
            BackHandler { brightnessSheetVisible = false }
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { brightnessSheetVisible = false },
                    ),
            )
            ReaderBrightnessSheet(
                followSystem = followSystemBrightness,
                percent = brightnessPercent,
                onFollowSystemChange = onFollowSystemBrightnessChange,
                onPercentChange = onBrightnessChange,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (bookmarkSheetVisible && bookmarks != null) {
            BackHandler { bookmarkSheetVisible = false }
            BookmarkSheet(
                pages = bookmarks.pages,
                onPick = { page ->
                    bookmarkSheetVisible = false
                    jumpBack = null
                    scrollTo(spreadIndexOf(currentSpreads, page))
                },
                onDelete = bookmarks.onToggle,
                onDismiss = { bookmarkSheetVisible = false },
            )
        } else if (bookListVisible && volumes.bookList != null) {
            BackHandler { bookListVisible = false }
            BookListSheet(
                folderName = volumes.bookList.folderName,
                entries = volumes.bookList.entries,
                covers = volumes.bookList.covers,
                onPick = { book ->
                    bookListVisible = false
                    volumes.onPick(book)
                },
                onDismiss = { bookListVisible = false },
            )
        } else if (overlayVisible) {
            val currentSpread = currentSpreads.getOrElse(spreadIndexOf(currentSpreads, currentPage)) { listOf(currentPage) }
            ReaderOverlayTop(
                title = title,
                twoPage = twoPage,
                direction = direction,
                onBack = onBack,
                onDirectionChange = onDirectionChange,
                showPageMode = tabletop == null,
                onOpenBookList = volumes.bookList?.let { { bookListVisible = true } },
                onOpenPageNav = { pageNavVisible = true },
                bookmarked = bookmarks?.let { spreadBookmarked(it.pages, currentSpread) },
                onToggleBookmark = { bookmarks?.let { b -> spreadToggleTargets(b.pages, currentSpread).forEach(b.onToggle) } },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(
                        if (tabletop != null) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                        },
                    ),
            )
            if (tabletop == null) {
                val bookRight = book?.let {
                    bookHalves(
                        IntSize(viewport.width.roundToInt().coerceAtLeast(1), viewport.height.roundToInt().coerceAtLeast(1)),
                        it.foldBounds,
                    ).second
                }
                ReaderOverlayBottom(
                    pageCount = pageCount,
                    currentPage = currentPage,
                    direction = direction,
                    onSeekPage = ::seekPage,
                    onSeekFinished = ::seekFinished,
                    onOpenGoToPage = { goToPageVisible = true },
                    showTapZoneEntry = !zoomed,
                    onAdjustTapZones = {
                        zoom = ZoomState()
                        widthSheetVisible = true
                    },
                    onOpenBrightness = { brightnessSheetVisible = true },
                    onOpenBookmarks = bookmarks?.let { { bookmarkSheetVisible = true } },
                    onPreviousBook = volumes.onPrevious,
                    onNextBook = volumes.onNext,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .then(
                            if (bookRight != null && bookRight.width > foldExclusionPx) {
                                Modifier
                                    .offset { IntOffset(bookRight.left + foldExclusionPx, 0) }
                                    .width(with(density) {
                                        (bookRight.width - foldExclusionPx).coerceAtLeast(0).toDp()
                                    })
                            } else {
                                Modifier.fillMaxWidth()
                            },
                        )
                        .onSizeChanged { bottomBarHeight = it.height },
                )
            }
        }
        LaunchedEffect(overlayVisible, sheetOpen) {
            if (!overlayVisible || sheetOpen) seekFinished()
        }
        val noticeInset = noticeBottomInset(
            bottomBarShown,
            with(density) { bottomBarHeight.toDp() },
        )
        EndNoticeHost(
            notice = endNotice,
            twoPage = twoPage,
            direction = direction,
            bottomInset = noticeInset,
            onOpen = ::openNextBook,
        )
        JumpBackHost(
            page = if (jumpBackShown) jumpBack?.page else null,
            bottomInset = noticeInset,
            onClick = ::jumpBackNow,
        )
        if (showTitleToast) {
            BookChangedToast(
                title = title,
                onShown = onTitleToastShown,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (goToPageVisible) {
            BackHandler { goToPageVisible = false }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReaderScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { goToPageVisible = false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GoToPageDialog(
                    pageCount = pageCount,
                    onDismiss = { goToPageVisible = false },
                    onGo = ::goToPage,
                )
            }
        }
    }
}

@Composable
private fun SpreadPageLayout(
    renderer: PageBitmaps,
    layout: List<Int>,
    posture: FoldPosture,
    direction: ReadingDirection,
    window: IntSize,
    tagged: Boolean,
) {
    val book = posture as? FoldPosture.Book
    if (book != null) {
        val halves = bookHalves(window, book.foldBounds)
        Box(Modifier.fillMaxSize().background(ReaderBackground)) {
            Box(
                Modifier
                    .offset { IntOffset(book.foldBounds.left, book.foldBounds.top) }
                    .width(with(LocalDensity.current) { book.foldBounds.width.toDp() })
                    .height(with(LocalDensity.current) { book.foldBounds.height.toDp() })
                    .then(if (tagged) Modifier.testTag("book_posture_fold_gap") else Modifier),
            )
            if (layout.size >= 2) {
                BookHalfPage(
                    renderer = renderer,
                    page = layout[0],
                    half = halves.first,
                    alignment = Alignment.CenterEnd,
                    tagged = tagged,
                )
                BookHalfPage(
                    renderer = renderer,
                    page = layout[1],
                    half = halves.second,
                    alignment = Alignment.CenterStart,
                    tagged = tagged,
                )
            } else {
                val half = bookSingleHalf(halves, direction)
                BookHalfPage(
                    renderer = renderer,
                    page = layout.first(),
                    half = half,
                    alignment = Alignment.Center,
                    tagged = tagged,
                )
            }
        }
        return
    }
    val pageSlotWidth = (window.width / layout.size.coerceAtLeast(1)).coerceAtLeast(1)
    val pageSlotHeight = window.height.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        layout.forEachIndexed { index, page ->
            val pageAlignment = when {
                layout.size == 1 -> Alignment.Center
                index == 0 -> Alignment.CenterEnd
                else -> Alignment.CenterStart
            }
            PageBitmap(
                renderer = renderer,
                page = page,
                maxWidth = pageSlotWidth,
                maxHeight = pageSlotHeight,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (tagged) Modifier.testTag("reader_page_$page") else Modifier),
                alignment = pageAlignment,
            )
        }
    }
}

@Composable
private fun BookHalfPage(
    renderer: PageBitmaps,
    page: Int,
    half: IntRect,
    alignment: Alignment,
    tagged: Boolean,
) {
    val density = LocalDensity.current
    if (half.width <= 0 || half.height <= 0) return
    Box(
        Modifier
            .offset { IntOffset(half.left, half.top) }
            .width(with(density) { half.width.toDp() })
            .height(with(density) { half.height.toDp() })
            .then(if (tagged) Modifier.testTag("reader_page_$page") else Modifier),
    ) {
        PageBitmap(
            renderer = renderer,
            page = page,
            maxWidth = half.width,
            maxHeight = half.height,
            modifier = Modifier.fillMaxSize(),
            alignment = alignment,
        )
    }
}

@Composable
private fun TabletopSlider(
    pageCount: Int,
    currentPage: Int,
    direction: ReadingDirection,
    onSeekPage: (Int) -> Unit,
    onSeekFinished: () -> Unit,
) {
    if (pageCount <= 1) return
    val sliderDirection =
        if (direction == ReadingDirection.RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides sliderDirection) {
        PdfBookReaderSlider(
            value = pageToSlider(currentPage, pageCount, ReadingDirection.LTR),
            onValueChange = { onSeekPage(sliderToPage(it, pageCount, ReadingDirection.LTR)) },
            onValueChangeFinished = onSeekFinished,
            valueRange = 0f..(pageCount - 1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = ReaderAccent,
                activeTrackColor = ReaderAccent,
                inactiveTrackColor = ReaderTrack,
                activeTickColor = ReaderAccent,
                inactiveTickColor = ReaderTrack,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reader_slider"),
        )
    }
}

@Composable
private fun BookChangedToast(title: String, onShown: () -> Unit, modifier: Modifier = Modifier) {
    val currentOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(title) {
        delay(BOOK_CHANGED_TOAST_MILLIS)
        currentOnShown()
    }
    Text(
        text = stringResource(R.string.reader_book_changed, title),
        color = ReaderOnOverlay,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .testTag("reader_book_changed_toast")
            .padding(horizontal = 32.dp)
            .background(ReaderOverlay, PdfBookShapeTokens.pill)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun VolumeButton(
    iconRes: Int,
    labelRes: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier,
        colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(labelRes),
        )
    }
}

@Composable
private fun ReaderOverlayTop(
    title: String,
    twoPage: Boolean,
    direction: ReadingDirection,
    onBack: () -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    showPageMode: Boolean = true,
    modifier: Modifier = Modifier,
    onOpenBookList: (() -> Unit)? = null,
    onOpenPageNav: () -> Unit = {},
    bookmarked: Boolean? = null,
    onToggleBookmark: () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .testTag("reader_overlay_top")
            .background(ReaderOverlay)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .safeDrawingTopBarPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
            modifier = Modifier.testTag("reader_back"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.reader_back),
                tint = ReaderOnOverlay,
            )
        }
        Text(
            text = title,
            color = ReaderOnOverlay,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (bookmarked != null) {
            IconButton(
                onClick = onToggleBookmark,
                colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                modifier = Modifier
                    .testTag("reader_bookmark_toggle")
                    .semantics {
                        role = Role.Checkbox
                        toggleableState = ToggleableState(bookmarked)
                    },
            ) {
                Icon(
                    painter = painterResource(if (bookmarked) R.drawable.ic_bookmark_on else R.drawable.ic_bookmark),
                    contentDescription = stringResource(
                        if (bookmarked) R.string.reader_bookmark_remove else R.string.reader_bookmark_add,
                    ),
                    tint = ReaderOnOverlay,
                )
            }
        }
        IconButton(
            onClick = onOpenPageNav,
            colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
            modifier = Modifier.testTag("reader_page_nav_button"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_page_nav),
                contentDescription = stringResource(R.string.reader_page_nav),
                tint = ReaderOnOverlay,
            )
        }
        if (onOpenBookList != null) {
            IconButton(
                onClick = onOpenBookList,
                colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                modifier = Modifier.testTag("reader_book_list_button"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_list),
                    contentDescription = stringResource(R.string.reader_book_list),
                    tint = ReaderOnOverlay,
                )
            }
        }
        if (showPageMode) {
            Icon(
                painter = painterResource(
                    if (twoPage) R.drawable.ic_page_spread else R.drawable.ic_page_single,
                ),
                contentDescription = stringResource(
                    if (twoPage) R.string.reader_mode_double else R.string.reader_mode_single,
                ),
                tint = ReaderOnOverlay,
                modifier = Modifier.padding(horizontal = 12.dp).testTag("reader_page_mode"),
            )
        }
        IconButton(
            onClick = { onDirectionChange(toggledDirection(direction)) },
            colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
            modifier = Modifier.testTag("reader_direction_toggle"),
        ) {
            Icon(
                painter = painterResource(
                    if (direction == ReadingDirection.RTL) {
                        R.drawable.ic_read_rtl
                    } else {
                        R.drawable.ic_read_ltr
                    },
                ),
                contentDescription = stringResource(
                    if (direction == ReadingDirection.RTL) {
                        R.string.settings_direction_rtl
                    } else {
                        R.string.settings_direction_ltr
                    },
                ),
                tint = ReaderOnOverlay,
            )
        }
    }
}

@Composable
private fun ReaderOverlayBottom(
    pageCount: Int,
    currentPage: Int,
    direction: ReadingDirection,
    onSeekPage: (Int) -> Unit,
    onSeekFinished: () -> Unit,
    onOpenGoToPage: () -> Unit,
    showTapZoneEntry: Boolean,
    onAdjustTapZones: () -> Unit,
    onOpenBrightness: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenBookmarks: (() -> Unit)? = null,
    onPreviousBook: (() -> Unit)? = null,
    onNextBook: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("reader_overlay_bottom")
            .background(ReaderOverlay)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .safeDrawingBottomBarPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showTapZoneEntry) {
            TapZoneEntry(onClick = onAdjustTapZones)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val rtlBook = direction == ReadingDirection.RTL
            val leftVolume = volumeButtonAt(VolumeEnd.Left, rtlBook)
            VolumeButton(
                R.drawable.ic_prev_book,
                leftVolume.labelRes,
                if (leftVolume == VolumeStep.Next) onNextBook else onPreviousBook,
                Modifier.testTag("reader_volume_left"),
            )
            if (pageCount > 1) {
                val sliderDirection =
                    if (direction == ReadingDirection.RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides sliderDirection) {
                    PdfBookReaderSlider(
                        value = pageToSlider(currentPage, pageCount, ReadingDirection.LTR),
                        onValueChange = { onSeekPage(sliderToPage(it, pageCount, ReadingDirection.LTR)) },
                        onValueChangeFinished = onSeekFinished,
                        valueRange = 0f..(pageCount - 1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = ReaderAccent,
                            activeTrackColor = ReaderAccent,
                            inactiveTrackColor = ReaderTrack,
                            activeTickColor = ReaderAccent,
                            inactiveTickColor = ReaderTrack,
                        ),
                        modifier = Modifier.weight(1f).testTag("reader_slider"),
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            val rightVolume = volumeButtonAt(VolumeEnd.Right, rtlBook)
            VolumeButton(
                R.drawable.ic_next_book,
                rightVolume.labelRes,
                if (rightVolume == VolumeStep.Next) onNextBook else onPreviousBook,
                Modifier.testTag("reader_volume_right"),
            )
        }
        // Under the slider (above page number): keeps |< slider >| mirrored pair intact;
        // tap-zone entry stays above the slider as before.
        ReaderToolRow(
            items = buildList {
                add(
                    ReaderToolItem.Action(
                        tool = ReaderTool.Brightness,
                        iconRes = R.drawable.ic_brightness,
                        label = stringResource(R.string.reader_brightness),
                        testTag = "reader_brightness",
                        onClick = onOpenBrightness,
                    ),
                )
                if (onOpenBookmarks != null) {
                    add(
                        ReaderToolItem.Action(
                            tool = ReaderTool.Bookmarks,
                            iconRes = R.drawable.ic_bookmark_list,
                            label = stringResource(R.string.reader_bookmark_list),
                            testTag = "reader_bookmark_list_button",
                            onClick = onOpenBookmarks,
                        ),
                    )
                }
            },
        )
        val goToPageLabel = stringResource(R.string.go_to_page_title)
        Text(
            text = stringResource(R.string.progress, currentPage, pageCount),
            style = PdfBookNumeric.copy(fontSize = 13.sp, color = ReaderOnOverlay),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(
                    role = Role.Button,
                    onClickLabel = goToPageLabel,
                    onClick = onOpenGoToPage,
                )
                .padding(horizontal = 16.dp, vertical = 22.dp)
                .testTag("reader_progress"),
        )
    }
}

@Composable
private fun TapZoneEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reader_tap_zone_entry")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reader_tap_zone_label),
            color = ReaderOnOverlayVariant,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.reader_tap_zone_adjust),
            color = ReaderOnOverlay,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
internal fun PageBitmap(
    renderer: PageBitmaps,
    page: Int,
    maxWidth: Int,
    maxHeight: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    var bitmap by remember(renderer, page, maxWidth, maxHeight) {
        mutableStateOf(renderer.cached(page, maxWidth, maxHeight))
    }
    LaunchedEffect(renderer, page, maxWidth, maxHeight) {
        if (bitmap?.isRecycled != false) {
            bitmap = renderer.render(page, maxWidth, maxHeight)
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = alignment,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private suspend fun PointerInputScope.detectCurlSwipe(
    direction: ReadingDirection,
    width: Float,
    currentIndex: () -> Int,
    pageCount: () -> Int,
    onDragStart: (targetIndex: Int, forward: Boolean) -> Boolean,
    onDrag: (progress: Float, side: CurlSide) -> Unit,
    onRelease: (
        progress: Float,
        fromIndex: Int,
        targetIndex: Int,
        side: CurlSide,
        startedCurl: Boolean,
        forward: Boolean,
        completes: Boolean,
    ) -> Unit,
    onSwipePastEnd: () -> Unit,
    onSlideFollow: ((from: Int, target: Int, progress: Float, forward: Boolean) -> Unit)? = null,
    fling: Boolean = false,
) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val velocityTracker = VelocityTracker()
        velocityTracker.addPosition(down.uptimeMillis, down.position)
        var drag = false
        var startedCurl = false
        var side = CurlSide.Right
        var fromIndex = currentIndex()
        var targetIndex = fromIndex
        var forward = true
        var progress = 0f
        var followOffset = 0f
        var last = down.position
        var pressed = true
        while (pressed) {
            val event = awaitPointerEvent()
            pressed = event.changes.any { it.pressed }
            if (event.changes.count { it.pressed } > 1) {
                // A second pointer ends the turn cleanly: settle to the nearest
                // state so the overlay is never left half-turned, then stop
                // consuming so pinch zoom can start.
                if (drag && (startedCurl || targetIndex != fromIndex)) {
                    onRelease(
                        progress,
                        fromIndex,
                        targetIndex,
                        side,
                        startedCurl,
                        forward,
                        curlSettleCompletes(progress),
                    )
                }
                return@awaitEachGesture
            }
            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
            last = change.position
            // Only moving samples: a still release sample would bend the fit to zero.
            if (change.pressed) velocityTracker.addPosition(change.uptimeMillis, change.position)
            val dx = last.x - down.position.x
            val dy = last.y - down.position.y
            if (!drag && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                drag = true
                forward = if (direction == ReadingDirection.RTL) dx > 0f else dx < 0f
                side = curlSide(direction, forward)
                fromIndex = currentIndex()
                targetIndex = pagerTargetIndex(fromIndex, if (forward) 1 else -1, pageCount())
                if (targetIndex != fromIndex) {
                    startedCurl = onDragStart(targetIndex, forward)
                    if (startedCurl) onDrag(0f, side)
                }
            }
            if (drag) {
                val raw = curlProgressFromDrag(dx, width, side)
                val following = !startedCurl && targetIndex != fromIndex && onSlideFollow != null
                progress = if (following) curlFollowProgress(raw, followOffset) else raw
                when {
                    startedCurl -> onDrag(progress, side)
                    following && pressed -> {
                        onSlideFollow(fromIndex, targetIndex, progress, forward)
                        followOffset = curlSlideOffset(progress, forward)
                    }
                }
                if (startedCurl || targetIndex != fromIndex) change.consume()
            }
        }
        if (!drag) return@awaitEachGesture
        if (targetIndex == fromIndex && forward &&
            isTurnPastEnd(fromIndex, 1, pageCount())
        ) {
            onSwipePastEnd()
            return@awaitEachGesture
        }
        val velocity = velocityTracker.calculateVelocity().x
        val alongVelocity = curlAlongVelocity(velocity, side)
        val completes = if (fling) {
            curlReleaseCompletes(
                progress = progress,
                alongVelocityPx = alongVelocity,
                flingVelocityPx = CURL_FLING_DP_PER_SECOND.dp.toPx(),
            )
        } else {
            shouldCompleteCurl(progress)
        }
        onRelease(progress, fromIndex, targetIndex, side, startedCurl, forward, completes)
    }
}

private suspend fun PointerInputScope.detectSwipePastEnd(
    direction: ReadingDirection,
    isZoomed: () -> Boolean,
    onSwipe: () -> Unit,
) {
    val threshold = END_SWIPE_THRESHOLD.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var multiTouch = false
        var last = down.position
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } > 1) multiTouch = true
            event.changes.firstOrNull { it.id == down.id }?.let { last = it.position }
        } while (event.changes.any { it.pressed })
        if (multiTouch || isZoomed()) return@awaitEachGesture
        val delta = last - down.position
        if (isSwipeTowardNext(delta.x, delta.y, threshold, direction)) onSwipe()
    }
}

private suspend fun PointerInputScope.detectReaderZoomGestures(
    isZoomed: () -> Boolean,
    onGesture: (centroid: Offset, zoomChange: Float, pan: Offset) -> Unit,
) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var panPastSlop = false
        var panDistance = 0f
        var pinch = PinchLatchState()
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            when {
                pressed >= 2 -> {
                    val zoomChange = event.calculateZoom()
                    val centroid = event.calculateCentroid()
                    val pan = event.calculatePan()
                    pinch = pinch.advance(zoomChange)
                    if (pinch.latched || isZoomed()) {
                        onGesture(centroid, zoomChange, pan)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) it.consume()
                    }
                }
                pressed == 1 && isZoomed() -> {
                    val pan = event.calculatePan()
                    panDistance += pan.getDistance()
                    if (!panPastSlop && panDistance >= touchSlop) {
                        panPastSlop = true
                    }
                    if (panPastSlop && pan != Offset.Zero) {
                        onGesture(Offset.Zero, 1f, pan)
                        event.changes.forEach {
                            if (it.positionChanged()) it.consume()
                        }
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

val ReaderZoomKey = SemanticsPropertyKey<ZoomState>("ReaderZoom")
