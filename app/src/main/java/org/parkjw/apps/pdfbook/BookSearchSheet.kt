package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookMonoFamily
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderScrim
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

const val READER_SEARCH_SHEET_TAG = "reader_search_sheet"
const val READER_SEARCH_FIELD_TAG = "reader_search_field"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchSheet(
    searcher: PageTextSearch,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchPageHits>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        val q = query.trim()
        hits = emptyList()
        searching = false
        finished = false
        if (q.isEmpty()) return@LaunchedEffect
        searching = true
        val collected = mutableListOf<SearchPageHits>()
        searcher.search(q) { pageHits ->
            collected += pageHits
            hits = collected.toList()
        }
        searching = false
        finished = true
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnJump by rememberUpdatedState(onJump)
    var closing by remember { mutableStateOf(false) }
    fun jumpTo(page: Int) {
        if (closing) return
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { currentOnJump(page) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(READER_SEARCH_SHEET_TAG).semantics { testTagsAsResourceId = true },
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = ReaderOverlay,
        contentColor = ReaderOnOverlay,
        tonalElevation = 0.dp,
        scrimColor = ReaderScrim,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(ReaderChip, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                    }
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .background(ReaderTrack, PdfBookShapeTokens.pill),
                )
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = with(LocalDensity.current) {
                    LocalWindowInfo.current.containerSize.height.toDp()
                } * 0.75f)
                .safeDrawingBottomBarPadding()
                .padding(bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.reader_search),
                    color = ReaderOnOverlay,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (hits.isNotEmpty()) {
                    Text(
                        text = withNumericSpans(
                            stringResource(R.string.reader_search_matches, hits.sumOf { it.count }),
                            SpanStyle(fontFamily = PdfBookMonoFamily),
                        ),
                        color = ReaderOnOverlayVariant,
                        fontSize = 13.sp,
                    )
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.reader_search_hint),
                        color = ReaderOnOverlayVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ReaderOnOverlay,
                    unfocusedTextColor = ReaderOnOverlay,
                    cursorColor = ReaderAccent,
                    focusedBorderColor = ReaderAccent,
                    unfocusedBorderColor = ReaderChip,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp)
                    .testTag(READER_SEARCH_FIELD_TAG),
            )
            when {
                hits.isNotEmpty() -> LazyColumn(Modifier.fillMaxWidth()) {
                    items(hits, key = { it.page }) { hit ->
                        SearchRow(
                            hit = hit,
                            onClick = { jumpTo(hit.page) },
                        )
                    }
                }
                searching -> Text(
                    text = stringResource(R.string.reader_search_searching),
                    color = ReaderOnOverlayVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                )
                finished -> Text(
                    text = stringResource(R.string.reader_search_no_text),
                    color = ReaderOnOverlayVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SearchRow(
    hit: SearchPageHits,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .testTag("reader_search_row")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = withNumericSpans(
                stringResource(R.string.reader_search_page, hit.page),
                SpanStyle(fontFamily = PdfBookMonoFamily),
            ),
            color = ReaderOnOverlay,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = withNumericSpans(
                stringResource(R.string.reader_search_matches, hit.count),
                SpanStyle(fontFamily = PdfBookMonoFamily),
            ),
            color = ReaderOnOverlayVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
