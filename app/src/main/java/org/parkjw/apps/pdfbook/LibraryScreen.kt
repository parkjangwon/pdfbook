package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.hidden.HiddenBadge
import org.parkjw.apps.pdfbook.hidden.PeekHiddenBanner
import org.parkjw.apps.pdfbook.hidden.findLibraryResumeBook
import org.parkjw.apps.pdfbook.hidden.shouldUnhideOnLongPress
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.safeDrawingGridContentPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingHorizontalPadding
import org.parkjw.apps.pdfbook.ui.safeDrawingPadding
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
            modifier = Modifier.height(40.dp).testTag("folder_pick_button"),
        ) { Text(buttonLabel) }
    }
}

const val SORT_BUTTON_TAG = "library_sort_button"
const val SORT_CHECK_TAG = "library_sort_check"
private val SORT_MENU_WIDTH = 176.dp

fun sortOptionTag(sort: BookSort): String = "library_sort_option_" + sort.name

fun sortLabelRes(sort: BookSort): Int = when (sort) {
    BookSort.Recent -> R.string.sort_recent
    BookSort.Name -> R.string.sort_name
}

@Composable
fun LibraryScreen(
    listing: Listing,
    states: Map<String, BookState>,
    onOpen: (BookInfo) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    covers: Map<String, Bitmap?> = emptyMap(),
    onBack: (() -> Unit)? = null,
    snackbarMessage: String? = null,
    snackbarActionLabel: String? = null,
    onSnackbarShown: () -> Unit = {},
    onSnackbarAction: () -> Unit = {},
    onHideBook: (BookInfo) -> Unit = {},
    onUnhideBook: (BookInfo) -> Unit = {},
    onMarkFinished: (BookInfo) -> Unit = {},
    onStartOver: (BookInfo) -> Unit = {},
    peekEnabled: Boolean = false,
    peekMenuEnabled: Boolean = false,
    onPeekToggle: () -> Unit = {},
    peekHiddenBookIds: Set<String> = emptySet(),
    showPeekMenu: Boolean = false,
    showSortMenu: Boolean = false,
    sort: BookSort = BookSort.Recent,
    onSortChange: (BookSort) -> Unit = {},
    subfolders: List<SubfolderItem> = emptyList(),
    onOpenSubfolder: (SubfolderItem) -> Unit = {},
    onHideSubfolder: (SubfolderItem) -> Unit = {},
    onUnhideSubfolder: (SubfolderItem) -> Unit = {},
    pathLine: String? = null,
    resumeBooks: List<BookInfo>? = null,
) {
    val byName = sort == BookSort.Name
    var menuBookUri by remember { mutableStateOf<String?>(null) }
    var peekMenuOpen by remember { mutableStateOf(showPeekMenu) }
    var sortMenuOpen by remember { mutableStateOf(showSortMenu) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sorted = remember(listing, states, byName) { sortBooks(listing.books, states, byName) }
    val resume = remember(byName, resumeBooks, sorted, states, peekHiddenBookIds) {
        when {
            byName -> null
            resumeBooks != null -> findLibraryResumeBook(sortBooks(resumeBooks, states, false), states, peekHiddenBookIds)
            else -> findLibraryResumeBook(sorted, states, peekHiddenBookIds)
        }
    }
    val currentOnSnackbarAction by rememberUpdatedState(onSnackbarAction)
    val currentOnSnackbarShown by rememberUpdatedState(onSnackbarShown)
    LaunchedEffect(snackbarMessage, snackbarActionLabel) {
        val message = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(message = message, actionLabel = snackbarActionLabel)
        if (result == SnackbarResult.ActionPerformed) currentOnSnackbarAction()
        currentOnSnackbarShown()
    }
    Box(modifier.fillMaxSize().testTag("library_screen")) {
    Column(Modifier.fillMaxSize()) {
        SafeTopBar(
            modifier = Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        colors = pdfBookIconButtonColors(),
                        modifier = Modifier.testTag("library_back"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.reader_back),
                        )
                    }
                }
                Column {
                    Text(
                        listing.folderName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pathLine != null) {
                        Text(
                            pathLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.StartEllipsis,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    val sortTitle = stringResource(R.string.sort_menu_title)
                    val sortLabel = stringResource(sortLabelRes(sort))
                    val density = LocalDensity.current
                    var sortButtonWidth by remember { mutableStateOf(0.dp) }
                    Row(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .onSizeChanged { sortButtonWidth = with(density) { it.width.toDp() } }
                            .testTag(SORT_BUTTON_TAG)
                            .clip(PdfBookShapeTokens.pill)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(role = Role.Button) { sortMenuOpen = true }
                            .semantics {
                                contentDescription = sortTitle
                                stateDescription = sortLabel
                            }
                            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = sortLabel,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_expand_more),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    PdfBookDropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                        modifier = Modifier.width(SORT_MENU_WIDTH),
                        offset = DpOffset(sortButtonWidth - SORT_MENU_WIDTH, 0.dp),
                    ) {
                        BookSort.entries.forEach { option ->
                            val chosen = option == sort
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(sortLabelRes(option)),
                                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = {
                                    Box(Modifier.size(20.dp)) {
                                        if (chosen) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_check),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .testTag(SORT_CHECK_TAG),
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    sortMenuOpen = false
                                    if (!chosen) onSortChange(option)
                                },
                                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag(sortOptionTag(option))
                                    .semantics {
                                        role = Role.RadioButton
                                        selected = chosen
                                    },
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onSettings,
                    colors = pdfBookIconButtonColors(),
                    modifier = Modifier.testTag("library_settings"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
                Box {
                    IconButton(
                        onClick = { peekMenuOpen = true },
                        colors = pdfBookIconButtonColors(),
                        modifier = Modifier.testTag("library_more"),
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
        if (peekEnabled) {
            PeekHiddenBanner(onStop = onPeekToggle)
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
                            menuExpanded = menuBookUri == "resume:" + resume.uri,
                            onMenuExpand = { menuBookUri = "resume:" + resume.uri },
                            onMenuDismiss = { menuBookUri = null },
                            onHide = {
                                menuBookUri = null
                                onHideBook(resume)
                            },
                            onMarkFinished = {
                                menuBookUri = null
                                onMarkFinished(resume)
                            },
                            onStartOver = {
                                menuBookUri = null
                                onStartOver(resume)
                            },
                        )
                    }
                }
                items(subfolders, key = { "folder:" + it.node.uri }) { item ->
                    SubfolderCell(
                        item = item,
                        covers = covers,
                        onOpen = { onOpenSubfolder(item) },
                        menuExpanded = menuBookUri == "folder:" + item.node.uri,
                        onMenuExpand = { menuBookUri = "folder:" + item.node.uri },
                        onMenuDismiss = { menuBookUri = null },
                        onHide = {
                            menuBookUri = null
                            onHideSubfolder(item)
                        },
                        onUnhide = {
                            menuBookUri = null
                            onUnhideSubfolder(item)
                        },
                    )
                }
                items(sorted, key = { it.uri }) { book ->
                    val isHidden = book.uri in peekHiddenBookIds
                    BookCell(
                        book,
                        states[bookKey(book.uri, book.size)],
                        onOpen,
                        covers[bookKey(book.uri, book.size)],
                        covers.containsKey(bookKey(book.uri, book.size)),
                        menuExpanded = menuBookUri == book.uri,
                        onMenuExpand = { menuBookUri = book.uri },
                        onMenuDismiss = { menuBookUri = null },
                        onHide = {
                            menuBookUri = null
                            onHideBook(book)
                        },
                        onUnhide = { onUnhideBook(book) },
                        onMarkFinished = {
                            menuBookUri = null
                            onMarkFinished(book)
                        },
                        onStartOver = {
                            menuBookUri = null
                            onStartOver(book)
                        },
                        isHidden = isHidden,
                    )
                }
            }
        }
    }
        PdfBookSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResumeCard(
    book: BookInfo,
    state: BookState,
    onOpen: (BookInfo) -> Unit,
    cover: Bitmap?,
    coverProvided: Boolean,
    menuExpanded: Boolean,
    onMenuExpand: () -> Unit,
    onMenuDismiss: () -> Unit,
    onHide: () -> Unit,
    onMarkFinished: () -> Unit,
    onStartOver: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
    Surface(
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.hairline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_resume_card")
            .combinedClickable(onClick = { onOpen(book) }, onLongClick = onMenuExpand),
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
        PdfBookDropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
            BookProgressMenuItems(state, onMarkFinished, onStartOver)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hide_item)) },
                onClick = onHide,
            )
        }
    }
}

