package org.parkjw.apps.pdfbook

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderListCurrent
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderScrim
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

const val PAGE_NAV_SHEET_TAG = "page_nav_sheet"

private val ThumbCorner = RoundedCornerShape(6.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageNavSheet(
    renderer: PageBitmaps,
    outline: List<OutlineEntry>,
    pageCount: Int,
    currentPage: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnPick by rememberUpdatedState(onPick)
    var closing by remember { mutableStateOf(false) }
    fun pick(page: Int) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { currentOnPick(page) }
    }
    val sheetMaxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.75f
    var contentsTab by rememberSaveable(outline.isNotEmpty()) { mutableStateOf(outline.isNotEmpty()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(PAGE_NAV_SHEET_TAG).semantics { testTagsAsResourceId = true },
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = ReaderOverlay,
        contentColor = ReaderOnOverlay,
        tonalElevation = 0.dp,
        scrimColor = ReaderScrim,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .safeDrawingBottomBarPadding()
                .padding(bottom = 8.dp),
        ) {
            if (outline.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavTab(R.string.page_nav_tab_contents, contentsTab, "page_nav_tab_contents") { contentsTab = true }
                    NavTab(R.string.page_nav_tab_pages, !contentsTab, "page_nav_tab_pages") { contentsTab = false }
                }
            }
            if (contentsTab && outline.isNotEmpty()) {
                OutlineList(outline, currentPage, ::pick)
            } else {
                ThumbGrid(renderer, pageCount, currentPage, ::pick)
            }
        }
    }
}

@Composable
private fun NavTab(labelRes: Int, selected: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        text = stringResource(labelRes),
        color = if (selected) ReaderOnOverlay else ReaderOnOverlayVariant,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ReaderChip else ReaderOverlay)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun OutlineList(outline: List<OutlineEntry>, currentPage: Int, onPick: (Int) -> Unit) {
    val here = currentOutlineIndex(outline, currentPage)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (here - 2).coerceAtLeast(0))
    LazyColumn(state = state, modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(outline) { index, entry ->
            val current = index == here
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("page_nav_outline_row")
                    .then(if (current) Modifier.background(ReaderListCurrent) else Modifier)
                    .semantics { selected = current }
                    .clickable(role = Role.Button) { onPick(entry.page) }
                    .padding(start = 20.dp + 16.dp * entry.depth.coerceAtMost(4), end = 20.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.title,
                    color = ReaderOnOverlay,
                    fontSize = 14.sp,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = entry.page.toString(),
                    style = PdfBookNumeric.copy(fontSize = 12.sp, color = ReaderOnOverlayVariant),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ThumbGrid(renderer: PageBitmaps, pageCount: Int, currentPage: Int, onPick: (Int) -> Unit) {
    val density = LocalDensity.current.density
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (currentPage - 1).coerceAtLeast(0))
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(THUMB_WIDTH_DP.dp),
        modifier = Modifier.fillMaxWidth().testTag("page_nav_grid"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items((1..pageCount).toList(), key = { it }) { page ->
            val current = page == currentPage
            val label = stringResource(R.string.page_nav_thumb_description, page)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .testTag("page_nav_thumb")
                    .semantics { contentDescription = label; selected = current }
                    .clickable(role = Role.Button) { onPick(page) },
            ) {
                // Scrolled-away cells cancel their produceState, so a fast fling drops queued renders.
                val bitmap by produceState<ImageBitmap?>(null, page) {
                    value = renderer.thumbnail(thumbRequest(page, density))?.asImageBitmap()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(THUMB_WIDTH_DP.toFloat() / THUMB_HEIGHT_DP)
                        .clip(ThumbCorner)
                        .background(ReaderChip)
                        .then(if (current) Modifier.border(2.dp, ReaderTrack, ThumbCorner) else Modifier),
                ) {
                    bitmap?.let {
                        Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                }
                Text(
                    text = page.toString(),
                    style = PdfBookNumeric.copy(fontSize = 11.sp, color = if (current) ReaderOnOverlay else ReaderOnOverlayVariant),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
