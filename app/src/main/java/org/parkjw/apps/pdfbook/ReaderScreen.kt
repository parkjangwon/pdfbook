package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingTopBarPadding

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
) {
    val currentSpreads by rememberUpdatedState(spreads)
    val rtl = direction == ReadingDirection.RTL
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(initialOverlayVisible) }
    var zoom by remember { mutableStateOf(ZoomState()) }
    val zoomRef = rememberUpdatedState(zoom)
    val zoomed = !zoom.allowsPageTurn

    LaunchedEffect(pagerState.settledPage) {
        zoom = ZoomState()
    }

    LaunchedEffect(volumeDelta, pagerState) {
        volumeDelta?.collect { delta ->
            if (!zoomRef.value.allowsPageTurn) return@collect
            val target = pagerTargetIndex(pagerState.targetPage, delta, pagerState.pageCount)
            if (target != pagerState.targetPage) {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    fun goTo(delta: Int) {
        val target = pagerTargetIndex(pagerState.targetPage, delta, pagerState.pageCount)
        if (target != pagerState.targetPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    fun seekPage(page: Int) {
        val index = spreadIndexOf(currentSpreads, page)
        if (pagerState.targetPage != index) {
            scope.launch { pagerState.scrollToPage(index) }
        }
    }

    fun toggleZoom() {
        zoom = zoom.toggleDoubleTap()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReaderBackground),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportWidth = constraints.maxWidth.toFloat()
            val viewportHeight = constraints.maxHeight.toFloat()
            val settledSpread = currentSpreads.getOrNull(pagerState.settledPage).orEmpty()
            val slotWidth = (constraints.maxWidth / settledSpread.size.coerceAtLeast(1)).coerceAtLeast(1)
            val slotHeight = constraints.maxHeight.coerceAtLeast(1)
            LaunchedEffect(pagerState.settledPage, currentSpreads, slotWidth, slotHeight) {
                renderer.prefetch(currentSpreads, pagerState.settledPage, slotWidth, slotHeight)
            }
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
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(viewportWidth, viewportHeight, activePage) {
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
                                        viewportWidth,
                                        viewportHeight,
                                    )
                                },
                            )
                        }
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
                    val pageSlotWidth = (constraints.maxWidth / layout.size.coerceAtLeast(1)).coerceAtLeast(1)
                    val pageSlotHeight = constraints.maxHeight.coerceAtLeast(1)
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
                                    .fillMaxHeight(),
                                alignment = pageAlignment,
                            )
                        }
                    }
                    if (zoomed) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(onDoubleTap = { toggleZoom() })
                                },
                        )
                    } else {
                        val leftDelta = if (rtl) 1 else -1
                        val rightDelta = if (rtl) -1 else 1
                        Row(Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { goTo(leftDelta) },
                                        onDoubleClick = { toggleZoom() },
                                    ),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { overlayVisible = !overlayVisible },
                                        onDoubleClick = { toggleZoom() },
                                    ),
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { goTo(rightDelta) },
                                        onDoubleClick = { toggleZoom() },
                                    ),
                            )
                        }
                    }
                }
            }
        }

        if (overlayVisible) {
            ReaderOverlayTop(
                title = title,
                twoPage = twoPage,
                direction = direction,
                onBack = onBack,
                onDirectionChange = onDirectionChange,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            ReaderOverlayBottom(
                pageCount = pageCount,
                currentPage = currentPage,
                direction = direction,
                onSeekPage = ::seekPage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ReaderOverlayTop(
    title: String,
    twoPage: Boolean,
    direction: ReadingDirection,
    onBack: () -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
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
        IconButton(onClick = onBack) {
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
        Icon(
            painter = painterResource(
                if (twoPage) R.drawable.ic_page_spread else R.drawable.ic_page_single,
            ),
            contentDescription = stringResource(
                if (twoPage) R.string.reader_mode_double else R.string.reader_mode_single,
            ),
            tint = ReaderOnOverlay,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        IconButton(onClick = { onDirectionChange(toggledDirection(direction)) }) {
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
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
        if (pageCount > 1) {
            val sliderDirection =
                if (direction == ReadingDirection.RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides sliderDirection) {
                Slider(
                    value = pageToSlider(currentPage, pageCount, ReadingDirection.LTR),
                    onValueChange = { onSeekPage(sliderToPage(it, pageCount, ReadingDirection.LTR)) },
                    valueRange = 0f..(pageCount - 1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ReaderAccent,
                        activeTrackColor = ReaderAccent,
                        inactiveTrackColor = ReaderTrack,
                        activeTickColor = ReaderAccent,
                        inactiveTickColor = ReaderTrack,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = withNumericSpans(
                stringResource(R.string.progress, currentPage, pageCount),
                PdfBookNumeric.copy(fontSize = 13.sp, color = ReaderOnOverlay).toSpanStyle(),
            ),
            style = MaterialTheme.typography.bodySmall.copy(color = ReaderOnOverlay),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PageBitmap(
    renderer: PageBitmaps,
    page: Int,
    maxWidth: Int,
    maxHeight: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
) {
    var bitmap by remember(renderer, page, maxWidth, maxHeight) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(renderer, page, maxWidth, maxHeight) {
        bitmap = renderer.render(page, maxWidth, maxHeight)
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
