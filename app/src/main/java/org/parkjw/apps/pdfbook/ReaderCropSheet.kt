package org.parkjw.apps.pdfbook

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderChip
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderOverlay
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack
import org.parkjw.apps.pdfbook.ui.theme.pdfBookTextButtonColors
import kotlin.math.min

const val READER_CROP_SHEET_TAG = "reader_crop_sheet"
const val READER_CROP_SWITCH_TAG = "reader_crop_switch"
const val READER_CROP_PREVIEW_TAG = "reader_crop_preview"
const val READER_CROP_PREVIEW_IMAGE_TAG = "reader_crop_preview_image"
const val READER_CROP_DONE_TAG = "reader_crop_done"
const val READER_CROP_RESET_TAG = "reader_crop_reset"

private val PREVIEW_HEIGHT = 220.dp
private val PREVIEW_HEIGHT_SPREAD = 280.dp

@Composable
fun ReaderCropSheet(
    renderer: PageBitmaps,
    pages: List<Int>,
    twoPage: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview by produceState<CropPreview?>(initialValue = null, renderer, pages) {
        value = loadCropPreview(renderer, pages)
    }
    val displayMode = LocalDisplayMode.current
    val colorFilter = rememberReaderColorFilter(displayMode)
    val previewHeight = if (twoPage) PREVIEW_HEIGHT_SPREAD else PREVIEW_HEIGHT
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(READER_CROP_SHEET_TAG)
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
        val stateText = stringResource(
            if (enabled) R.string.reader_crop_state_on else R.string.reader_crop_state_off,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                .semantics { stateDescription = stateText }
                .testTag(READER_CROP_SWITCH_TAG),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reader_auto_crop_label),
                    color = ReaderOnOverlay,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(
                        if (twoPage) {
                            R.string.reader_crop_subtitle_spread
                        } else {
                            R.string.reader_crop_subtitle_single
                        },
                    ),
                    color = ReaderOnOverlayVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ReaderOverlay,
                    checkedTrackColor = ReaderOnOverlay,
                    checkedBorderColor = ReaderOnOverlay,
                    uncheckedThumbColor = ReaderOnOverlayVariant,
                    uncheckedTrackColor = ReaderTrack,
                    uncheckedBorderColor = ReaderTrack,
                ),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(previewHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(ReaderBackground)
                .padding(vertical = 12.dp)
                .testTag(READER_CROP_PREVIEW_TAG),
        ) {
            val data = preview
            if (data != null) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(previewHeight - 24.dp)
                        .testTag(READER_CROP_PREVIEW_IMAGE_TAG),
                ) {
                    drawCropPreview(data, enabled, colorFilter)
                }
            }
        }
        if (preview?.hasMargins == false) {
            Text(
                text = stringResource(R.string.reader_crop_empty),
                color = ReaderOnOverlayVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_file_check),
                contentDescription = null,
                tint = ReaderOnOverlayVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.reader_crop_notice),
                color = ReaderOnOverlayVariant,
                fontSize = 12.sp,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (enabled) {
                TextButton(
                    onClick = { onEnabledChange(false) },
                    colors = pdfBookTextButtonColors(contentColor = ReaderOnOverlay),
                    modifier = Modifier.height(40.dp).testTag(READER_CROP_RESET_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.reader_crop_reset),
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReaderOnOverlay,
                    contentColor = ReaderOverlay,
                ),
                modifier = Modifier.height(40.dp).testTag(READER_CROP_DONE_TAG),
            ) {
                Text(
                    text = stringResource(R.string.reader_crop_done),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

private data class PreviewPage(
    val image: ImageBitmap,
    val width: Int,
    val height: Int,
    val crop: CropMargins,
)

private data class CropPreview(val pages: List<PreviewPage>) {
    val hasMargins: Boolean get() = pages.any { !it.crop.isNone }
}

private suspend fun loadCropPreview(renderer: PageBitmaps, pages: List<Int>): CropPreview? {
    if (pages.isEmpty()) return null
    val bitmaps = pages.map { page -> renderer.render(page, 480, 640) ?: return null }
    val detected = withContext(Dispatchers.Default) { bitmaps.map { detectCropMargins(it) } }
    val crops = if (bitmaps.size >= 2) {
        val aligned = alignedTwoPageMargins(detected[0], detected[1])
        listOf(aligned.first, aligned.second)
    } else {
        detected
    }
    return CropPreview(
        bitmaps.mapIndexed { index, bitmap ->
            PreviewPage(
                image = bitmap.asImageBitmap(),
                width = bitmap.width,
                height = bitmap.height,
                crop = crops.getOrElse(index) { CropMargins() },
            )
        },
    )
}

private fun DrawScope.drawCropPreview(
    data: CropPreview,
    enabled: Boolean,
    colorFilter: ColorFilter?,
) {
    val pages = data.pages
    if (pages.isEmpty()) return
    val totalSourceWidth = pages.sumOf { it.width.toDouble() }.toFloat()
    val maxSourceHeight = pages.maxOf { it.height }.toFloat()
    val scale = min(size.width / totalSourceWidth, size.height / maxSourceHeight)
    if (scale <= 0f || !scale.isFinite()) return
    val contentWidth = totalSourceWidth * scale
    val contentTop = (size.height - maxSourceHeight * scale) / 2f
    var x = (size.width - contentWidth) / 2f
    val cropRects = ArrayList<Rect>(pages.size)
    pages.forEach { page ->
        val width = page.width * scale
        val height = page.height * scale
        val top = contentTop + (maxSourceHeight * scale - height) / 2f
        drawImage(
            image = page.image,
            dstOffset = androidx.compose.ui.unit.IntOffset(x.toInt(), top.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1)),
            colorFilter = colorFilter,
        )
        cropRects += Rect(
            left = x + page.crop.left * width,
            top = top + page.crop.top * height,
            right = x + width - page.crop.right * width,
            bottom = top + height - page.crop.bottom * height,
        )
        x += width
    }
    val union = if (cropRects.size >= 2) {
        Rect(
            left = cropRects.first().left,
            top = maxOf(cropRects.first().top, cropRects.last().top),
            right = cropRects.last().right,
            bottom = minOf(cropRects.first().bottom, cropRects.last().bottom),
        )
    } else {
        cropRects.first()
    }
    val shade = ReaderBackground.copy(alpha = if (enabled) 0.60f else 0.30f)
    drawRect(shade, topLeft = Offset.Zero, size = Size(size.width, union.top.coerceAtLeast(0f)))
    drawRect(
        shade,
        topLeft = Offset(0f, union.bottom.coerceAtMost(size.height)),
        size = Size(size.width, (size.height - union.bottom).coerceAtLeast(0f)),
    )
    drawRect(
        shade,
        topLeft = Offset(0f, union.top.coerceAtLeast(0f)),
        size = Size(union.left.coerceAtLeast(0f), union.height.coerceAtLeast(0f)),
    )
    drawRect(
        shade,
        topLeft = Offset(union.right.coerceAtMost(size.width), union.top.coerceAtLeast(0f)),
        size = Size((size.width - union.right).coerceAtLeast(0f), union.height.coerceAtLeast(0f)),
    )
    val borderWidth = if (enabled) 1.5.dp.toPx() else 1.dp.toPx()
    val borderColor = if (enabled) ReaderOnOverlay else ReaderOnOverlayVariant
    val dash = if (enabled) null else PathEffect.dashPathEffect(
        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
    )
    drawRect(
        color = borderColor,
        topLeft = Offset(union.left, union.top),
        size = Size(union.width, union.height),
        style = Stroke(width = borderWidth, pathEffect = dash),
    )
    if (cropRects.size >= 2) {
        val spine = cropRects.last().left
        drawLine(
            color = ReaderOnOverlay.copy(alpha = 0.35f),
            start = Offset(spine, union.top),
            end = Offset(spine, union.bottom),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
    }
}
