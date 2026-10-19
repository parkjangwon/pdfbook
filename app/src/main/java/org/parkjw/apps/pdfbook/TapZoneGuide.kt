package org.parkjw.apps.pdfbook

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneCenter
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneDivider
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneLabel
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneSide

const val TAP_ZONE_GUIDE_TAG = "tap_zone_guide"

@Composable
fun TapZoneGuide(
    direction: ReadingDirection,
    modifier: Modifier = Modifier,
    sideFraction: Float = DEFAULT_TAP_ZONE_SIDE_FRACTION,
) {
    val spans = tapZoneSpans(direction, sideFraction)
    Row(modifier.fillMaxSize().testTag(TAP_ZONE_GUIDE_TAG)) {
        spans.forEachIndexed { index, span ->
            if (span.width > 0f) {
                TapZoneArea(
                    zone = span.zone,
                    icon = when (index) {
                        0 -> R.drawable.ic_chevron_left
                        spans.lastIndex -> R.drawable.ic_chevron_right
                        else -> R.drawable.ic_menu
                    },
                    modifier = Modifier
                        .weight(span.width)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun TapZoneArea(
    zone: TapZone,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    val isMenu = zone == TapZone.Menu
    Box(
        modifier = modifier
            .background(if (isMenu) ReaderTapZoneCenter else ReaderTapZoneSide)
            .drawBehind {
                if (isMenu) {
                    val stroke = 1.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    val leftX = stroke / 2f
                    val rightX = size.width - stroke / 2f
                    drawLine(ReaderTapZoneDivider, Offset(leftX, 0f), Offset(leftX, size.height), stroke, pathEffect = dash)
                    drawLine(ReaderTapZoneDivider, Offset(rightX, 0f), Offset(rightX, size.height), stroke, pathEffect = dash)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        TapZoneLabel(zone, icon)
    }
}

@Composable
private fun TapZoneLabel(zone: TapZone, @DrawableRes icon: Int) {
    val label = stringResource(
        when (zone) {
            TapZone.Previous -> R.string.reader_zone_previous
            TapZone.Menu -> R.string.reader_zone_menu
            TapZone.Next -> R.string.reader_zone_next
        },
    )
    Column(
        modifier = Modifier
            .background(ReaderTapZoneLabel, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = ReaderOnOverlay,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            color = ReaderOnOverlay,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

