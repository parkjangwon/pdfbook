package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.io.Closeable
import kotlin.math.roundToInt
import org.parkjw.apps.pdfbook.settings.DisplayMode
import org.parkjw.apps.pdfbook.settings.SettingsGroup
import org.parkjw.apps.pdfbook.settings.SettingsSection
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookReaderSlider
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors

const val IMAGE_ADJUST_SCREEN_TAG = "image_adjust_screen"
const val IMAGE_ADJUST_PREVIEW_TAG = "image_adjust_preview"
const val IMAGE_ADJUST_BRIGHTNESS_TAG = "image_adjust_brightness"
const val IMAGE_ADJUST_CONTRAST_TAG = "image_adjust_contrast"
const val IMAGE_ADJUST_GAMMA_TAG = "image_adjust_gamma"
const val IMAGE_ADJUST_GRAYSCALE_TAG = "image_adjust_grayscale"
const val IMAGE_ADJUST_RESET_TAG = "image_adjust_reset"

private val PREVIEW_HEIGHT = 248.dp
private const val PREVIEW_MAX_PX = 1200

/**
 * Preview and sliders for one book. Changes save as they move; there is no save
 * button, matching the other reading settings.
 */
@Composable
internal fun ImageAdjustScreen(
    adjust: ImageAdjust,
    bookTitle: String,
    page: Int,
    renderer: PageBitmaps?,
    displayMode: DisplayMode,
    pageColorLabel: String,
    gammaSupported: Boolean,
    onAdjustChange: (ImageAdjust) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SafeTopBar {
                IconButton(
                    onClick = onBack,
                    colors = pdfBookIconButtonColors(),
                    modifier = Modifier.testTag("image_adjust_back"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.reader_back),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_image_adjust),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag(IMAGE_ADJUST_SCREEN_TAG)
                .padding(padding),
        ) {
            val unfolded = maxWidth >= 600.dp
            val preview = rememberAdjustPreview(renderer, page)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = PdfBookSpacing.lg),
            ) {
                val previewBlock: @Composable () -> Unit = {
                    AdjustPreview(
                        bitmap = preview,
                        title = bookTitle,
                        page = page,
                        adjust = adjust,
                        displayMode = displayMode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val controlsBlock: @Composable () -> Unit = {
                    Column(Modifier.fillMaxWidth()) {
                        NoticeNote()
                        SettingsSection(stringResource(R.string.image_adjust_section))
                        SettingsGroup {
                            AdjustSliderRow(
                                title = stringResource(R.string.image_adjust_brightness),
                                valueText = signedPercent(adjust.brightness),
                                value = adjust.brightness.toFloat(),
                                valueRange = -ImageAdjust.BRIGHTNESS_RANGE.toFloat()..ImageAdjust.BRIGHTNESS_RANGE.toFloat(),
                                testTag = IMAGE_ADJUST_BRIGHTNESS_TAG,
                                enabled = true,
                                supporting = null,
                                onValueChange = { onAdjustChange(adjust.copy(brightness = (it / 5f).roundToInt() * 5)) },
                            )
                            AdjustSliderRow(
                                title = stringResource(R.string.image_adjust_contrast),
                                valueText = signedPercent(adjust.contrast),
                                value = adjust.contrast.toFloat(),
                                valueRange = -ImageAdjust.CONTRAST_RANGE.toFloat()..ImageAdjust.CONTRAST_RANGE.toFloat(),
                                testTag = IMAGE_ADJUST_CONTRAST_TAG,
                                enabled = true,
                                supporting = null,
                                onValueChange = { onAdjustChange(adjust.copy(contrast = (it / 5f).roundToInt() * 5)) },
                            )
                            AdjustSliderRow(
                                title = stringResource(R.string.image_adjust_gamma),
                                valueText = stringResource(R.string.image_adjust_value_gamma, adjust.gamma),
                                value = adjust.gamma,
                                valueRange = ImageAdjust.GAMMA_MIN..ImageAdjust.GAMMA_MAX,
                                testTag = IMAGE_ADJUST_GAMMA_TAG,
                                enabled = gammaSupported,
                                supporting = if (gammaSupported) {
                                    null
                                } else {
                                    stringResource(R.string.image_adjust_gamma_unavailable)
                                },
                                onValueChange = { onAdjustChange(adjust.copy(gamma = (it * 20f).roundToInt() / 20f)) },
                            )
                            AdjustSliderRow(
                                title = stringResource(R.string.image_adjust_grayscale),
                                valueText = stringResource(R.string.image_adjust_value_percent, adjust.grayscale),
                                value = adjust.grayscale.toFloat(),
                                valueRange = 0f..ImageAdjust.GRAYSCALE_RANGE.toFloat(),
                                testTag = IMAGE_ADJUST_GRAYSCALE_TAG,
                                enabled = true,
                                supporting = null,
                                onValueChange = { onAdjustChange(adjust.copy(grayscale = (it / 10f).roundToInt() * 10)) },
                            )
                        }
                        OrderLine(pageColorLabel)
                        ResetRow(enabled = !adjust.isDefault, onReset = { onAdjustChange(ImageAdjust.Default) })
                    }
                }
                if (unfolded) {
                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(
                                    start = PdfBookSpacing.screenHorizontalUnfolded,
                                    end = PdfBookSpacing.sm,
                                ),
                        ) {
                            previewBlock()
                        }
                        Box(Modifier.weight(1f)) { controlsBlock() }
                    }
                } else {
                    Column {
                        Box(Modifier.padding(horizontal = PdfBookSpacing.screenHorizontalFolded)) {
                            previewBlock()
                        }
                        controlsBlock()
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberAdjustPreview(renderer: PageBitmaps?, page: Int): Bitmap? {
    val preview by produceState<Bitmap?>(null, renderer, page) {
        value = renderer?.render(page, PREVIEW_MAX_PX, PREVIEW_MAX_PX)
    }
    return preview
}

/** Opens the last read book once for the preview and closes it with the screen. */
@Composable
internal fun rememberBookRenderer(book: BookInfo?): PageBitmaps? {
    val context = LocalContext.current
    val uri = remember(book) { book?.let { Uri.parse(it.uri) } }
    val state = produceState<PageBitmaps?>(null, uri) {
        val opened = uri?.let { openReaderRenderer(context, it) }
        value = opened
        awaitDispose { (opened as? Closeable)?.close() }
    }
    return state.value
}

@Composable
private fun AdjustPreview(
    bitmap: Bitmap?,
    title: String,
    page: Int,
    adjust: ImageAdjust,
    displayMode: DisplayMode,
    modifier: Modifier = Modifier,
) {
    var showingOriginal by remember { mutableStateOf(false) }
    val shown = if (showingOriginal) ImageAdjust.Default else adjust
    val filter: ColorFilter? = remember(displayMode, shown) { readerColorFilter(displayMode, shown) }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val description = stringResource(R.string.image_adjust_preview_description, page)
    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT)
                .testTag(IMAGE_ADJUST_PREVIEW_TAG)
                .clip(CardCorner)
                .background(ReaderBackground)
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            showingOriginal = true
                            tryAwaitRelease()
                            showingOriginal = false
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = filter,
                    modifier = Modifier.fillMaxSize().padding(PdfBookSpacing.sm),
                )
            }
        }
        Text(
            text = stringResource(R.string.image_adjust_preview_hint, title, page),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = PdfBookSpacing.xs),
        )
    }
}

