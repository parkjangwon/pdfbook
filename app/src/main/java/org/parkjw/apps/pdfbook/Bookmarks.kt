package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookMonoFamily
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderScrim
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

const val BOOKMARK_SHEET_TAG = "bookmark_sheet"

/** Bookmarks of the open book; [pages] is sorted ascending and unique. */
data class ReaderBookmarks(
    val pages: List<Int>,
    val onToggle: (page: Int) -> Unit,
)

fun toggledBookmark(pages: List<Int>, page: Int): List<Int> =
    if (page in pages) pages - page else (pages + page).sorted()

fun encodeBookmarks(pages: List<Int>): String = pages.joinToString(",")

fun decodeBookmarks(raw: String?, pageCount: Int = Int.MAX_VALUE): List<Int> =
    raw.orEmpty().split(',').mapNotNull { it.toIntOrNull() }.filter { it in 1..pageCount }.distinct().sorted()

/** A spread counts as bookmarked if any of its pages is. */
fun spreadBookmarked(pages: List<Int>, spread: List<Int>): Boolean = spread.any { it in pages }

/** Removes every bookmarked page of the spread, else adds its first page. */
fun spreadToggleTargets(pages: List<Int>, spread: List<Int>): List<Int> =
    spread.filter { it in pages }.ifEmpty { spread.take(1) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkSheet(
    pages: List<Int>,
    onPick: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(BOOKMARK_SHEET_TAG).semantics { testTagsAsResourceId = true },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 640.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = ReaderOverlay,
        contentColor = ReaderOnOverlay,
        tonalElevation = 0.dp,
        scrimColor = ReaderScrim,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 32.dp, height = 4.dp).background(ReaderTrack, PdfBookShapeTokens.pill))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).safeDrawingBottomBarPadding().padding(bottom = 8.dp)) {
            Text(
                text = stringResource(R.string.bookmark_list_title),
                color = ReaderOnOverlay,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            )
            if (pages.isEmpty()) {
                Text(
                    text = stringResource(R.string.bookmark_empty),
                    color = ReaderOnOverlayVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(pages, key = { it }) { page ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("bookmark_row")
                            .clickable(role = Role.Button) { onPick(page) }
                            .padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = withNumericSpans(
                                stringResource(R.string.bookmark_page, page),
                                SpanStyle(fontFamily = PdfBookMonoFamily),
                            ),
                            color = ReaderOnOverlay,
                            fontSize = 14.sp,
                        )
                        IconButton(
                            onClick = { onDelete(page) },
                            colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                            modifier = Modifier.testTag("bookmark_delete"),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.bookmark_delete, page),
                                tint = ReaderOnOverlay,
                            )
                        }
                    }
                }
            }
        }
    }
}
