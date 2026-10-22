package org.parkjw.apps.pdfbook

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
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
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookReaderSlider
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import kotlinx.coroutines.flow.Flow
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingTopBarPadding
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
    posture: FoldPosture = FoldPosture.Flat,
    autoCrop: Boolean = false,
    onAutoCropChange: (Boolean) -> Unit = {},
) {
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
            posture = posture,
            autoCrop = autoCrop,
            onAutoCropChange = onAutoCropChange,
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
    posture: FoldPosture,
    autoCrop: Boolean,
    onAutoCropChange: (Boolean) -> Unit,
) {
    val cropRevision by renderer.cropUpdates.collectAsState(initial = 0)
    SideEffect { renderer.setAutoCrop(autoCrop) }
    var bookListVisible by rememberSaveable { mutableStateOf(initialBookListVisible) }
    val currentSpreads by rememberUpdatedState(spreads)
    val rtl = direction == ReadingDirection.RTL
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(initialOverlayVisible) }
    var widthSheetVisible by remember { mutableStateOf(initialTapZoneWidthSheetVisible) }
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

    var endNotice by remember { mutableStateOf(initialEndNotice) }
    var endNoticeToken by remember { mutableIntStateOf(0) }
    val currentVolumes by rememberUpdatedState(volumes)
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    var jumpBack by remember(renderer) { mutableStateOf(initialJumpBack) }
    var seekOrigin by remember(renderer) { mutableStateOf<Int?>(null) }
    var seekTarget by remember(renderer) { mutableIntStateOf(0) }
    val sheetOpen = widthSheetVisible || (bookListVisible && volumes.bookList != null)
    val jumpBackShown = jumpBackVisible(jumpBack, endNotice, sheetOpen)

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

    fun goTo(delta: Int) {
        if (delta < 0) endNotice = null
        val target = pagerTargetIndex(pagerState.targetPage, delta, pagerState.pageCount)
        if (target != pagerState.targetPage) {
            jumpBack = null
            scope.launch { pagerState.animateScrollToPage(target) }
        } else if (isTurnPastEnd(pagerState.targetPage, delta, pagerState.pageCount)) {
            nextAtEnd()
        }
    }

    LaunchedEffect(volumeDelta, pagerState) {
        volumeDelta?.collect { delta ->
            goTo(delta)
        }
    }

    fun scrollTo(index: Int) {
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
            val prerenderWidth = (pageArea?.width ?: window.width).coerceAtLeast(1)
            val prerenderHeight = (pageArea?.height ?: window.height).coerceAtLeast(1)
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
                    userScrollEnabled = !zoomed,
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
                            .pointerInput(lastActive, direction) {
                                if (!lastActive) return@pointerInput
                                detectSwipePastEnd(
                                    direction = direction,
                                    isZoomed = { !zoomRef.value.allowsPageTurn },
                                    onSwipe = { if (pagerState.targetPage == pageIndex) nextAtEnd() },
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
                                cropRevision = cropRevision,
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
            ReaderOverlayTop(
                title = title,
                twoPage = twoPage,
                direction = direction,
                onBack = onBack,
                onDirectionChange = onDirectionChange,
                showPageMode = tabletop == null,
                onOpenBookList = volumes.bookList?.let { { bookListVisible = true } },
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
                    showTapZoneEntry = !zoomed,
                    onAdjustTapZones = {
                        zoom = ZoomState()
                        widthSheetVisible = true
                    },
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
    autoCrop: Boolean,
    cropRevision: Int,
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
                    spreadPages = layout,
                    spreadIndex = 0,
                    autoCrop = autoCrop,
                    cropRevision = cropRevision,
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
                    cropRevision = cropRevision,
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
                    cropRevision = cropRevision,
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
                spreadPages = layout,
                spreadIndex = index,
                autoCrop = autoCrop,
                cropRevision = cropRevision,
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
    cropRevision: Int = 0,
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
            cropRevision = cropRevision,
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
    showTapZoneEntry: Boolean,
    onAdjustTapZones: () -> Unit,
    modifier: Modifier = Modifier,
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
        AutoCropEntry(checked = autoCrop, onCheckedChange = onAutoCropChange)
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
        Text(
            text = stringResource(R.string.progress, currentPage, pageCount),
            style = PdfBookNumeric.copy(fontSize = 13.sp, color = ReaderOnOverlay),
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("reader_progress"),
        )
    }
}

@Composable
private fun AutoCropEntry(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reader_auto_crop")
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reader_auto_crop_label),
            color = ReaderOnOverlayVariant,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ReaderOverlay,
                checkedTrackColor = ReaderOnOverlay,
                uncheckedThumbColor = ReaderOnOverlayVariant,
                uncheckedTrackColor = ReaderTrack,
                uncheckedBorderColor = ReaderTrack,
            ),
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
    cropRevision: Int = 0,
) {
    var bitmap by remember(renderer, page, maxWidth, maxHeight, cropRevision) {
        mutableStateOf(renderer.cached(page, maxWidth, maxHeight))
    }
    LaunchedEffect(renderer, page, maxWidth, maxHeight, cropRevision) {
        if (bitmap?.isRecycled != false) {
            bitmap = renderer.render(page, maxWidth, maxHeight)
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            val crop = if (autoCrop) alignedCropFor(renderer, page, spreadPages, spreadIndex) else null
            if (crop != null && !crop.isNone) {
                CroppedPage(
                    image = imageBitmap,
                    width = bmp.width,
                    height = bmp.height,
                    crop = crop,
                    alignment = alignment,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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

@Composable
private fun CroppedPage(
    image: ImageBitmap,
    width: Int,
    height: Int,
    crop: CropMargins,
    alignment: Alignment,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val left = (crop.left * width).roundToInt().coerceIn(0, width - 1)
        val top = (crop.top * height).roundToInt().coerceIn(0, height - 1)
        val right = (crop.right * width).roundToInt().coerceIn(0, width - 1 - left)
        val bottom = (crop.bottom * height).roundToInt().coerceIn(0, height - 1 - top)
        val sourceWidth = (width - left - right).coerceAtLeast(1)
        val sourceHeight = (height - top - bottom).coerceAtLeast(1)
        val scale = min(size.width / sourceWidth, size.height / sourceHeight)
        val drawWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
        val drawHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
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
        )
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
