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
 * Canvas geometry of a two-page curl. All rects are canvas pixels.
 *
 * [sheet] is the drawn rect of the turning page, [spread] is the two facing pages, and
 * [backFace] is where the flap's back-face page is drawn before the turn transform (the
 * opposite page's drawn rect). [spineX] is the canvas x where the flap crosses the hinge.
 *
 * A single-page curl passes null and keeps the v0.7.0 geometry (the whole canvas is the sheet).
 */
data class CurlSheetLayout(
    val sheet: Rect,
    val spread: Rect,
    val backFace: Rect,
    val spineX: Float,
)

/**
 * Draws a mid-curl (or any progress) using cached page bitmaps.
 * [progress] is read in the draw phase, and every path, matrix and brush is
 * created once, so a turn does not recompose or allocate per frame.
 *
 * [colorFilter] is applied to every page bitmap (front, back and the flap's back face),
 * never to the paper-back color, fold or shadow.
 * [contentCrop] crops the front page bitmap by a source rect fraction (0..1); null
 * draws the whole bitmap. [backContentCrop] crops the revealed page the same way
 * and defaults to [contentCrop]; adjacent pages can have different margins, so a
 * turn passes the to-page's own crop here.
 *
 * [flapColor], [shadowColor], [shadowAlpha], [shadowMidAlpha] and
 * [foldHighlightColor] carry the per-mode values (normal mode by default; the
 * sepia and night variants are supplied by the display-mode work).
 *
 * [flapBack] is the back-face bitmap for the peeled flap: when non-null it is drawn at
 * full opacity, cropped by [flapContentCrop] and tinted by [colorFilter]; when null the
 * flap keeps the v0.7.0 look (paper-back color plus the front page mirrored at 8%).
 * [spreadFlap] lets the flap extend over the opposite page across the spine.
 *
 * [layout] switches to the two-page curl: the front page is drawn into [CurlSheetLayout.sheet],
 * the revealed page under it, and the flap's back face into [CurlSheetLayout.backFace]. The back
 * face transform is R∘Mspine (a rotation) instead of the fold mirror alone, so at progress 1 it
 * lands on the opposite page right-side up. Null keeps the single-sheet output byte-identical.
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
    backContentCrop: Rect? = contentCrop,
    flapBack: Bitmap? = null,
    flapContentCrop: Rect? = backContentCrop,
    spreadFlap: Boolean = false,
    layout: CurlSheetLayout? = null,
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
    val backCrop = remember(back, backContentCrop) {
        sourceCropPixels(back.width, back.height, backContentCrop)
    }
    val flapBackCrop = remember(flapBack, flapContentCrop) {
        flapBack?.let { sourceCropPixels(it.width, it.height, flapContentCrop) }
    }
    val shape = remember { CurlShape() }
    val frontPath = remember { Path() }
    val revealedPath = remember { Path() }
    val flapPath = remember { Path() }
    val mirror = remember { Matrix() }
    val mirrorValues = remember { FloatArray(9) }
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
        val sheet = layout?.sheet ?: Rect(0f, 0f, size.width, size.height)
        if (sheet.width <= 0f || sheet.height <= 0f) return@Canvas
        // ContentScale.Fit in primitives: the draw phase must not allocate.
        val frontFit = fitContent(crop, sheet)
        val left = frontFit.left
        val top = frontFit.top
        val w = frontFit.width
        val h = frontFit.height

        drawImageInto(backImage, backCrop, sheet, colorFilter)
        if (p <= 0.001f) {
            drawImageInto(frontImage, crop, sheet, colorFilter)
            return@Canvas
        }

        updateCurlShape(w, h, p, side, shape, spreadFlap || layout != null)
        if (!shape.foldValid) {
            drawImageInto(frontImage, crop, sheet, colorFilter)
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
                drawImageInto(frontImage, crop, sheet, colorFilter)
            }
        }

        // Flap: back face, inner shade, fold highlight.
        if (shape.flapCount >= 3) {
            flapPath.setPolygon(shape.flap, shape.flapCount, left, top)
            drawPath(flapPath, flapColor)
            clipPath(flapPath) {
                val backFace = flapBack
                val backFaceCrop = flapBackCrop
                if (backFace != null && backFaceCrop != null) {
                    drawMirroredPage(
                        bitmap = backFace,
                        crop = backFaceCrop,
                        dest = layout?.backFace ?: frontFit,
                        foldLeft = left,
                        foldTop = top,
                        shape = shape,
                        mirror = mirror,
                        mirrorValues = mirrorValues,
                        paint = mirrorPaint,
                        androidFilter = androidFilter,
                        srcRect = srcRect,
                        dstRect = dstRect,
                        alpha = 255,
                        // Two-page: R∘Mspine so the flap ends as a rotation, not a mirror.
                        spineX = layout?.spineX,
                    )
                } else {
                    drawMirroredPage(
                        bitmap = front,
                        crop = crop,
                        dest = frontFit,
                        foldLeft = left,
                        foldTop = top,
                        shape = shape,
                        mirror = mirror,
                        mirrorValues = mirrorValues,
                        paint = mirrorPaint,
                        androidFilter = androidFilter,
                        srcRect = srcRect,
                        dstRect = dstRect,
                        alpha = (CURL_MIRROR_ALPHA * 255f).roundToInt(),
                        spineX = null,
                    )
                }
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

/**
 * Draws [bitmap] fit inside [dest], then folds it into the turning flap.
 *
 * With [spineX] null the transform is the fold reflection alone (the v0.7.0 mirror).
 * With [spineX] set it is R∘Mspine (fold reflection composed with the spine reflection):
 * a rotation, so the two-page back face ends right-side up on the opposite page.
 */
