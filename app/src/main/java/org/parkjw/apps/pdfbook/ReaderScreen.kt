package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    renderer: PageRenderer,
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
        )
    }
}

@Composable
private fun ReaderScreenContent(
    renderer: PageRenderer,
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
) {
    val currentSpreads by rememberUpdatedState(spreads)
    val rtl = direction == ReadingDirection.RTL
    val scope = rememberCoroutineScope()
    var overlayVisible by remember { mutableStateOf(false) }

    LaunchedEffect(volumeDelta, pagerState) {
        volumeDelta?.collect { delta ->
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val settledSpread = currentSpreads.getOrNull(pagerState.settledPage).orEmpty()
            val slotWidth = (constraints.maxWidth / settledSpread.size.coerceAtLeast(1)).coerceAtLeast(1)
            val slotHeight = constraints.maxHeight.coerceAtLeast(1)
            LaunchedEffect(pagerState.settledPage, currentSpreads, slotWidth, slotHeight) {
                renderer.prefetch(currentSpreads, pagerState.settledPage, slotWidth, slotHeight)
            }
            HorizontalPager(
                state = pagerState,
                reverseLayout = rtl,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                val spread = currentSpreads.getOrNull(pageIndex) ?: return@HorizontalPager
                val layout = spreadLayout(spread, direction)
                BoxWithConstraints(Modifier.fillMaxSize()) {
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
                }
            }
        }

        Row(Modifier.fillMaxSize()) {
            val leftDelta = if (rtl) 1 else -1
            val rightDelta = if (rtl) -1 else 1
            val noIndication = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = noIndication,
                        indication = null,
                        onClick = { goTo(leftDelta) },
                    ),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { overlayVisible = !overlayVisible },
                    ),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { goTo(rightDelta) },
                    ),
            )
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
    val onSurface = Color(0xFFE6E6EA)
    Row(
        modifier
            .fillMaxWidth()
            .background(Color(0xCC121316))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.reader_back),
                tint = onSurface,
            )
        }
        Text(
            text = title,
            color = onSurface,
            fontSize = 16.sp,
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
            tint = onSurface,
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
                tint = onSurface,
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
    val onSurface = Color(0xFFE6E6EA)
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xCC121316))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (pageCount > 1) {
            Slider(
                value = pageToSlider(currentPage, pageCount, direction),
                onValueChange = { onSeekPage(sliderToPage(it, pageCount, direction)) },
                valueRange = 0f..(pageCount - 1).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(R.string.progress, currentPage, pageCount),
            color = onSurface,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun PageBitmap(
    renderer: PageRenderer,
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
