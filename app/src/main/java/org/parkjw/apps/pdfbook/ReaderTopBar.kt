package org.parkjw.apps.pdfbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.parkjw.apps.pdfbook.ui.safeDrawingTopBarPadding
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors

internal enum class ReaderTopBarMode {
    Compact,
    Medium,
    Expanded,
}

internal fun readerTopBarMode(widthDp: Int): ReaderTopBarMode = when {
    widthDp < 400 -> ReaderTopBarMode.Compact
    widthDp < 600 -> ReaderTopBarMode.Medium
    else -> ReaderTopBarMode.Expanded
}

internal fun readerMoreMenuOffset(
    anchorBounds: IntRect,
    popupWidth: Int,
    marginPx: Int,
    barPaddingPx: Int,
): IntOffset = IntOffset(
    x = anchorBounds.right + barPaddingPx - popupWidth - marginPx,
    y = anchorBounds.bottom + barPaddingPx + barPaddingPx,
)

@Composable
internal fun ReaderOverlayTop(
    title: String,
    twoPage: Boolean,
    direction: ReadingDirection,
    onBack: () -> Unit,
    onDirectionChange: (ReadingDirection) -> Unit,
    showPageMode: Boolean = true,
    modifier: Modifier = Modifier,
    windowWidthDp: Int = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.width.toDp().value.toInt()
    },
    onOpenBookList: (() -> Unit)? = null,
    onOpenPageNav: () -> Unit = {},
    bookmarked: Boolean? = null,
    onToggleBookmark: () -> Unit = {},
) {
    val mode = readerTopBarMode(windowWidthDp)
    var moreExpanded by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .testTag("reader_overlay_top")
            .background(ReaderOverlay)
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingTopBarPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
            modifier = Modifier.testTag("reader_back"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.reader_back),
                tint = ReaderOnOverlay,
            )
        }
        Text(
            text = title,
            color = ReaderOnOverlay,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("reader_title")
                .padding(start = 4.dp),
        )
        if (bookmarked != null) {
            IconButton(
                onClick = onToggleBookmark,
                colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                modifier = Modifier
                    .testTag("reader_bookmark_toggle")
                    .semantics {
                        role = Role.Checkbox
                        toggleableState = ToggleableState(bookmarked)
                    },
            ) {
                Icon(
                    painter = painterResource(if (bookmarked) R.drawable.ic_bookmark_on else R.drawable.ic_bookmark),
                    contentDescription = stringResource(
                        if (bookmarked) R.string.reader_bookmark_remove else R.string.reader_bookmark_add,
                    ),
                    tint = ReaderOnOverlay,
                )
            }
        }
        IconButton(
            onClick = onOpenPageNav,
            colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
            modifier = Modifier.testTag("reader_page_nav_button"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_page_nav),
                contentDescription = stringResource(R.string.reader_page_nav),
                tint = ReaderOnOverlay,
            )
        }
        if (mode == ReaderTopBarMode.Expanded) {
            if (onOpenBookList != null) {
                IconButton(
                    onClick = onOpenBookList,
                    colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                    modifier = Modifier.testTag("reader_book_list_button"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_list),
                        contentDescription = stringResource(R.string.reader_book_list),
                        tint = ReaderOnOverlay,
                    )
                }
            }
            if (showPageMode) {
                Icon(
                    painter = painterResource(
                        if (twoPage) R.drawable.ic_page_spread else R.drawable.ic_page_single,
                    ),
                    contentDescription = stringResource(
                        if (twoPage) R.string.reader_mode_double else R.string.reader_mode_single,
                    ),
                    tint = ReaderOnOverlay,
                    modifier = Modifier.padding(horizontal = 12.dp).testTag("reader_page_mode"),
                )
            }
        }
        if (mode != ReaderTopBarMode.Compact) {
            IconButton(
                onClick = { onDirectionChange(toggledDirection(direction)) },
                colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                modifier = Modifier.testTag("reader_direction_toggle"),
            ) {
                Icon(
                    painter = painterResource(
                        if (direction == ReadingDirection.RTL) {
                            R.drawable.ic_read_rtl
                        } else {
                            R.drawable.ic_read_ltr
                        },
                    ),
                    contentDescription = stringResource(
                        if (direction == ReadingDirection.RTL) {
                            R.string.settings_direction_rtl
                        } else {
                            R.string.settings_direction_ltr
                        },
                    ),
                    tint = ReaderOnOverlay,
                )
            }
        }
        val moreHasItems = mode == ReaderTopBarMode.Compact || onOpenBookList != null || showPageMode
        if (mode != ReaderTopBarMode.Expanded && moreHasItems) {
            IconButton(
                onClick = { moreExpanded = true },
                colors = pdfBookIconButtonColors(contentColor = ReaderOnOverlay),
                modifier = Modifier.testTag("reader_more_button"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = stringResource(R.string.reader_more),
                    tint = ReaderOnOverlay,
                )
            }
        }
        ReaderMoreMenu(
            expanded = moreExpanded,
            onDismissRequest = { moreExpanded = false },
            direction = direction,
            onDirectionChange = onDirectionChange,
            showDirection = mode == ReaderTopBarMode.Compact,
            onOpenBookList = onOpenBookList,
            showPageMode = showPageMode,
            twoPage = twoPage,
        )
    }
}

