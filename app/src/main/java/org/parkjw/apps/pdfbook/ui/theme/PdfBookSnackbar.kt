package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PdfBookSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(PdfBookSpacing.sm),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = PdfBookSpacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = snackbarData.visuals.message,
                modifier = Modifier.padding(vertical = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
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
