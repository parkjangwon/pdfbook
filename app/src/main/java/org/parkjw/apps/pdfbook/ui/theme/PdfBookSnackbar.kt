package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.parkjw.apps.pdfbook.R

@Composable
fun PdfBookSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val actionLabel = snackbarData.visuals.actionLabel
    val withDismissAction = snackbarData.visuals.withDismissAction
    val ink = MaterialTheme.colorScheme.inverseOnSurface
    Surface(
        modifier = modifier.padding(PdfBookSpacing.sm),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = ink,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        val containerModifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
        if (actionLabel == null && !withDismissAction) {
            Box(
                modifier = containerModifier.padding(horizontal = PdfBookSpacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = snackbarData.visuals.message,
                    modifier = Modifier.padding(vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Row(
                modifier = containerModifier.padding(start = PdfBookSpacing.md, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = snackbarData.visuals.message,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp)
                        .padding(end = if (actionLabel == null) 8.dp else 0.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (actionLabel != null) {
                    TextButton(
                        onClick = { snackbarData.performAction() },
                        colors = pdfBookTextButtonColors(contentColor = ink),
                    ) {
                        Text(actionLabel)
                    }
                }
                if (withDismissAction) {
                    IconButton(
                        onClick = { snackbarData.dismiss() },
                        colors = pdfBookIconButtonColors(contentColor = ink),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.snackbar_dismiss),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfBookSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        PdfBookSnackbar(snackbarData = data)
    }
}
