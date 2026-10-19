package org.parkjw.apps.pdfbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

val END_SWIPE_THRESHOLD = 48.dp
private val END_NOTICE_HEIGHT = 56.dp
private val END_NOTICE_MAX_WIDTH = 480.dp
private val END_NOTICE_SIDE_MARGIN = 16.dp

@Composable
fun EndNoticeHost(
    notice: EndNotice?,
    twoPage: Boolean,
    direction: ReadingDirection,
    bottomInset: Dp,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val last = remember { arrayOfNulls<EndNotice>(1) }
    if (notice != null) last[0] = notice
    Box(modifier.fillMaxSize()) {
        val area = if (twoPage) {
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(if (direction == ReadingDirection.RTL) Alignment.BottomStart else Alignment.BottomEnd)
        } else {
            Modifier.fillMaxSize()
        }
        Box(area) {
            AnimatedVisibility(
                visible = notice != null,
                enter = fadeIn(tween(END_NOTICE_FADE_MILLIS)),
                exit = fadeOut(tween(END_NOTICE_FADE_MILLIS)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = END_NOTICE_SIDE_MARGIN, end = END_NOTICE_SIDE_MARGIN, bottom = bottomInset),
            ) {
                (notice ?: last[0])?.let { EndNoticeCard(it, onOpen) }
            }
        }
    }
}

@Composable
fun EndNoticeCard(notice: EndNotice, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .widthIn(max = END_NOTICE_MAX_WIDTH)
            .fillMaxWidth()
            .height(END_NOTICE_HEIGHT)
            .background(ReaderChip, CardCorner)
            .border(1.dp, ReaderTrack, CardCorner)
            .pointerInput(Unit) { detectTapGestures { } }
            .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite }
            .padding(start = 16.dp, end = if (notice is EndNotice.NextBook) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_book),
            contentDescription = null,
            tint = ReaderOnOverlayVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        when (notice) {
            is EndNotice.NextBook -> {
                val title = titleOf(notice.book.name)
                val full = stringResource(R.string.reader_next_book, title)
                val titleStart = full.lastIndexOf(title).coerceAtLeast(0)
                val text = buildAnnotatedString {
                    withStyle(SpanStyle(color = ReaderOnOverlayVariant)) { append(full.substring(0, titleStart)) }
                    withStyle(SpanStyle(color = ReaderOnOverlay, fontWeight = FontWeight.SemiBold)) {
                        append(full.substring(titleStart))
                    }
                }
                Text(
                    text = text,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val openLabel = stringResource(R.string.reader_next_book_open_description, title)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clearAndSetSemantics {
                            contentDescription = openLabel
                            role = Role.Button
                            onClick { onOpen(); true }
                        }
                        .clickable(role = Role.Button, onClick = onOpen)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.reader_next_book_open),
                        color = ReaderAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            EndNotice.NoNextBook -> Text(
                text = stringResource(R.string.reader_no_next_book),
                color = ReaderOnOverlay,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
