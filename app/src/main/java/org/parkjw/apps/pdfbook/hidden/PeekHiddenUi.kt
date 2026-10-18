package org.parkjw.apps.pdfbook.hidden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors

private val PeekBannerCorner = RoundedCornerShape(10.dp)
private val HiddenBadgeCorner = RoundedCornerShape(6.dp)

@Composable
fun PeekHiddenBanner(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val horizontal = if (maxWidth < 600.dp) {
            PdfBookSpacing.screenHorizontalFolded
        } else {
            PdfBookSpacing.screenHorizontalUnfolded
        }
        Surface(
            shape = PeekBannerCorner,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontal, vertical = PdfBookSpacing.xs),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PdfBookSpacing.sm, vertical = PdfBookSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.peek_hidden_banner),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.peek_hidden_hint),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onStop, colors = pdfBookTextButtonColors()) {
                    Text(
                        text = stringResource(R.string.peek_hidden_stop),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun HiddenBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = HiddenBadgeCorner,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.hairlineStrong),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PdfBookSpacing.xxs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_eye_off),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.hidden_badge),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
