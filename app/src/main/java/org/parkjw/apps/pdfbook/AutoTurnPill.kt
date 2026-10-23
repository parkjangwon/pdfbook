package org.parkjw.apps.pdfbook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

const val AUTO_TURN_STATUS_TAG = "reader_auto_turn_status"
private val AUTO_TURN_PILL_HEIGHT = 44.dp

@Composable
fun AutoTurnStatusHost(
    text: String?,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val last = remember { arrayOfNulls<String>(1) }
    if (text != null) last[0] = text
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn(tween(END_NOTICE_FADE_MILLIS)),
            exit = fadeOut(tween(END_NOTICE_FADE_MILLIS)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset),
        ) {
            AutoTurnStatusPill(text ?: last[0].orEmpty())
        }
    }
}

@Composable
fun AutoTurnStatusPill(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .testTag(AUTO_TURN_STATUS_TAG)
            .height(AUTO_TURN_PILL_HEIGHT)
            .clip(PdfBookShapeTokens.pill)
            .background(ReaderChip, PdfBookShapeTokens.pill)
            .border(1.dp, ReaderTrack, PdfBookShapeTokens.pill)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = ReaderOnOverlay,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}
