package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun PdfBookProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun PdfBookReaderProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = ReaderAccent,
        trackColor = ReaderTrack,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfBookReaderSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: SliderColors,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors = colors,
        modifier = modifier,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
            )
        },
    )
}
