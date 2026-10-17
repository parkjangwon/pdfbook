package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.safeDrawingGridContentPadding
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.CoverCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookDropdownMenu
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookProgressBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookOutlinedButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

@Composable
fun FolderShelfScreen(
    shelf: FolderShelfUi,
    covers: Map<String, Bitmap?>,
    snackbarMessage: String?,
    scrollToUri: String?,
    showRenameFor: FolderCardUi?,
    onSnackbarShown: () -> Unit,
    onScrollHandled: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenResume: (GlobalResumeUi) -> Unit,
    onAddFolder: () -> Unit,
    onSettings: () -> Unit,
    onRenameRequest: (FolderCardUi) -> Unit,
    onRenameConfirm: (String, String) -> Unit,
    onRenameDismiss: () -> Unit,
    onRepick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onLostFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
    showMenuFor: String? = null,
    showRemoveConfirmFor: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var menuUri by remember { mutableStateOf(showMenuFor) }
    var confirmRemoveUri by remember { mutableStateOf(showRemoveConfirmFor) }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onSnackbarShown()
    }
    LaunchedEffect(scrollToUri, shelf.cards) {
        val uri = scrollToUri ?: return@LaunchedEffect
        val index = shelf.cards.indexOfFirst { it.uri == uri }
        if (!shouldClearScrollTarget(index)) return@LaunchedEffect
        val offset = if (shelf.globalResume != null) 1 else 0
        val listIndex = index + offset
        val lastIndex = offset + shelf.cards.lastIndex
        if (listIndex >= lastIndex) {
            listState.scrollToItem(listIndex, scrollOffset = Int.MAX_VALUE / 4)
        } else {
            listState.animateScrollToItem(listIndex)
        }
        onScrollHandled()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val ink = MaterialTheme.extendedColors
            ExtendedFloatingActionButton(
                onClick = onAddFolder,
                shape = CardCorner,
                containerColor = ink.inkButton,
                contentColor = ink.onInkButton,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.folders_add),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        },
        topBar = {
            SafeTopBar(
                modifier = Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.folders_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(onClick = onSettings, colors = pdfBookIconButtonColors()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = safeDrawingGridContentPadding(
                extraBottom = 56.dp + PdfBookSpacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.md),
        ) {
            shelf.globalResume?.let { resume ->
                item(key = "resume") {
                    GlobalResumeCard(
                        resume = resume,
                        cover = covers[bookKey(resume.book.uri, resume.book.size)],
                        onClick = { onOpenResume(resume) },
                    )
                }
            }
            itemsIndexed(shelf.cards, key = { _, card -> card.uri }) { _, card ->
                Box {
                    FolderCard(
                        card = card,
                        covers = covers,
                        menuExpanded = menuUri == card.uri,
                        onMenuExpand = { menuUri = card.uri },
                        onMenuDismiss = { menuUri = null },
                        onOpen = {
                            if (card.access == FolderAccess.Lost) onLostFolder(card.uri)
                            else onOpenFolder(card.uri)
                        },
                        onRename = {
                            menuUri = null
                            onRenameRequest(card)
                        },
                        onRepick = {
                            menuUri = null
                            onRepick(card.uri)
                        },
                        onRemove = {
                            menuUri = null
                            confirmRemoveUri = card.uri
                        },
                    )
                }
            }
        }
    }

    showRenameFor?.let { card ->
        RenameFolderDialog(
            initial = card.displayName,
            onDismiss = onRenameDismiss,
            onConfirm = { onRenameConfirm(card.uri, it) },
        )
    }
    confirmRemoveUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRemoveUri = null },
            title = { Text(stringResource(R.string.folder_remove_confirm_title)) },
            text = { Text(stringResource(R.string.folder_remove_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemoveUri = null
                        onRemove(uri)
                    },
                    colors = pdfBookTextButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.folder_remove))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmRemoveUri = null },
                    colors = pdfBookTextButtonColors(),
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GlobalResumeCard(
    resume: GlobalResumeUi,
    cover: Bitmap?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(PdfBookSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.md),
        ) {
            BookCover(
                title = titleOf(resume.book.name),
                cover = cover,
                modifier = Modifier.width(88.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs)) {
                Text(
                    text = stringResource(R.string.continue_reading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = titleOf(resume.book.name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (resume.state.pageCount > 0) {
                    ResumeProgress(resume.percent, resume.progress)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    card: FolderCardUi,
    covers: Map<String, Bitmap?>,
    menuExpanded: Boolean,
    onMenuExpand: () -> Unit,
    onMenuDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onRepick: () -> Unit,
    onRemove: () -> Unit,
) {
    val lost = card.access == FolderAccess.Lost
    Surface(
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onMenuExpand),
    ) {
        Column(Modifier.padding(PdfBookSpacing.md)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
                ) {
                    Text(
                        text = card.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = card.pathTail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (lost) {
                        Surface(
                            shape = PdfBookShapeTokens.pill,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.folder_no_access_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(
                                    horizontal = PdfBookSpacing.xs,
                                    vertical = PdfBookSpacing.xxs,
                                ),
                            )
                        }
                    } else {
                        val countText = pluralStringResource(
                            R.plurals.folder_books_count,
                            card.bookCount,
                            card.bookCount,
                        )
                        Text(
                            text = withNumericSpans(
                                countText,
                                SpanStyle(
                                    fontFamily = PdfBookNumeric.fontFamily,
                                    fontSize = 12.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box {
                    IconButton(onClick = onMenuExpand, colors = pdfBookIconButtonColors()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more),
                            contentDescription = null,
                        )
                    }
                    PdfBookDropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_rename)) },
                            onClick = onRename,
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_repick)) },
                            onClick = onRepick,
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.folder_remove),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = onRemove,
                        )
                    }
                }
            }
            Spacer(Modifier.height(PdfBookSpacing.sm))
            CoverStrip(card.coverKeys, covers, dimmed = lost)
            if (lost) {
                Spacer(Modifier.height(PdfBookSpacing.sm))
                val ink = MaterialTheme.extendedColors
                OutlinedButton(
                    onClick = onRepick,
                    shape = ButtonCorner,
                    border = BorderStroke(1.dp, ink.hairlineStrong),
                    colors = pdfBookOutlinedButtonColors(),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(stringResource(R.string.folder_repick))
                }
            } else if (card.resume != null) {
                Spacer(Modifier.height(PdfBookSpacing.sm))
                Text(
                    text = card.resume.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ResumeProgress(card.resume.percent, card.resume.progress)
            }
        }
    }
}

@Composable
private fun CoverStrip(
    coverKeys: List<String>,
    covers: Map<String, Bitmap?>,
    dimmed: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs),
        modifier = Modifier.alpha(if (dimmed) 0.4f else 1f),
    ) {
        repeat(4) { index ->
            val key = coverKeys.getOrNull(index)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(2f / 3f)
                    .clip(CoverCorner)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (key != null) {
                    BookCover(
                        title = "",
                        cover = covers[key],
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeProgress(percent: Int, progress: Float) {
    Text(
        text = withNumericSpans(
            "$percent%",
            SpanStyle(
                fontFamily = PdfBookNumeric.fontFamily,
                fontSize = 11.sp,
                letterSpacing = (-0.2).sp,
            ),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PdfBookProgressBar(
        progress = progress,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun RenameFolderDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_rename)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            val ink = MaterialTheme.extendedColors
            Button(
                onClick = { onConfirm(value) },
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
            ) {
                Text(stringResource(R.string.folder_rename))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = pdfBookTextButtonColors(),
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
