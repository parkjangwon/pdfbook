package org.parkjw.apps.pdfbook

import android.database.ContentObserver
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingTopBarPadding
import kotlin.math.abs
import kotlin.math.min
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
    autoCrop: Boolean = false,
    onAutoCropChange: (Boolean) -> Unit = {},
    bookmarks: ReaderBookmarks? = null,
    pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.Slide,
    fullScreenReading: Boolean = true,
    displayMode: DisplayMode = DisplayMode.Default,
    keepScreenOn: Boolean = true,
) {
    val view = LocalView.current
    DisposableEffect(view, keepScreenOn) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = previous }
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalDisplayMode provides displayMode,
    ) {
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
            autoCrop = autoCrop,
            onAutoCropChange = onAutoCropChange,
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
    autoCrop: Boolean,
    onAutoCropChange: (Boolean) -> Unit,
    bookmarks: ReaderBookmarks?,
    pageTurnAnimation: PageTurnAnimation,
    fullScreenReading: Boolean,
) {
    SideEffect { renderer.setAutoCrop(autoCrop) }
    // Slider drags preview the window brightness without a DataStore write; only release persists.
    var brightnessPreview by remember(brightnessPercent) { mutableIntStateOf(brightnessPercent) }
    ReaderBrightnessEffect(followSystemBrightness, brightnessPreview)
    var bookmarkSheetVisible by rememberSaveable { mutableStateOf(initialBookmarkSheetVisible) }
    var bookListVisible by rememberSaveable { mutableStateOf(initialBookListVisible) }
    val currentSpreads by rememberUpdatedState(spreads)
    val rtl = direction == ReadingDirection.RTL
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(initialOverlayVisible) }
    var widthSheetVisible by remember { mutableStateOf(initialTapZoneWidthSheetVisible) }
    var brightnessSheetVisible by remember { mutableStateOf(initialBrightnessSheetVisible) }
    var cropSheetVisible by remember { mutableStateOf(false) }
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
        pageTurnAnimation == PageTurnAnimation.Curl && !zoomed && !animOff

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
        cropSheetVisible ||
        (bookListVisible && volumes.bookList != null) ||
        (bookmarkSheetVisible && bookmarks != null)
    val jumpBackShown = jumpBackVisible(jumpBack, endNotice, sheetOpen)

    var curlFront by remember { mutableStateOf<Bitmap?>(null) }
    var curlBack by remember { mutableStateOf<Bitmap?>(null) }
    var curlTwoPage by remember { mutableStateOf<TwoPageCurl?>(null) }
    var curlSideState by remember { mutableStateOf(CurlSide.Right) }
    var curlProgress by remember { mutableFloatStateOf(0f) }
    var curlActive by remember { mutableStateOf(false) }
    var curlEffect by remember { mutableStateOf<CurlEffectState?>(null) }
    var curlJob by remember { mutableStateOf<Job?>(null) }
    val curlProgressAnim = remember { Animatable(0f) }
    // Drag reference for a two-page curl: the drawn spread width (2w), not the pane.
    val curlDragWidth = remember { mutableFloatStateOf(0f) }
    var renderSlot by remember { mutableStateOf(IntSize(1, 1)) }
    ReaderSystemBars(
        fullScreenReading = fullScreenReading,
        overlayVisible = overlayVisible || sheetOpen,
    )

    TrimCacheOnMemoryPressure(renderer)

    // Backgrounding trims the cache to the current page and cancels the prerender job.
    // Re-request the neighbours as soon as the reader is visible again, otherwise the
    // first turn after resume finds the next page uncached and falls back to the slide.
    // Only a return from a stop counts: ON_START also replays on first composition, before
    // BoxWithConstraints has measured, and a 1x1 prerender would freeze zero crop margins.
    var wasStopped by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { wasStopped = true }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (!wasStopped) return@LifecycleEventEffect
        val slot = renderSlot
        if (slot.width <= 1 || slot.height <= 1) return@LifecycleEventEffect
        wasStopped = false
        renderer.prerender(
            spreadRequests(
                currentSpreads.getOrNull(pagerState.settledPage).orEmpty(),
                slot.width,
                slot.height,
            ),
            prerenderTargets(currentSpreads, pagerState.settledPage, slot.width, slot.height),
        )
    }

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
        curlTwoPage = null
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
        curlTwoPage = null
        curlProgress = 0f
    }

    fun beginCurl(effect: CurlEffectState, front: Bitmap, back: Bitmap, twoPage: TwoPageCurl? = null) {
        curlSideState = effect.side
        curlFront = front
        curlBack = back
        curlTwoPage = twoPage
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
        twoPage: TwoPageCurl? = null,
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
            twoPage,
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
        twoPage: TwoPageCurl? = null,
    ) {
        curlJob?.cancel()
        curlJob = scope.launch {
            runCurl(fromIndex, toIndex, side, front, back, fromProgress, animateForward, twoPage)
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
            val fromSpread = currentSpreads.getOrNull(from).orEmpty()
            val off = animationsOff(animatorScale)
            val cached = isSpreadCached(renderer, nextSpread, slotW, slotH)
            val mode = resolvePageTurn(
                preference = pageTurnAnimation,
                twoPage = twoPage,
                zoomed = zoomRef.value.allowsPageTurn.not(),
                nextPageCached = cached,
                animationsOff = off,
                endNoticeOnly = false,
                fromSpreadPages = fromSpread.size,
                toSpreadPages = nextSpread.size,
            )
            if (mode != ResolvedPageTurn.Curl) {
                // Field-report diagnostics: why the turn did not curl. Geometry and mode only,
                // never page content, file names or numbers.
                Log.d(TURN_LOG_TAG, "turn mode=${mode.name} twoPage=$twoPage")
                when (mode) {
                    ResolvedPageTurn.Instant -> pagerState.scrollToPage(target)
                    ResolvedPageTurn.Slide -> {
                        if (off) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
                    }
                    ResolvedPageTurn.Curl -> pagerState.scrollToPage(target)
                }
                return@launch
            }
            if (twoPage && fromSpread.size == 2 && nextSpread.size == 2) {
                val windowSize = IntSize(
                    viewport.width.roundToInt().coerceAtLeast(1),
                    viewport.height.roundToInt().coerceAtLeast(1),
                )
                val built = buildTwoPageCurl(
                    renderer = renderer,
                    currentSpread = fromSpread,
                    targetSpread = nextSpread,
                    direction = directionRef.value,
                    side = side,
                    posture = posture,
                    window = windowSize,
                    autoCrop = autoCrop,
                )
                val two = built.curl
                if (two == null) {
                    Log.i(TURN_LOG_TAG, "two-page curl fallback=${built.fallback}")
                    if (off) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
                    return@launch
                }
                runCurl(from, target, side, two.front, two.back, twoPage = two)
                return@launch
            }
            val front = cachedSpreadBitmaps(renderer, fromSpread, slotW, slotH)
            val back = cachedSpreadBitmaps(renderer, nextSpread, slotW, slotH)
            if (front == null || back == null) {
                Log.i(TURN_LOG_TAG, "single-sheet curl fallback=CacheMiss")
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
                                    width = { pagerWidth },
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
                                    width = { curlDragWidth.floatValue },
                                    currentIndex = { pagerState.targetPage },
                                    pageCount = { pagerState.pageCount },
                                    onDragStart = { targetIndex, forward ->
                                        if (!forward) endNotice = null
                                        val from = pagerState.targetPage
                                        val fromSpread = currentSpreads.getOrNull(from).orEmpty()
                                        val toSpread = currentSpreads.getOrNull(targetIndex).orEmpty()
                                        val dragSide = curlSide(direction, forward)
                                        val windowSize = IntSize(
                                            viewport.width.roundToInt().coerceAtLeast(1),
                                            viewport.height.roundToInt().coerceAtLeast(1),
                                        )
                                        val two = if (twoPage && fromSpread.size == 2 && toSpread.size == 2) {
                                            buildTwoPageCurl(
                                                renderer = renderer,
                                                currentSpread = fromSpread,
                                                targetSpread = toSpread,
                                                direction = direction,
                                                side = dragSide,
                                                posture = posture,
                                                window = windowSize,
                                                autoCrop = autoCrop,
                                            ).curl
                                        } else {
                                            null
                                        }
                                        // Two-page: progress is drag / spread width (2w).
                                        curlDragWidth.floatValue =
                                            two?.layout?.spread?.width ?: pagerWidth
                                        val slotW = renderSlot.width
                                        val slotH = renderSlot.height
                                        val fromBmp = two?.front
                                            ?: cachedSpreadBitmaps(renderer, fromSpread, slotW, slotH)
                                        val toBmp = two?.back
                                            ?: cachedSpreadBitmaps(renderer, toSpread, slotW, slotH)
                                        if (fromBmp == null || toBmp == null) {
                                            false
                                        } else {
                                            curlJob?.cancel()
                                            beginCurl(
                                                CurlEffectState(
                                                    fromIndex = from,
                                                    toIndex = targetIndex,
                                                    side = dragSide,
                                                    progress = 0f,
                                                    dragging = true,
                                                    completing = true,
                                                ),
                                                fromBmp,
                                                toBmp,
                                                two,
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
                                            twoPage = curlTwoPage,
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
                                autoCrop = autoCrop,
                            )
                        }
                        Row(Modifier.fillMaxSize()) {
                            tapZoneSpans(direction, sideFraction).forEach { span ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val zoneLabel = stringResource(
                                    when (span.zone) {
                                        TapZone.Previous -> R.string.reader_zone_previous_a11y
                                        TapZone.Menu -> R.string.reader_zone_menu_a11y
                                        TapZone.Next -> R.string.reader_zone_next_a11y
                                    },
                                )
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
                                        .semantics { contentDescription = zoneLabel }
                                        .then(
                                            if (zoomed || span.zone.acceptsDoubleTapAtBaseScale) {
                                                Modifier.combinedClickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = onTap,
                                                    onDoubleClick = { toggleZoom() },
                                                )
                                            } else {
                                                Modifier.clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                                    role = Role.Button,
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
                    val frontPage = currentSpreads
                        .getOrNull(curlEffect?.fromIndex ?: pagerState.currentPage)
                        ?.let { spreadLayout(it, direction) }
                        ?.firstOrNull()
                    val frontCrop = if (autoCrop) {
                        frontPage?.let { renderer.cropMargins(it) }
                    } else {
                        null
                    }
                    // The revealed page arrives on toIndex; it can have different margins,
                    // so crop it with its own rect or the last frame jumps to the settled page.
                    val backPage = curlEffect?.toIndex
                        ?.let { currentSpreads.getOrNull(it) }
                        ?.let { spreadLayout(it, direction) }
                        ?.firstOrNull()
                    val backCrop = if (autoCrop) {
                        backPage?.let { renderer.cropMargins(it) }
                    } else {
                        null
                    }
                    val displayMode = LocalDisplayMode.current
                    val curlFilter = remember(displayMode) { displayMode.colorFilter() }
                    val curlLook = remember(displayMode) { displayMode.curlLook() }
                    val two = curlTwoPage
                    PageCurlOverlay(
                        front = frontBmp,
                        back = backBmp,
                        progress = { curlProgress },
                        side = curlSideState,
                        modifier = overlayModifier.testTag("reader_page_curl"),
                        contentCrop = two?.frontCrop ?: frontCrop?.takeIf { !it.isNone }?.let {
                            Rect(it.left, it.top, 1f - it.right, 1f - it.bottom)
                        },
                        backContentCrop = two?.backCrop ?: backCrop?.takeIf { !it.isNone }?.let {
                            Rect(it.left, it.top, 1f - it.right, 1f - it.bottom)
                        },
                        flapBack = two?.flapBack,
                        flapContentCrop = two?.flapCrop,
                        still = two?.still,
                        stillContentCrop = two?.stillCrop,
                        layout = two?.layout,
                        colorFilter = curlFilter,
                        flapColor = curlLook.flapColor,
                        shadowColor = curlLook.shadowColor,
                        shadowAlpha = curlLook.shadowAlpha,
                        shadowMidAlpha = curlLook.shadowMidAlpha,
                        foldHighlightColor = curlLook.foldHighlightColor,
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
                    .pointerInput(Unit) { detectTapGestures { widthSheetVisible = false } },
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
                    .pointerInput(Unit) { detectTapGestures { brightnessSheetVisible = false } },
            )
            ReaderBrightnessSheet(
                followSystem = followSystemBrightness,
                percent = brightnessPercent,
                onFollowSystemChange = onFollowSystemBrightnessChange,
                onPercentPreview = { brightnessPreview = it },
                onPercentChange = onBrightnessChange,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (cropSheetVisible) {
            BackHandler { cropSheetVisible = false }
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { cropSheetVisible = false },
                    ),
            )
            val previewSpread = currentSpreads.getOrNull(pagerState.currentPage).orEmpty()
            val previewPages = spreadLayout(previewSpread, direction)
            ReaderCropSheet(
                renderer = renderer,
                pages = previewPages,
                twoPage = previewPages.size >= 2,
                enabled = autoCrop,
                onEnabledChange = onAutoCropChange,
                onDone = { cropSheetVisible = false },
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
                    onOpenAutoCrop = {
                        zoom = ZoomState()
                        cropSheetVisible = true
                    },
                    onOpenBookmarks = bookmarks?.let { { bookmarkSheetVisible = true } },
                    onPreviousBook = volumes.onPrevious,
                    onNextBook = volumes.onNext,
                    autoCrop = autoCrop,
                    onAutoCropChange = onAutoCropChange,
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
                    .pointerInput(Unit) { detectTapGestures { goToPageVisible = false } },
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

/** Logcat tag for why a turn used Slide/Instant instead of the curl. */
private const val TURN_LOG_TAG = "PdfBookTurn"

/** Why [buildTwoPageCurl] refused the curl and the turn used Slide instead. */
internal enum class TwoPageCurlFallback { SpreadPages, NoCanvas, CacheMiss, SpineGap }

internal data class TwoPageCurlBuild(
    val curl: TwoPageCurl?,
    val fallback: TwoPageCurlFallback?,
)

/** Everything the two-page curl overlay needs beyond the shared front/back bitmaps. */
internal data class TwoPageCurl(
    val front: Bitmap,
    val back: Bitmap,
    val flapBack: Bitmap,
    val still: Bitmap,
    val layout: CurlSheetLayout,
    val frontCrop: Rect?,
    val backCrop: Rect?,
    val flapCrop: Rect?,
    val stillCrop: Rect?,
)

/**
 * Builds the two-page curl for a two-page spread turning onto another two-page spread.
 *
 * Design A: the page on [side] turns about the spine. Its revealed page is the same-side
 * page of [targetSpread], and the flap's back face is the opposite-side page, drawn at 100%.
 * The current spread's other page is returned as [TwoPageCurl.still] so the overlay can keep
 * it on screen while the pager is already on the target spread.
 * Each page keeps its own measured rect, so spreads whose pages differ by a few points (a real
 * PDF) curl without the last frame jumping.
 * A Book posture with a hinge gap is computed on the virtual spread (halves butted together)
 * and carries the [CurlHinge], so the overlay cuts the turn at the fold instead of sliding.
 * Null (Slide fallback) when the bitmap cache missed, when the two slots do not meet at the
 * spine, or when the sheet and the pages it lands on differ by more than 1px (오대리 rule),
 * so the last frame cannot jump.
 */
internal fun buildTwoPageCurl(
    renderer: PageBitmaps,
    currentSpread: List<Int>,
    targetSpread: List<Int>,
    direction: ReadingDirection,
    side: CurlSide,
    posture: FoldPosture,
    window: IntSize,
    autoCrop: Boolean,
): TwoPageCurlBuild {
    if (currentSpread.size != 2 || targetSpread.size != 2) {
        return TwoPageCurlBuild(null, TwoPageCurlFallback.SpreadPages)
    }
    val canvas = curlCanvas(window, posture)
        ?: return TwoPageCurlBuild(null, TwoPageCurlFallback.NoCanvas)
    val visualCurrent = spreadLayout(currentSpread, direction)
    val visualTarget = spreadLayout(targetSpread, direction)
    val slots = listOf(canvas.left, canvas.right)
    // spreadRequests() renders both pages of a spread at slotWidth / 2 x slotHeight, with the
    // slot from pageRenderSlot(): the window for Flat, the top half for Tabletop, and the
    // virtual spread for a two-page Book with a hinge gap (so each half matches what
    // BookHalfPage renders). Look them up with that key.
    val slot = pageRenderSlot(window, posture, direction, twoPage = true)
    val requestWidth = (slot.width / 2).coerceAtLeast(1)
    val bitmapsCurrent = visualCurrent.map { page ->
        renderer.cached(page, requestWidth, slot.height)
            ?.takeUnless { it.isRecycled }
            ?: return TwoPageCurlBuild(null, TwoPageCurlFallback.CacheMiss)
    }
    val bitmapsTarget = visualTarget.map { page ->
        renderer.cached(page, requestWidth, slot.height)
            ?.takeUnless { it.isRecycled }
            ?: return TwoPageCurlBuild(null, TwoPageCurlFallback.CacheMiss)
    }
    val cropsCurrent = spreadCrops(renderer, visualCurrent, autoCrop)
    val cropsTarget = spreadCrops(renderer, visualTarget, autoCrop)
    val scaleCurrent = sharedCropScale(bitmapsCurrent, cropsCurrent, slots, canvas.size.height)
    val scaleTarget = sharedCropScale(bitmapsTarget, cropsTarget, slots, canvas.size.height)
    val rectCurrent = listOf(
        pageDrawRect(bitmapsCurrent[0].width, bitmapsCurrent[0].height, cropsCurrent[0], slots[0], SlotAlignment.End, scaleCurrent),
        pageDrawRect(bitmapsCurrent[1].width, bitmapsCurrent[1].height, cropsCurrent[1], slots[1], SlotAlignment.Start, scaleCurrent),
    )
    val rectTarget = listOf(
        pageDrawRect(bitmapsTarget[0].width, bitmapsTarget[0].height, cropsTarget[0], slots[0], SlotAlignment.End, scaleTarget),
        pageDrawRect(bitmapsTarget[1].width, bitmapsTarget[1].height, cropsTarget[1], slots[1], SlotAlignment.Start, scaleTarget),
    )
    val turning = if (side == CurlSide.Right) 1 else 0
    val sheet = rectCurrent[turning]
    val landing = rectTarget[turning]
    val backFace = rectTarget[1 - turning]
    // The current spread's other page stays on screen where it already was while the pager is
    // on the target spread.
    val still = rectCurrent[1 - turning]
    val spineX = if (side == CurlSide.Right) sheet.left else sheet.right
    if (!twoPageCurlMeetsAtSpine(
            sheet = sheet,
            landing = landing,
            backFace = backFace,
            side = side,
            still = still,
            spineX = spineX,
            canvasWidth = canvas.size.width.toFloat(),
            canvasHeight = canvas.size.height.toFloat(),
        )
    ) {
        return TwoPageCurlBuild(null, TwoPageCurlFallback.SpineGap)
    }
    return TwoPageCurlBuild(
        TwoPageCurl(
            front = bitmapsCurrent[turning],
            back = bitmapsTarget[turning],
            flapBack = bitmapsTarget[1 - turning],
            still = bitmapsCurrent[1 - turning],
            layout = CurlSheetLayout(
                sheet = sheet,
                spread = sheet.expandedTo(backFace),
                backFace = backFace,
                spineX = spineX,
                landing = landing,
                still = still,
                hinge = canvas.hinge,
            ),
            frontCrop = cropsCurrent[turning].toContentRect(),
            backCrop = cropsTarget[turning].toContentRect(),
            flapCrop = cropsTarget[1 - turning].toContentRect(),
            stillCrop = cropsCurrent[1 - turning].toContentRect(),
        ),
        null,
    )
}

/** Aligned margins for a visually ordered spread, as PageBitmap computes them. */
private fun spreadCrops(
    renderer: PageBitmaps,
    visualPages: List<Int>,
    autoCrop: Boolean,
): List<CropMargins> {
    if (!autoCrop) return List(visualPages.size) { CropMargins() }
    if (visualPages.size < 2) return visualPages.map { renderer.cropMargins(it) ?: CropMargins() }
    val left = renderer.cropMargins(visualPages[0]) ?: return List(visualPages.size) { CropMargins() }
    val right = renderer.cropMargins(visualPages[1]) ?: return List(visualPages.size) { CropMargins() }
    val aligned = alignedTwoPageMargins(left, right)
    return listOf(aligned.first, aligned.second)
}

/**
 * Shared spread scale, or null when a page has no crop: PageBitmap then fits each page on
 * its own (ContentScale.Fit), which [pageDrawRect] reproduces with a null scale.
 */
private fun sharedCropScale(
    bitmaps: List<Bitmap>,
    crops: List<CropMargins>,
    slots: List<IntRect>,
    slotHeight: Int,
): Float? {
    if (crops.any { it.isNone }) return null
    return spreadCropScale(
        metrics = bitmaps.indices.map { SpreadPageMetrics(bitmaps[it].width, bitmaps[it].height, crops[it]) },
        slotWidths = slots.map { it.width },
        slotHeight = slotHeight,
    )
}

private fun CropMargins.toContentRect(): Rect = Rect(left, top, 1f - right, 1f - bottom)

@Composable
private fun SpreadPageLayout(
    renderer: PageBitmaps,
    layout: List<Int>,
    posture: FoldPosture,
    direction: ReadingDirection,
    window: IntSize,
    tagged: Boolean,
    autoCrop: Boolean,
) {
    val book = posture as? FoldPosture.Book
    if (book != null) {
        val halves = bookHalves(window, book.foldBounds)
        val metrics = remember(layout, autoCrop) { mutableStateMapOf<Int, SpreadPageMetrics>() }
        val slotWidths = remember(layout, halves, autoCrop) {
            if (layout.size >= 2) listOf(halves.first.width, halves.second.width) else listOf(halves.first.width)
        }
        val sharedScale by remember(layout, halves, autoCrop) {
            derivedStateOf {
                spreadCropScale(
                    metrics = layout.map { metrics[it] },
                    slotWidths = slotWidths,
                    slotHeight = window.height,
                )
            }
        }
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
                    spreadPages = layout,
                    spreadIndex = 0,
                    autoCrop = autoCrop,
                    sharedScale = sharedScale,
                    onMetrics = { if (metrics[layout[0]] != it) metrics[layout[0]] = it },
                )
                BookHalfPage(
                    renderer = renderer,
                    page = layout[1],
                    half = halves.second,
                    alignment = Alignment.CenterStart,
                    tagged = tagged,
                    spreadPages = layout,
                    spreadIndex = 1,
                    autoCrop = autoCrop,
                    sharedScale = sharedScale,
                    onMetrics = { if (metrics[layout[1]] != it) metrics[layout[1]] = it },
                )
            } else {
                val half = bookSingleHalf(halves, direction)
                BookHalfPage(
                    renderer = renderer,
                    page = layout.first(),
                    half = half,
                    alignment = Alignment.Center,
                    tagged = tagged,
                    autoCrop = autoCrop,
                )
            }
        }
        return
    }
    val pageSlotWidth = (window.width / layout.size.coerceAtLeast(1)).coerceAtLeast(1)
    val pageSlotHeight = window.height.coerceAtLeast(1)
    val metrics = remember(layout, autoCrop) { mutableStateMapOf<Int, SpreadPageMetrics>() }
    val sharedScale by remember(layout, autoCrop, pageSlotWidth, pageSlotHeight) {
        derivedStateOf {
            spreadCropScale(
                metrics = layout.map { metrics[it] },
                slotWidths = List(layout.size) { pageSlotWidth },
                slotHeight = pageSlotHeight,
            )
        }
    }
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
                spreadPages = layout,
                spreadIndex = index,
                autoCrop = autoCrop,
                sharedScale = sharedScale,
                onMetrics = { if (metrics[page] != it) metrics[page] = it },
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
    spreadPages: List<Int> = listOf(page),
    spreadIndex: Int = 0,
    autoCrop: Boolean = false,
    sharedScale: Float? = null,
    onMetrics: ((SpreadPageMetrics) -> Unit)? = null,
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
            spreadPages = spreadPages,
            spreadIndex = spreadIndex,
            autoCrop = autoCrop,
            sharedScale = sharedScale,
            onMetrics = onMetrics,
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
    onOpenAutoCrop: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenBookmarks: (() -> Unit)? = null,
    onPreviousBook: (() -> Unit)? = null,
    onNextBook: (() -> Unit)? = null,
    autoCrop: Boolean = false,
    onAutoCropChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("reader_overlay_bottom")
            .background(ReaderOverlay)
            .pointerInput(Unit) { detectTapGestures { } }
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
                add(
                    ReaderToolItem.Action(
                        tool = ReaderTool.AutoCrop,
                        iconRes = if (autoCrop) R.drawable.ic_crop_on else R.drawable.ic_crop,
                        label = stringResource(R.string.reader_auto_crop_label),
                        testTag = "reader_auto_crop",
                        onClick = onOpenAutoCrop,
                        stateDescription = stringResource(
                            if (autoCrop) {
                                R.string.reader_crop_state_on
                            } else {
                                R.string.reader_crop_state_off
                            },
                        ),
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
    spreadPages: List<Int> = listOf(page),
    spreadIndex: Int = 0,
    autoCrop: Boolean = false,
    sharedScale: Float? = null,
    onMetrics: ((SpreadPageMetrics) -> Unit)? = null,
) {
    var bitmap by remember(renderer, page, maxWidth, maxHeight, autoCrop) {
        mutableStateOf(renderer.cached(page, maxWidth, maxHeight))
    }
    LaunchedEffect(renderer, page, maxWidth, maxHeight, autoCrop) {
        if (bitmap?.isRecycled != false) {
            bitmap = renderer.render(page, maxWidth, maxHeight)
        }
    }
    // Per-page crop state: a new page's detection only updates that page (and its spread),
    // never every visible PageBitmap like a global revision key did.
    val crop by produceState<CropMargins?>(
        initialValue = null,
        renderer,
        page,
        spreadPages,
        spreadIndex,
        autoCrop,
    ) {
        if (!autoCrop) {
            value = null
            return@produceState
        }
        value = alignedCropFor(renderer, page, spreadPages, spreadIndex)
        renderer.cropUpdates.collect {
            val next = alignedCropFor(renderer, page, spreadPages, spreadIndex)
            if (next != null && next != value) value = next
        }
    }
    val displayMode = LocalDisplayMode.current
    val colorFilter = remember(displayMode) { displayMode.colorFilter() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        val pageCrop = crop
        if (bmp != null && !bmp.isRecycled) {
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            if (pageCrop != null && !pageCrop.isNone) {
                SideEffect {
                    onMetrics?.invoke(SpreadPageMetrics(bmp.width, bmp.height, pageCrop))
                }
                CroppedPage(
                    image = imageBitmap,
                    width = bmp.width,
                    height = bmp.height,
                    crop = pageCrop,
                    alignment = alignment,
                    scale = sharedScale,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = alignment,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun alignedCropFor(
    renderer: PageBitmaps,
    page: Int,
    spreadPages: List<Int>,
    spreadIndex: Int,
): CropMargins? {
    if (spreadPages.size < 2) return renderer.cropMargins(page)
    val left = renderer.cropMargins(spreadPages[0]) ?: return null
    val right = renderer.cropMargins(spreadPages[1]) ?: return null
    val aligned = alignedTwoPageMargins(left, right)
    return if (spreadIndex == 0) aligned.first else aligned.second
}

internal data class SpreadPageMetrics(
    val width: Int,
    val height: Int,
    val crop: CropMargins,
) {
    fun croppedWidth(): Int = (width - (crop.left + crop.right) * width).roundToInt().coerceAtLeast(1)

    fun croppedHeight(): Int = (height - (crop.top + crop.bottom) * height).roundToInt().coerceAtLeast(1)
}

/**
 * One scale shared by both halves of a spread, so two pages with different outer margins end up
 * the same height. Null until both pages are measured (or for a single page).
 */
internal fun spreadCropScale(
    metrics: List<SpreadPageMetrics?>,
    slotWidths: List<Int>,
    slotHeight: Int,
): Float? {
    if (metrics.size < 2 || metrics.any { it == null } || slotHeight <= 0) return null
    var scale = Float.MAX_VALUE
    metrics.forEachIndexed { index, page ->
        val measured = page ?: return null
        val slotWidth = slotWidths.getOrNull(index) ?: return null
        if (slotWidth <= 0) return null
        val srcWidth = measured.croppedWidth().toFloat()
        val srcHeight = measured.croppedHeight().toFloat()
        scale = min(scale, min(slotWidth / srcWidth, slotHeight / srcHeight))
    }
    return scale.takeIf { it.isFinite() && it > 0f }
}

@Composable
internal fun CroppedPage(
    image: ImageBitmap,
    width: Int,
    height: Int,
    crop: CropMargins,
    alignment: Alignment,
    scale: Float? = null,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val left = (crop.left * width).roundToInt().coerceIn(0, width - 1)
        val top = (crop.top * height).roundToInt().coerceIn(0, height - 1)
        val right = (crop.right * width).roundToInt().coerceIn(0, width - 1 - left)
        val bottom = (crop.bottom * height).roundToInt().coerceIn(0, height - 1 - top)
        val sourceWidth = (width - left - right).coerceAtLeast(1)
        val sourceHeight = (height - top - bottom).coerceAtLeast(1)
        val fitScale = min(size.width / sourceWidth, size.height / sourceHeight)
        // A spread passes one shared scale; a single page fits its own slot.
        val drawScale = (scale ?: fitScale).coerceAtMost(fitScale)
        val drawWidth = (sourceWidth * drawScale).roundToInt().coerceAtLeast(1)
        val drawHeight = (sourceHeight * drawScale).roundToInt().coerceAtLeast(1)
        val offsetX = when {
            alignment == Alignment.CenterStart -> 0
            alignment == Alignment.CenterEnd -> size.width.roundToInt() - drawWidth
            else -> (size.width.roundToInt() - drawWidth) / 2
        }
        val offsetY = (size.height.roundToInt() - drawHeight) / 2
        drawImage(
            image = image,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(sourceWidth, sourceHeight),
            dstOffset = IntOffset(offsetX, offsetY),
            dstSize = IntSize(drawWidth, drawHeight),
            colorFilter = colorFilter,
        )
    }
}

private suspend fun PointerInputScope.detectCurlSwipe(
    direction: ReadingDirection,
    width: () -> Float,
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
                val raw = curlProgressFromDrag(dx, width(), side)
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
