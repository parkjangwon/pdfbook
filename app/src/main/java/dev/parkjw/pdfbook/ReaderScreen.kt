package dev.parkjw.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    renderer: PageRenderer,
    pagerState: PagerState,
    spreads: List<List<Int>>,
    modifier: Modifier = Modifier,
    direction: ReadingDirection = ReadingDirection.RTL,
    volumeDelta: Flow<Int>? = null,
) {
    val currentSpreads by rememberUpdatedState(spreads)
    val rtl = direction == ReadingDirection.RTL
    val scope = rememberCoroutineScope()

    LaunchedEffect(volumeDelta, pagerState) {
        volumeDelta?.collect { delta ->
            val target = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
            if (target != pagerState.currentPage) {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    fun goTo(delta: Int) {
        val target = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
        if (target != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = rtl,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            val spread = currentSpreads[pageIndex]
            val layout = spreadLayout(spread, direction)
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val slotWidth = (constraints.maxWidth / layout.size.coerceAtLeast(1)).coerceAtLeast(1)
                val slotHeight = constraints.maxHeight.coerceAtLeast(1)
                LaunchedEffect(pageIndex, slotWidth, slotHeight, currentSpreads) {
                    renderer.prefetch(currentSpreads, pageIndex, slotWidth, slotHeight)
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
                            maxWidth = slotWidth,
                            maxHeight = slotHeight,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            alignment = pageAlignment,
                        )
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
                    .fillMaxHeight(),
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
    var bitmap by remember(page, maxWidth, maxHeight) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page, maxWidth, maxHeight) {
        bitmap = renderer.render(page, maxWidth, maxHeight)
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = alignment,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