@Composable
private fun ReaderMoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    direction: ReadingDirection,
    onDirectionChange: (ReadingDirection) -> Unit,
    showDirection: Boolean,
    onOpenBookList: (() -> Unit)?,
    showPageMode: Boolean,
    twoPage: Boolean,
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded
    if (!transitionState.currentState && !transitionState.targetState) return

    val density = LocalDensity.current
    val positionProvider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val margin = with(density) { 8.dp.roundToPx() }
                val barPadding = with(density) { 4.dp.roundToPx() }
                return readerMoreMenuOffset(
                    anchorBounds = anchorBounds,
                    popupWidth = popupContentSize.width,
                    marginPx = margin,
                    barPaddingPx = barPadding,
                )
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(visibleState = transitionState, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier
                    .width(232.dp)
                    .clip(CardCorner)
                    .background(ReaderOverlay)
                    .border(1.dp, ReaderTrack, CardCorner)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("reader_more_menu")
                    .padding(vertical = 8.dp),
            ) {
                if (onOpenBookList != null) {
                    ReaderMenuRow(
                        iconRes = R.drawable.ic_book_list,
                        label = stringResource(R.string.reader_book_list),
                        testTag = "reader_more_book_list",
                        onClick = {
                            onDismissRequest()
                            onOpenBookList()
                        },
                    )
                }
                if (showDirection) {
                    ReaderMenuRow(
                        iconRes = if (direction == ReadingDirection.RTL) {
                            R.drawable.ic_read_rtl
                        } else {
                            R.drawable.ic_read_ltr
                        },
                        label = stringResource(R.string.reader_direction_menu),
                        value = stringResource(
                            if (direction == ReadingDirection.RTL) {
                                R.string.settings_direction_rtl
                            } else {
                                R.string.settings_direction_ltr
                            },
                        ),
                        testTag = "reader_more_direction",
                        onClick = {
                            onDirectionChange(toggledDirection(direction))
                            onDismissRequest()
                        },
                    )
                }
                if (showPageMode) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(1.dp)
                            .background(ReaderChip),
                    )
                    ReaderMenuRow(
                        iconRes = if (twoPage) R.drawable.ic_page_spread else R.drawable.ic_page_single,
                        label = stringResource(
                            if (twoPage) R.string.reader_mode_status_double else R.string.reader_mode_status_single,
                        ),
                        tint = ReaderOnOverlayVariant,
                        testTag = "reader_more_status",
                        onClick = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderMenuRow(
    iconRes: Int,
    label: String,
    testTag: String,
    onClick: (() -> Unit)?,
    value: String? = null,
    tint: Color = ReaderOnOverlay,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = ReaderOnOverlay,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (value != null) {
                Text(
                    text = value,
                    color = ReaderOnOverlayVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
