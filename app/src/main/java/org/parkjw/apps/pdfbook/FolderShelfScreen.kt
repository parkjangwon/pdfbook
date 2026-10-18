package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.hidden.HiddenBadge
import org.parkjw.apps.pdfbook.hidden.PeekHiddenBanner
import org.parkjw.apps.pdfbook.hidden.shouldUnhideOnLongPress
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.safeDrawingGridContentPadding
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.CoverCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookDropdownMenu
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookProgressBar
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSnackbarHost
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
    snackbarActionLabel: String? = null,
    onSnackbarAction: () -> Unit = {},
    onHide: (String) -> Unit = {},
    peekEnabled: Boolean = false,
    peekMenuEnabled: Boolean = false,
    onPeekToggle: () -> Unit = {},
    onUnhide: (String) -> Unit = {},
    showPeekMenu: Boolean = false,
    exitHintToken: Int = 0,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    var menuUri by remember { mutableStateOf(showMenuFor) }
    var peekMenuOpen by remember { mutableStateOf(showPeekMenu) }
    var confirmRemoveUri by remember { mutableStateOf(showRemoveConfirmFor) }
    val hasResume = shelf.globalResume != null

    val exitConfirm = stringResource(R.string.exit_confirm)
    LaunchedEffect(snackbarMessage, snackbarActionLabel) {
        val message = snackbarMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message = message, actionLabel = snackbarActionLabel)
        if (result == SnackbarResult.ActionPerformed) onSnackbarAction()
        onSnackbarShown()
    }
    LaunchedEffect(exitHintToken) {
        if (exitHintToken == 0) return@LaunchedEffect
        val shown = launch {
            snackbarHostState.showSnackbar(
                message = exitConfirm,
                duration = SnackbarDuration.Indefinite,
            )
        }
        delay(2_000)
        shown.cancel()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { PdfBookSnackbarHost(snackbarHostState) },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSettings, colors = pdfBookIconButtonColors()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { peekMenuOpen = true },
                            colors = pdfBookIconButtonColors(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = null,
                            )
                        }
                        PdfBookDropdownMenu(
                            expanded = peekMenuOpen,
                            onDismissRequest = { peekMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (peekEnabled) R.string.peek_hidden_off else R.string.peek_hidden_on,
                                        ),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_eye),
                                        contentDescription = null,
                                    )
                                },
                                enabled = peekMenuEnabled,
                                onClick = {
                                    peekMenuOpen = false
                                    onPeekToggle()
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (peekEnabled) {
                PeekHiddenBanner(onStop = onPeekToggle)
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = folderShelfColumns(maxWidth.value.toInt())
            LaunchedEffect(scrollToUri, shelf.cards, hasResume, columns) {
                val uri = scrollToUri ?: return@LaunchedEffect
                val index = shelf.cards.indexOfFirst { it.uri == uri }
                if (!shouldClearScrollTarget(index)) return@LaunchedEffect
                val listIndex = folderShelfScrollIndex(index, hasResume)
                if (folderShelfIsLastRow(index, shelf.cards.size, columns)) {
                    gridState.scrollToItem(listIndex, scrollOffset = Int.MAX_VALUE / 4)
                } else {
                    gridState.animateScrollToItem(listIndex)
                }
                onScrollHandled()
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = safeDrawingGridContentPadding(
                    extraBottom = PdfBookSpacing.folderAddFabHeight + PdfBookSpacing.md,
                ),
                horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.md),
            ) {
                shelf.globalResume?.let { resume ->
                    item(key = "resume", span = { GridItemSpan(maxLineSpan) }) {
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
                            onHide = {
                                menuUri = null
                                onHide(card.uri)
                            },
                            onUnhide = { onUnhide(card.uri) },
                        )
                    }
                }
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
    onHide: () -> Unit,
    onUnhide: () -> Unit = {},
) {
    val lost = card.access == FolderAccess.Lost
    val contentAlpha = if (card.isHidden) 0.4f else 1f
    Surface(
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    if (shouldUnhideOnLongPress(card.isHidden)) onUnhide()
                    else onMenuExpand()
                },
            ),
    ) {
        Column(Modifier.padding(PdfBookSpacing.md)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .alpha(contentAlpha),
                    verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
                ) {
                    Text(
                        text = card.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (lost) {
                        Text(
                            text = card.pathTail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                            text = folderPathBookAnnotated(
                                card.pathTail,
                                countText,
                                SpanStyle(
                                    fontFamily = PdfBookNumeric.fontFamily,
                                    fontSize = 12.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (card.isHidden) {
                    HiddenBadge(Modifier.padding(top = PdfBookSpacing.xxs))
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
                        if (!card.isHidden) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hide_item)) },
                                onClick = onHide,
                            )
                        }
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
            CoverStrip(card.coverKeys, covers, dimmed = lost || card.isHidden)
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
                Column(Modifier.alpha(contentAlpha)) {
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
