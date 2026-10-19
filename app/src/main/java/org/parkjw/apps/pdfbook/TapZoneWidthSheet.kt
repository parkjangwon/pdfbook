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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
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
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans

const val TAP_ZONE_WIDTH_SHEET_TAG = "tap_zone_width_sheet"
const val TAP_ZONE_WIDTH_PREVIEW_TAG = "tap_zone_width_preview"

fun tapZonePreviewTag(zone: TapZone): String = "tap_zone_width_preview_" + zone.name

data class TapZoneWidthSheetColors(
    val container: Color,
    val divider: Color,
    val handle: Color,
    val title: Color,
    val body: Color,
    val accent: Color,
    val track: Color,
    val reset: Color,
    val doneContainer: Color,
    val doneContent: Color,
)

object TapZoneWidthSheetDefaults {
    val readerColors = TapZoneWidthSheetColors(
        container = ReaderOverlay,
        divider = ReaderChip,
        handle = ReaderTrack,
        title = ReaderOnOverlay,
        body = ReaderOnOverlayVariant,
        accent = ReaderAccent,
        track = ReaderTrack,
        reset = ReaderOnOverlay,
        doneContainer = ReaderOnOverlay,
        doneContent = ReaderOverlay,
    )

    @Composable
    fun appColors(): TapZoneWidthSheetColors {
        val scheme = MaterialTheme.colorScheme
        val ink = MaterialTheme.extendedColors
        return TapZoneWidthSheetColors(
            container = if (scheme.surface.luminance() < 0.5f) scheme.surfaceContainer else scheme.surfaceContainerLow,
            divider = ink.hairline,
            handle = ink.hairlineStrong,
            title = scheme.onSurface,
            body = scheme.onSurfaceVariant,
            accent = scheme.primary,
            track = scheme.surfaceVariant,
            reset = scheme.onSurface,
            doneContainer = ink.inkButton,
            doneContent = ink.onInkButton,
        )
    }
}

@Composable
fun TapZoneWidthSheet(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    colors: TapZoneWidthSheetColors = TapZoneWidthSheetDefaults.readerColors,
    previewDirection: ReadingDirection? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TAP_ZONE_WIDTH_SHEET_TAG)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.container)
            .drawBehind {
                drawLine(colors.divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
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
                .background(colors.handle, PdfBookShapeTokens.pill),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.reader_tap_width_title),
            color = colors.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.reader_tap_width_body),
            color = colors.body,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        if (previewDirection != null) {
            TapZoneWidthPreview(
                direction = previewDirection,
                percent = percent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 16.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_tap_width_percent, TAP_ZONE_SIDE_MIN_PERCENT),
                style = PdfBookNumeric.copy(fontSize = 11.sp, color = colors.body),
            )
            PdfBookReaderSlider(
                value = percent.toFloat(),
                onValueChange = { onPercentChange(snapTapZoneSidePercent(it)) },
                valueRange = TAP_ZONE_SIDE_MIN_PERCENT.toFloat()..TAP_ZONE_SIDE_MAX_PERCENT.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.track,
                    activeTickColor = colors.accent,
                    inactiveTickColor = colors.track,
                ),
                modifier = Modifier.weight(1f).testTag("tap_zone_width_slider"),
            )
            Text(
                text = stringResource(R.string.reader_tap_width_percent, TAP_ZONE_SIDE_MAX_PERCENT),
                style = PdfBookNumeric.copy(fontSize = 11.sp, color = colors.body),
            )
        }
        Text(
            text = withNumericSpans(
                stringResource(R.string.reader_tap_width_value, percent, DEFAULT_TAP_ZONE_SIDE_PERCENT),
                SpanStyle(fontFamily = PdfBookMonoFamily),
            ),
            color = colors.body,
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
                colors = pdfBookTextButtonColors(contentColor = colors.reset),
                modifier = Modifier.testTag("tap_zone_width_reset"),
            ) {
                Text(stringResource(R.string.reader_tap_width_reset))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.testTag("tap_zone_width_done"),
                shape = ButtonCorner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.doneContainer,
                    contentColor = colors.doneContent,
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

@Composable
private fun TapZoneWidthPreview(
    direction: ReadingDirection,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = MaterialTheme.extendedColors
    val shape = RoundedCornerShape(6.dp)
    val spans = tapZoneSpans(direction, tapZoneSideFraction(percent))
    Row(
        modifier
            .size(width = 120.dp, height = 168.dp)
            .testTag(TAP_ZONE_WIDTH_PREVIEW_TAG)
            .clip(shape)
            .background(scheme.surfaceContainerLowest)
            .border(1.dp, ink.hairlineStrong, shape),
    ) {
        spans.forEachIndexed { index, span ->
            Box(
                modifier = Modifier
                    .weight(span.end - span.start)
                    .fillMaxHeight()
                    .testTag(tapZonePreviewTag(span.zone))
                    .background(
                        if (span.zone == TapZone.Menu) scheme.surfaceContainerLowest else scheme.surfaceContainerHigh,
                    )
                    .drawBehind {
                        if (index > 0) {
                            drawLine(
                                color = ink.hairlineStrong,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        when (span.zone) {
                            TapZone.Previous -> R.string.reader_zone_previous
                            TapZone.Menu -> R.string.reader_zone_menu
                            TapZone.Next -> R.string.reader_zone_next
                        },
                    ),
                    color = scheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
