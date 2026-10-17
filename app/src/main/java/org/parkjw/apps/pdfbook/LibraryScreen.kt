package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.safeDrawingGridContentPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingPadding

@Composable
fun FolderPickScreen(
    illustration: Int,
    title: String,
    body: String,
    buttonLabel: String,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.size(160.dp, 120.dp),
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Button(onClick = onPick) { Text(buttonLabel) }
    }
}

@Composable
fun LibraryScreen(
    listing: Listing,
    states: Map<String, BookState>,
    onOpen: (BookInfo) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var byName by remember { mutableStateOf(false) }
    val sorted = remember(listing, states, byName) { sortBooks(listing.books, states, byName) }
    val resume = if (byName) null else sorted.firstOrNull { (states[bookKey(it.uri, it.size)]?.openedAt ?: 0L) > 0L }
    Column(modifier.fillMaxSize()) {
        SafeTopBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(listing.folderName, style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { byName = !byName }) {
                    Text(stringResource(if (byName) R.string.sort_name else R.string.sort_recent))
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsFor(maxWidth.value.toInt())),
                contentPadding = safeDrawingGridContentPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (resume != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ResumeCard(resume, states[bookKey(resume.uri, resume.size)]!!, onOpen)
                    }
                }
                items(sorted, key = { it.uri }) { book ->
                    BookCell(book, states[bookKey(book.uri, book.size)], onOpen)
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(book: BookInfo, state: BookState, onOpen: (BookInfo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(book) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Cover(book, Modifier.width(110.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.labelLarge)
            Text(titleOf(book.name), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ProgressInfo(state)
        }
    }
}

@Composable
private fun BookCell(book: BookInfo, state: BookState?, onOpen: (BookInfo) -> Unit) {
    Column(
        Modifier.clickable { onOpen(book) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Cover(book, Modifier.fillMaxWidth())
        Text(titleOf(book.name), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        ProgressInfo(state ?: BookState())
    }
}

@Composable
private fun ProgressInfo(state: BookState) {
    if (state.pageCount <= 0) return
    Text(stringResource(R.string.progress, state.page, state.pageCount), style = MaterialTheme.typography.labelSmall)
    LinearProgressIndicator(progress = { progressOf(state.page, state.pageCount) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Cover(book: BookInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cover by produceState<Bitmap?>(null, book) { value = loadCover(context, book) }
    Box(modifier.aspectRatio(2f / 3f), contentAlignment = Alignment.Center) {
        cover?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = titleOf(book.name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
