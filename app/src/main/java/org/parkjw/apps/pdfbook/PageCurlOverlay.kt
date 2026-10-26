package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageBack
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageFoldHighlight
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShade
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShadow

/**
 * Draws a mid-curl (or any progress) using cached current/next page bitmaps.
 * [progress] is read in the draw phase, and every path, matrix and brush is
 * created once, so a turn does not recompose or allocate per frame.
 *
 * [colorFilter] is applied to every page bitmap (front, back and the mirrored
 * front on the flap), never to the paper-back color, fold or shadow.
 * [contentCrop] crops the page bitmaps by a source rect fraction (0..1); null
 * draws the whole bitmap.
 *
 * [flapColor], [shadowColor], [shadowAlpha], [shadowMidAlpha] and
 * [foldHighlightColor] carry the per-mode values (normal mode by default; the
 * sepia and night variants are supplied by the display-mode work).
 */
@Composable
fun PageCurlOverlay(
    front: Bitmap,
    back: Bitmap,
    progress: () -> Float,
    side: CurlSide,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    contentCrop: Rect? = null,
    flapColor: Color = ReaderPageBack,
    shadowColor: Color = ReaderPageShadow,
    shadowAlpha: Float = CURL_SHADOW_ALPHA,
    shadowMidAlpha: Float = CURL_SHADOW_MID_ALPHA,
    foldHighlightColor: Color = ReaderPageFoldHighlight.copy(alpha = CURL_FOLD_HIGHLIGHT_ALPHA),
) {
    val frontImage = remember(front) { front.asImageBitmap() }
    val backImage = remember(back) { back.asImageBitmap() }
    val androidFilter = remember(colorFilter) { colorFilter?.asAndroidColorFilter() }
    val frontCrop = remember(front, contentCrop) {
        sourceCropPixels(front.width, front.height, contentCrop)
    }
    val backCrop = remember(back, contentCrop) {
        sourceCropPixels(back.width, back.height, contentCrop)
    }
    val shape = remember { CurlShape() }
    val frontPath = remember { Path() }
    val revealedPath = remember { Path() }
    val flapPath = remember { Path() }
    val mirror = remember { Matrix() }
    val mirrorPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    }
    val srcRect = remember { android.graphics.Rect() }
    val dstRect = remember { RectF() }
    val shadowBrush = remember(shadowColor, shadowAlpha, shadowMidAlpha) {
        Brush.linearGradient(
            0f to shadowColor.copy(alpha = shadowAlpha),
            0.35f to shadowColor.copy(alpha = shadowMidAlpha),
            1f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(1f, 0f),
        )
    }
    val shadeBrush = remember {
        Brush.linearGradient(
            0f to ReaderPageShade.copy(alpha = CURL_SHADE_ALPHA),
            1f to Color.Transparent,
            start = Offset.Zero,
            end = Offset(1f, 0f),
        )
    }
    Canvas(modifier.fillMaxSize()) {
        val crop = frontCrop
        if (crop.width <= 0 || crop.height <= 0) return@Canvas
        val p = clampCurlProgress(progress())
        // ContentScale.Fit in primitives: the draw phase must not allocate.
        val scale = min(size.width / crop.width, size.height / crop.height)
        val w = crop.width * scale
        val h = crop.height * scale
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f

        drawImageFit(backImage, backCrop, colorFilter)
        if (p <= 0.001f) {
            drawImageFit(frontImage, crop, colorFilter)
            return@Canvas
        }

        updateCurlShape(w, h, p, side, shape)
        if (!shape.foldValid) {
            drawImageFit(frontImage, crop, colorFilter)
            return@Canvas
        }

        val diag = hypot(w, h)
        val foldTop = Offset(left + shape.foldTopX, top + shape.foldTopY)
        val foldBottom = Offset(left + shape.foldBottomX, top + shape.foldBottomY)
        val mid = Offset(left + shape.midX, top + shape.midY)

        // Shadow: one linear gradient from the fold onto the revealed page only.
        val shadowWidth = CURL_SHADOW_WIDTH_DP.dp.toPx() * curlShadowWidthFraction(p)
        if (shadowWidth > 0.5f && shape.revealedCount >= 3) {
            revealedPath.setPolygon(shape.revealed, shape.revealedCount, left, top)
            clipPath(revealedPath) {
                drawFoldGradient(
                    brush = shadowBrush,
                    origin = mid,
                    angle = atan2(-shape.normalY, -shape.normalX),
                    lengthPx = shadowWidth,
                    diagonal = diag,
                )
            }
        }

        // Front page: everything the fold has not peeled yet.
        if (shape.frontCount >= 3) {
            frontPath.setPolygon(shape.front, shape.frontCount, left, top)
            clipPath(frontPath) {
                drawImageFit(frontImage, crop, colorFilter)
            }
        }

        // Flap: paper back, mirrored front at 8%, inner shade, fold highlight.
        if (shape.flapCount >= 3) {
            flapPath.setPolygon(shape.flap, shape.flapCount, left, top)
            drawPath(flapPath, flapColor)
            clipPath(flapPath) {
                drawMirroredFront(
                    bitmap = front,
                    crop = crop,
                    left = left,
                    top = top,
                    width = w,
                    height = h,
                    shape = shape,
                    mirror = mirror,
                    paint = mirrorPaint,
                    androidFilter = androidFilter,
                    srcRect = srcRect,
                    dstRect = dstRect,
                )
                drawFoldGradient(
                    brush = shadeBrush,
                    origin = mid,
                    angle = atan2(shape.normalY, shape.normalX),
                    lengthPx = CURL_SHADE_WIDTH_DP.dp.toPx(),
                    diagonal = diag,
                )
            }
            drawLine(
                color = foldHighlightColor,
                start = foldTop,
                end = foldBottom,
                strokeWidth = CURL_FOLD_HIGHLIGHT_DP.dp.toPx(),
            )
        }
    }
}

