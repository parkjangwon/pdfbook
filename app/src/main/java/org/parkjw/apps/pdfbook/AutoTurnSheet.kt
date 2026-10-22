package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.SheetTopCorner

const val AUTO_TURN_SHEET_TAG = "auto_turn_sheet"
const val AUTO_TURN_START_TAG = "auto_turn_start"

fun autoTurnIntervalTag(seconds: Int): String = "auto_turn_interval_$seconds"

@Composable
fun AutoTurnSheet(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = autoTurnSecondsOrDefault(seconds)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AUTO_TURN_SHEET_TAG)
            .clip(SheetTopCorner)
            .background(ReaderOverlay)
            .drawBehind {
                drawLine(ReaderChip, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .safeDrawingBottomBarPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 32.dp, height = 4.dp)
                .background(ReaderTrack, PdfBookShapeTokens.pill),
        )
        Text(
            text = stringResource(R.string.reader_auto_turn),
            color = ReaderOnOverlay,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = stringResource(R.string.reader_auto_turn_body),
            color = ReaderOnOverlayVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        AUTO_TURN_INTERVAL_SECONDS.chunked(4).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { value ->
                    AutoTurnIntervalOption(
                        seconds = value,
                        selected = value == selected,
                        onClick = { onSecondsChange(value) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(AUTO_TURN_START_TAG),
            shape = ButtonCorner,
            colors = ButtonDefaults.buttonColors(
                containerColor = ReaderOnOverlay,
                contentColor = ReaderOverlay,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp,
            ),
        ) {
            Text(stringResource(R.string.reader_auto_turn_start))
        }
    }
}

@Composable
private fun AutoTurnIntervalOption(
    seconds: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = PdfBookShapeTokens.pill
    Box(
        modifier = modifier
            .height(44.dp)
            .testTag(autoTurnIntervalTag(seconds))
            .clip(shape)
            .background(if (selected) ReaderOnOverlay else ReaderChip)
            .border(1.dp, if (selected) ReaderOnOverlay else ReaderTrack, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.reader_auto_turn_interval, seconds),
            color = if (selected) ReaderOverlay else ReaderOnOverlay,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