@Composable
private fun NoticeNote() {
    Row(
        modifier = Modifier.padding(
            start = PdfBookSpacing.screenHorizontalFolded,
            end = PdfBookSpacing.screenHorizontalFolded,
            top = PdfBookSpacing.md,
            bottom = PdfBookSpacing.xs,
        ),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.image_adjust_saved_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = PdfBookSpacing.xs),
        )
    }
}

@Composable
private fun AdjustSliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    testTag: String,
    enabled: Boolean,
    supporting: String?,
    onValueChange: (Float) -> Unit,
) {
    val ink = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = PdfBookSpacing.md, vertical = PdfBookSpacing.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) ink.onSurface else MaterialTheme.extendedColors.muted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PdfBookNumeric.fontFamily),
                color = if (enabled) ink.onSurfaceVariant else MaterialTheme.extendedColors.muted,
            )
        }
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extendedColors.muted,
            )
        }
        PdfBookReaderSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ink.primary,
                activeTrackColor = ink.primary,
                inactiveTrackColor = ink.surfaceVariant,
            ),
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(testTag)
                .semantics {
                    contentDescription = title
                    stateDescription = valueText
                },
        )
    }
}

@Composable
private fun OrderLine(pageColorLabel: String) {
    Text(
        text = stringResource(R.string.image_adjust_order, pageColorLabel),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = PdfBookSpacing.md,
            end = PdfBookSpacing.md,
            top = PdfBookSpacing.sm,
        ),
    )
}

@Composable
private fun ResetRow(enabled: Boolean, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PdfBookSpacing.md),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onReset,
            enabled = enabled,
            colors = pdfBookTextButtonColors(),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(IMAGE_ADJUST_RESET_TAG),
        ) {
            Text(stringResource(R.string.reader_crop_reset))
        }
    }
}

private fun signedPercent(value: Int): String = if (value > 0) "+$value%" else "$value%"
