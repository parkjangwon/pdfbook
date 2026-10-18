package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.ButtonCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookMonoFamily
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookReaderSlider
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

const val TAP_ZONE_WIDTH_SHEET_TAG = "tap_zone_width_sheet"

@Composable
fun TapZoneWidthSheet(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TAP_ZONE_WIDTH_SHEET_TAG)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.reader_tap_width_title),
            color = ReaderOnOverlay,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.reader_tap_width_body),
            color = ReaderOnOverlayVariant,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_tap_width_percent, TAP_ZONE_SIDE_MIN_PERCENT),
                style = PdfBookNumeric.copy(fontSize = 11.sp, color = ReaderOnOverlayVariant),
            )
            PdfBookReaderSlider(
                value = percent.toFloat(),
                onValueChange = { onPercentChange(snapTapZoneSidePercent(it)) },
                valueRange = TAP_ZONE_SIDE_MIN_PERCENT.toFloat()..TAP_ZONE_SIDE_MAX_PERCENT.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = ReaderAccent,
                    activeTrackColor = ReaderAccent,
                    inactiveTrackColor = ReaderTrack,
                    activeTickColor = ReaderAccent,
                    inactiveTickColor = ReaderTrack,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.reader_tap_width_percent, TAP_ZONE_SIDE_MAX_PERCENT),
                style = PdfBookNumeric.copy(fontSize = 11.sp, color = ReaderOnOverlayVariant),
            )
        }
        Text(
            text = withNumericSpans(
                stringResource(R.string.reader_tap_width_value, percent, DEFAULT_TAP_ZONE_SIDE_PERCENT),
                SpanStyle(fontFamily = PdfBookMonoFamily),
            ),
            color = ReaderOnOverlayVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onPercentChange(DEFAULT_TAP_ZONE_SIDE_PERCENT) },
                colors = pdfBookTextButtonColors(contentColor = ReaderOnOverlay),
            ) {
                Text(stringResource(R.string.reader_tap_width_reset))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onDone,
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
                Text(stringResource(R.string.reader_tap_width_done))
            }
        }
    }
}
