package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.safeDrawingGridContentPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingHorizontalPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingPadding
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.CoverCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookProgressBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import kotlin.math.roundToInt

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
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(PdfBookSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.size(160.dp, 120.dp),
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        val ink = MaterialTheme.extendedColors
        Button(
            onClick = onPick,
            shape = ButtonCorner,
            colors = ButtonDefaults.buttonColors(
                containerColor = ink.inkButton,
                contentColor = ink.onInkButton,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp,
            ),
            modifier = Modifier.height(40.dp),
        ) { Text(buttonLabel) }
    }
}

@Composable
fun LibraryScreen(
    listing: Listing,
    states: Map<String, BookState>,
    onOpen: (BookInfo) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    covers: Map<String, Bitmap?> = emptyMap(),
) {
    var byName by remember { mutableStateOf(false) }
    val sorted = remember(listing, states, byName) { sortBooks(listing.books, states, byName) }
    val resume = if (byName) null else sorted.firstOrNull { (states[bookKey(it.uri, it.size)]?.openedAt ?: 0L) > 0L }
    Column(modifier.fillMaxSize()) {
        SafeTopBar(
            modifier = Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(listing.folderName, style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { byName = !byName },
                    colors = pdfBookTextButtonColors(),
                ) {
                    Text(stringResource(if (byName) R.string.sort_name else R.string.sort_recent))
                }
                IconButton(
                    onClick = onSettings,
                    colors = pdfBookIconButtonColors(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingHorizontalPadding()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsFor(maxWidth.value.toInt())),
                contentPadding = safeDrawingGridContentPadding(),
                horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.md),
            ) {
                if (resume != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ResumeCard(
                            resume,
                            states[bookKey(resume.uri, resume.size)]!!,
                            onOpen,
                            covers[bookKey(resume.uri, resume.size)],
                            covers.containsKey(bookKey(resume.uri, resume.size)),
                        )
                    }
                }
                items(sorted, key = { it.uri }) { book ->
                    BookCell(
                        book,
                        states[bookKey(book.uri, book.size)],
                        onOpen,
                        covers[bookKey(book.uri, book.size)],
                        covers.containsKey(bookKey(book.uri, book.size)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(
    book: BookInfo,
    state: BookState,
    onOpen: (BookInfo) -> Unit,
    cover: Bitmap?,
    coverProvided: Boolean,
) {
    Surface(
        onClick = { onOpen(book) },
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(PdfBookSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.md),
        ) {
            Cover(book, Modifier.width(110.dp), cover, coverProvided)
            Column(verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs, Alignment.CenterVertically)) {
                Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.labelSmall)
                Text(
                    titleOf(book.name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ProgressInfo(state)
            }
        }
    }
}

@Composable
private fun BookCell(
    book: BookInfo,
    state: BookState?,
    onOpen: (BookInfo) -> Unit,
    cover: Bitmap?,
    coverProvided: Boolean,
) {
    Column(
        Modifier.clickable { onOpen(book) },
        verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
    ) {
        Cover(book, Modifier.fillMaxWidth(), cover, coverProvided)
        Text(
            titleOf(book.name),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ProgressInfo(state ?: BookState())
    }
}

@Composable
private fun ProgressInfo(state: BookState) {
    if (state.pageCount <= 0) return
    val percent = (progressOf(state.page, state.pageCount) * 100f).roundToInt()
    Text(
        text = "$percent%",
        style = PdfBookNumeric.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PdfBookProgressBar(
        progress = progressOf(state.page, state.pageCount),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Cover(
    book: BookInfo,
    modifier: Modifier = Modifier,
    cover: Bitmap? = null,
    coverProvided: Boolean = false,
) {
    val context = LocalContext.current
    val loaded by produceState(cover, book, cover, coverProvided) {
        value = if (coverProvided) cover else loadCover(context, book)
    }
    BookCover(titleOf(book.name), loaded, modifier)
}

@Composable
fun BookCover(
    title: String,
    cover: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(CoverCorner)
            .border(1.dp, MaterialTheme.extendedColors.hairline, CoverCorner),
        contentAlignment = Alignment.Center,
    ) {
        cover?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