private fun DrawScope.drawMirroredPage(
    bitmap: Bitmap,
    crop: PixelCrop,
    dest: Rect,
    foldLeft: Float,
    foldTop: Float,
    shape: CurlShape,
    mirror: Matrix,
    mirrorValues: FloatArray,
    paint: Paint,
    androidFilter: android.graphics.ColorFilter?,
    srcRect: android.graphics.Rect,
    dstRect: RectF,
    alpha: Int,
    spineX: Float?,
) {
    if (crop.width <= 0 || crop.height <= 0 || dest.width <= 0f || dest.height <= 0f) return
    val nx = shape.normalX
    val ny = shape.normalY
    val a = 1f - 2f * nx * nx
    val b = -2f * nx * ny
    val d = 1f - 2f * ny * ny
    val midX = foldLeft + shape.midX
    val midY = foldTop + shape.midY
    val tx = midX - (a * midX + b * midY)
    val ty = midY - (b * midX + d * midY)
    // Filled in place: the draw phase must not allocate.
    if (spineX == null) {
        mirrorValues[0] = a
        mirrorValues[1] = b
        mirrorValues[2] = tx
        mirrorValues[3] = b
        mirrorValues[4] = d
        mirrorValues[5] = ty
    } else {
        // R∘Mspine: Mspine(x) = 2*spineX - x, then R. Column/translation form below.
        mirrorValues[0] = -a
        mirrorValues[1] = b
        mirrorValues[2] = 2f * a * spineX + tx
        mirrorValues[3] = -b
        mirrorValues[4] = d
        mirrorValues[5] = 2f * b * spineX + ty
    }
    mirrorValues[6] = 0f
    mirrorValues[7] = 0f
    mirrorValues[8] = 1f
    mirror.setValues(mirrorValues)
    paint.alpha = alpha
    paint.colorFilter = androidFilter
    srcRect.set(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height)
    dstRect.set(dest.left, dest.top, dest.right, dest.bottom)
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val save = native.save()
        native.concat(mirror)
        native.drawBitmap(bitmap, srcRect, dstRect, paint)
        native.restoreToCount(save)
    }
}

/** Draws the cropped source of [image] fit and centered inside [dest]. */
private fun DrawScope.drawImageInto(
    image: ImageBitmap,
    crop: PixelCrop,
    dest: Rect,
    colorFilter: ColorFilter?,
) {
    if (crop.width <= 0 || crop.height <= 0 || dest.width <= 0f || dest.height <= 0f) return
    val fit = fitContent(crop, dest)
    val scale = fit.width / crop.width
    withTransform({
        translate(fit.left, fit.top)
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

/** Largest centered ContentScale.Fit rect for [crop] inside [dest]. */
private fun fitContent(crop: PixelCrop, dest: Rect): Rect {
    val scale = min(dest.width / crop.width, dest.height / crop.height)
    val width = crop.width * scale
    val height = crop.height * scale
    val left = dest.left + (dest.width - width) / 2f
    val top = dest.top + (dest.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}
