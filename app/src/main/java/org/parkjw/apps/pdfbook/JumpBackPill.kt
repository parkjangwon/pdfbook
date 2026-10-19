package org.parkjw.apps.pdfbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.PdfBookMonoFamily
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

private val JUMP_BACK_HEIGHT = 44.dp
private val JUMP_BACK_ICON = 20.dp

@Composable
fun JumpBackHost(
    page: Int?,
    bottomInset: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val last = remember { IntArray(1) }
    if (page != null) last[0] = page
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = page != null,
            enter = fadeIn(tween(END_NOTICE_FADE_MILLIS)),
            exit = fadeOut(tween(END_NOTICE_FADE_MILLIS)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset),
        ) {
            JumpBackPill(page = page ?: last[0], onClick = onClick)
        }
    }
}

@Composable
fun JumpBackPill(page: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val number = String.format(LocalConfiguration.current.locales[0], "%d", page)
    val full = stringResource(R.string.reader_jump_back, page)
    val start = full.indexOf(number).coerceAtLeast(0)
    val end = (start + number.length).coerceAtMost(full.length)
    val text = buildAnnotatedString {
        append(full.substring(0, start))
        withStyle(SpanStyle(fontFamily = PdfBookMonoFamily, fontWeight = FontWeight.SemiBold)) {
            append(full.substring(start, end))
        }
        append(full.substring(end))
    }
    Row(
        modifier = modifier
            .testTag("jump_back_pill")
            .height(JUMP_BACK_HEIGHT)
            .clip(PdfBookShapeTokens.pill)
            .background(ReaderChip, PdfBookShapeTokens.pill)
            .border(1.dp, ReaderTrack, PdfBookShapeTokens.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(start = 14.dp, end = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_jump_back),
            contentDescription = null,
            tint = ReaderOnOverlay,
            modifier = Modifier.size(JUMP_BACK_ICON),
        )
        Text(
            text = text,
            color = ReaderOnOverlay,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