private fun Path.setPolygon(points: FloatArray, count: Int, left: Float, top: Float) {
    reset()
    if (count < 3) return
    moveTo(points[0] + left, points[1] + top)
    for (i in 1 until count) {
        lineTo(points[i * 2] + left, points[i * 2 + 1] + top)
    }
    close()
}

private fun DrawScope.drawFoldGradient(
    brush: Brush,
    origin: Offset,
    angle: Float,
    lengthPx: Float,
    diagonal: Float,
) {
    if (lengthPx <= 0f) return
    withTransform({
        translate(origin.x, origin.y)
        // Pivot must be the local origin: the default pivot is the canvas center.
        rotate(degrees = Math.toDegrees(angle.toDouble()).toFloat(), pivot = Offset.Zero)
        scale(lengthPx, 1f, pivot = Offset.Zero)
    }) {
        drawRect(
            brush = brush,
            topLeft = Offset(0f, -diagonal),
            size = Size(1f, diagonal * 2f),
        )
    }
}

private fun DrawScope.drawMirroredFront(
    bitmap: Bitmap,
    crop: PixelCrop,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    shape: CurlShape,
    mirror: Matrix,
    paint: Paint,
    androidFilter: android.graphics.ColorFilter?,
    srcRect: android.graphics.Rect,
    dstRect: RectF,
) {
    val nx = shape.normalX
    val ny = shape.normalY
    val a = 1f - 2f * nx * nx
    val b = -2f * nx * ny
    val d = 1f - 2f * ny * ny
    val midX = left + shape.midX
    val midY = top + shape.midY
    mirror.setValues(
        floatArrayOf(
            a,
            b,
            midX - (a * midX + b * midY),
            b,
            d,
            midY - (b * midX + d * midY),
            0f,
            0f,
            1f,
        ),
    )
    paint.alpha = (CURL_MIRROR_ALPHA * 255f).roundToInt()
    paint.colorFilter = androidFilter
    srcRect.set(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height)
    dstRect.set(left, top, left + width, top + height)
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val save = native.save()
        native.concat(mirror)
        native.drawBitmap(bitmap, srcRect, dstRect, paint)
        native.restoreToCount(save)
    }
}

private fun DrawScope.drawImageFit(
    image: ImageBitmap,
    crop: PixelCrop,
    colorFilter: ColorFilter?,
) {
    if (crop.width <= 0 || crop.height <= 0) return
    val scale = min(size.width / crop.width, size.height / crop.height)
    val left = (size.width - crop.width * scale) / 2f
    val top = (size.height - crop.height * scale) / 2f
    withTransform({
        translate(left, top)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawImage(
            image = image,
            srcOffset = IntOffset(crop.left, crop.top),
            srcSize = IntSize(crop.width, crop.height),
            colorFilter = colorFilter,
        )
    }
}
