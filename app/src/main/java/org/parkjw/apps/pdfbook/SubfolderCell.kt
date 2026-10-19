package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.hidden.HiddenBadge
import org.parkjw.apps.pdfbook.hidden.shouldUnhideOnLongPress
import org.parkjw.apps.pdfbook.ui.theme.CoverCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookDropdownMenu
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubfolderCell(
    item: SubfolderItem,
    covers: Map<String, Bitmap?>,
    onOpen: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpand: () -> Unit,
    onMenuDismiss: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
) {
    val contentAlpha = if (item.isHidden) 0.4f else 1f
    Box(Modifier.fillMaxWidth().testTag("library_subfolder")) {
        Column(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = {
                        if (shouldUnhideOnLongPress(item.isHidden)) onUnhide() else onMenuExpand()
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
        ) {
            Box {
                SubfolderTile(item, covers, Modifier.fillMaxWidth().alpha(contentAlpha))
                if (item.isHidden) {
                    HiddenBadge(Modifier.align(Alignment.TopEnd).padding(PdfBookSpacing.xxs))
                }
            }
            Column(Modifier.alpha(contentAlpha)) {
                Text(
                    item.node.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    withNumericSpans(
                        pluralStringResource(R.plurals.library_subfolder_count, item.bookCount, item.bookCount),
                        SpanStyle(fontFamily = PdfBookNumeric.fontFamily, fontSize = 11.sp),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!item.isHidden) {
            PdfBookDropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.hide_item)) },
                    onClick = onHide,
                )
            }
        }
    }
}

@Composable
private fun SubfolderTile(
    item: SubfolderItem,
    covers: Map<String, Bitmap?>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(CoverCorner)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CoverCorner),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        val books = item.coverBooks
        val count = books.size
        books.asReversed().forEachIndexed { reversedIndex, book ->
            val index = count - 1 - reversedIndex
            val shift = (index - (count - 1) / 2f) * 12f
            StackedCover(
                book = book,
                covers = covers,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .offset(x = shift.dp, y = shift.dp),
            )
        }
    }
}

@Composable
private fun StackedCover(book: BookInfo, covers: Map<String, Bitmap?>, modifier: Modifier) {
    val context = LocalContext.current
    val key = bookKey(book.uri, book.size)
    val provided = covers.containsKey(key)
    val loaded by produceState(covers[key], book, provided, covers[key]) {
        value = if (provided) covers[key] else loadCover(context, book)
    }
    val bitmap = loaded ?: return
    BookCover(title = "", cover = bitmap, modifier = modifier)
}
