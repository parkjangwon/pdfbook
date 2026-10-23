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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookReaderSlider
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderAccent
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

const val READER_BRIGHTNESS_SHEET_TAG = "reader_brightness_sheet"
const val READER_BRIGHTNESS_SWITCH_TAG = "reader_brightness_switch"
const val READER_BRIGHTNESS_SLIDER_TAG = "reader_brightness_slider"

@Composable
fun ReaderBrightnessSheet(
    followSystem: Boolean,
    percent: Int,
    onFollowSystemChange: (Boolean) -> Unit,
    onPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(READER_BRIGHTNESS_SHEET_TAG)
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
            text = stringResource(R.string.reader_brightness),
            color = ReaderOnOverlay,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.reader_brightness_system),
                color = ReaderOnOverlayVariant,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = followSystem,
                onCheckedChange = onFollowSystemChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ReaderOnOverlay,
                    checkedTrackColor = ReaderAccent,
                    checkedBorderColor = ReaderAccent,
                    uncheckedThumbColor = ReaderOnOverlayVariant,
                    uncheckedTrackColor = ReaderTrack,
                    uncheckedBorderColor = ReaderTrack,
                ),
                modifier = Modifier.testTag(READER_BRIGHTNESS_SWITCH_TAG),
            )
        }
        if (!followSystem) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.reader_brightness_percent, READER_BRIGHTNESS_MIN_PERCENT),
                    style = PdfBookNumeric.copy(fontSize = 11.sp, color = ReaderOnOverlayVariant),
                )
                PdfBookReaderSlider(
                    value = percent.toFloat(),
                    onValueChange = { onPercentChange(snapReaderBrightnessPercent(it)) },
                    valueRange = READER_BRIGHTNESS_MIN_PERCENT.toFloat()..READER_BRIGHTNESS_MAX_PERCENT.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = ReaderAccent,
                        activeTrackColor = ReaderAccent,
                        inactiveTrackColor = ReaderTrack,
                        activeTickColor = ReaderAccent,
                        inactiveTickColor = ReaderTrack,
                    ),
                    modifier = Modifier.weight(1f).testTag(READER_BRIGHTNESS_SLIDER_TAG),
                )
                Text(
                    text = stringResource(R.string.reader_brightness_percent, READER_BRIGHTNESS_MAX_PERCENT),
                    style = PdfBookNumeric.copy(fontSize = 11.sp, color = ReaderOnOverlayVariant),
                )
            }
            Text(
                text = stringResource(R.string.reader_brightness_percent, percent),
                style = PdfBookNumeric.copy(fontSize = 13.sp, color = ReaderOnOverlay),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp)
                    .testTag("reader_brightness_value"),
            )
        }
    }
}