@Composable
private fun BookProgressMenuItems(
    state: BookState?,
    onMarkFinished: () -> Unit,
    onStartOver: () -> Unit,
) {
    if (canMarkFinished(state)) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.mark_finished)) },
            onClick = onMarkFinished,
        )
    }
    if (canStartOver(state)) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.start_over)) },
            onClick = onStartOver,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCell(
    book: BookInfo,
    state: BookState?,
    onOpen: (BookInfo) -> Unit,
    cover: Bitmap?,
    coverProvided: Boolean,
    menuExpanded: Boolean,
    onMenuExpand: () -> Unit,
    onMenuDismiss: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit = {},
    onMarkFinished: () -> Unit = {},
    onStartOver: () -> Unit = {},
    isHidden: Boolean = false,
) {
    Box(Modifier.fillMaxWidth()) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("library_book")
            .combinedClickable(
                onClick = { onOpen(book) },
                onLongClick = {
                    if (shouldUnhideOnLongPress(isHidden)) onUnhide()
                    else onMenuExpand()
                },
            ),
        verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
    ) {
        Box {
            Cover(
                book,
                Modifier
                    .fillMaxWidth()
                    .alpha(if (isHidden) 0.4f else 1f),
                cover,
                coverProvided,
            )
            if (isHidden) {
                HiddenBadge(Modifier.align(Alignment.TopEnd).padding(PdfBookSpacing.xxs))
            }
        }
        Column(Modifier.alpha(if (isHidden) 0.4f else 1f)) {
            Text(
                titleOf(book.name),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ProgressInfo(state ?: BookState())
        }
    }
        if (!isHidden) {
            PdfBookDropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                BookProgressMenuItems(state, onMarkFinished, onStartOver)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.hide_item)) },
                    onClick = onHide,
                )
            }
        }
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
