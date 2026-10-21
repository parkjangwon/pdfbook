package org.parkjw.apps.pdfbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.ui.theme.PadCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

@Composable
fun TabletopPads(
    direction: ReadingDirection,
    currentPage: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onMenu: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    slider: (@Composable () -> Unit)? = null,
) {
    val zones = tabletopPadOrder(direction)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ReaderOverlay)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            .testTag("tabletop_pads"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (slider != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                slider()
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            zones.forEach { zone ->
                val weight = if (zone == TapZone.Menu) 0.6f else 1f
                val onClick = when (zone) {
                    TapZone.Previous -> onPrevious
                    TapZone.Menu -> onMenu
                    TapZone.Next -> onNext
                }
                TabletopPad(
                    zone = zone,
                    direction = direction,
                    onClick = onClick,
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.progress, currentPage, pageCount),
            style = PdfBookNumeric.copy(fontSize = 12.sp, color = ReaderOnOverlay),
            modifier = Modifier.testTag("tabletop_page_number"),
        )
    }
}

@Composable
private fun TabletopPad(
    zone: TapZone,
    direction: ReadingDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val label = when (zone) {
        TapZone.Previous -> R.string.reader_zone_previous
        TapZone.Menu -> R.string.reader_zone_menu
        TapZone.Next -> R.string.reader_zone_next
    }
    val iconRes = when (zone) {
        TapZone.Menu -> R.drawable.ic_menu
        TapZone.Next -> if (direction == ReadingDirection.RTL) {
            R.drawable.ic_chevron_left
        } else {
            R.drawable.ic_chevron_right
        }
        TapZone.Previous -> if (direction == ReadingDirection.RTL) {
            R.drawable.ic_chevron_right
        } else {
            R.drawable.ic_chevron_left
        }
    }
    Column(
        modifier = modifier
            .background(if (pressed) ReaderTrack else ReaderChip, PadCorner)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .testTag("tabletop_pad_${zone.name}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(label),
            tint = ReaderOnOverlay,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(label),
            color = ReaderOnOverlay,
            fontSize = 12.sp,
        )
    }
}

fun tabletopPadOrder(direction: ReadingDirection): List<TapZone> =
    if (direction == ReadingDirection.RTL) {
        listOf(TapZone.Next, TapZone.Menu, TapZone.Previous)
    } else {
        listOf(TapZone.Previous, TapZone.Menu, TapZone.Next)
    }
