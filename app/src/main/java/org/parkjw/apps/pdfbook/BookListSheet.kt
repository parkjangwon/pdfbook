package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookMonoFamily
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookReaderProgressBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderListCurrent
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderScrim
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

const val BOOK_LIST_SHEET_TAG = "book_list_sheet"
const val BOOK_LIST_SCRIM_TAG = "book_list_scrim"

private val ThumbCorner = RoundedCornerShape(4.dp)

@Composable
fun BookListSheet(
    folderName: String,
    entries: List<BookListEntry>,
    onPick: (BookInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    covers: Map<String, Bitmap?> = emptyMap(),
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val sheetMaxHeight = maxHeight * 0.75f
        Box(
            Modifier
                .fillMaxSize()
                .testTag(BOOK_LIST_SCRIM_TAG)
                .background(ReaderScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .testTag(BOOK_LIST_SHEET_TAG)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(ReaderOverlay)
                .drawBehind {
                    drawLine(ReaderChip, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                }
                .pointerInput(Unit) { detectTapGestures { } }
                .safeDrawingBottomBarPadding()
                .padding(top = 10.dp, bottom = 8.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .background(ReaderTrack, PdfBookShapeTokens.pill),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = folderName,
                    color = ReaderOnOverlay,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = withNumericSpans(
                        pluralStringResource(R.plurals.book_list_count, entries.size, entries.size),
                        SpanStyle(fontFamily = PdfBookMonoFamily),
                    ),
                    color = ReaderOnOverlayVariant,
                    fontSize = 13.sp,
                )
            }
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = (currentEntryIndex(entries) - 2).coerceAtLeast(0),
            )
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(entries, key = { _, entry -> entry.book.uri }) { _, entry ->
                    BookListRow(
                        entry = entry,
                        covers = covers,
                        onClick = { if (entry.isCurrent) onDismiss() else onPick(entry.book) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookListRow(
    entry: BookListEntry,
    covers: Map<String, Bitmap?>,
    onClick: () -> Unit,
) {
    val current = entry.isCurrent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(if (current) Modifier.background(ReaderListCurrent) else Modifier)
            .drawBehind {
                if (current) drawRect(ReaderAccent, size = Size(3.dp.toPx(), size.height))
            }
            .semantics { selected = current }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookListThumb(entry.book, covers)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = titleOf(entry.book.name),
                color = ReaderOnOverlay,
                fontSize = 14.sp,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PdfBookReaderProgressBar(
                    progress = (entry.percent ?: 0) / 100f,
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.book_list_progress, entry.percent ?: 0),
                    style = PdfBookNumeric.copy(fontSize = 11.sp, color = ReaderOnOverlayVariant),
                )
            }
        }
        if (current) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.book_list_reading),
                color = ReaderAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BookListThumb(book: BookInfo, covers: Map<String, Bitmap?>) {
    val context = LocalContext.current
    val key = bookKey(book.uri, book.size)
    val provided = covers.containsKey(key)
    val loaded by produceState(covers[key], book, provided, covers[key]) {
        value = if (provided) covers[key] else loadCover(context, book)
    }
    Box(
        Modifier
            .size(width = 40.dp, height = 56.dp)
            .clip(ThumbCorner)
            .background(ReaderChip),
    ) {
        loaded?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
