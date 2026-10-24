package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderTrack

/**
 * Draws a mid-curl (or any progress) using cached current/next page bitmaps.
 * Paths are remembered and reused to avoid per-frame allocation.
 */
@Composable
fun PageCurlOverlay(
    front: Bitmap,
    back: Bitmap,
    progress: Float,
    side: CurlSide,
    modifier: Modifier = Modifier,
) {
    val frontImage = remember(front) { front.asImageBitmap() }
    val backImage = remember(back) { back.asImageBitmap() }
    val foldPath = remember { Path() }
    val curlPath = remember { Path() }
    val shadowPath = remember { Path() }
    val pageClip = remember { Path() }
    Canvas(modifier.fillMaxSize()) {
        val fit = fittedPageRect(front.width, front.height, size.width, size.height)
            ?: return@Canvas
        val left = fit.left
        val top = fit.top
        val w = fit.width
        val h = fit.height
        pageClip.reset()
        pageClip.addRect(Rect(left, top, left + w, top + h))
        clipPath(pageClip, ClipOp.Intersect) {
            drawImageFit(backImage, back.width, back.height, size.width, size.height)
            val p = clampCurlProgress(progress)
            if (p <= 0.001f) {
                drawImageFit(frontImage, front.width, front.height, size.width, size.height)
                return@clipPath
            }
            if (p >= 0.999f) return@clipPath
            val geo = curlGeometry(w, h, p, side)
            if (geo == null) {
                drawImageFit(frontImage, front.width, front.height, size.width, size.height)
                return@clipPath
            }
            val tip = Offset(left + geo.tip.x, top + geo.tip.y)
            val foldTop = Offset(left + geo.foldTop.x, top + geo.foldTop.y)
            val foldBottom = Offset(left + geo.foldBottom.x, top + geo.foldBottom.y)
            foldPath.reset()
            buildRemainingPagePath(foldPath, left, top, w, h, foldTop, foldBottom, side)
            clipPath(foldPath, ClipOp.Intersect) {
                drawImageFit(frontImage, front.width, front.height, size.width, size.height)
            }
            curlPath.reset()
            curlPath.moveTo(foldTop.x, foldTop.y)
            curlPath.lineTo(tip.x, tip.y)
            curlPath.lineTo(foldBottom.x, foldBottom.y)
            curlPath.close()
            drawPath(curlPath, color = ReaderTrack.copy(alpha = 0.92f))
            shadowPath.reset()
            val shadowPad = 18f * (1f - p)
            val foldDx = foldBottom.x - foldTop.x
            val foldDy = foldBottom.y - foldTop.y
            val fl = kotlin.math.hypot(foldDx, foldDy).coerceAtLeast(1f)
            val sx = -foldDy / fl * shadowPad
            val sy = foldDx / fl * shadowPad
            val sign = if (side == CurlSide.Right) 1f else -1f
            shadowPath.moveTo(foldTop.x, foldTop.y)
            shadowPath.lineTo(foldBottom.x, foldBottom.y)
            shadowPath.lineTo(foldBottom.x + sx * sign, foldBottom.y + sy * sign)
            shadowPath.lineTo(foldTop.x + sx * sign, foldTop.y + sy * sign)
            shadowPath.close()
            // Keep shadow inside the fitted page (clipPath already active).
            clipRect(left, top, left + w, top + h) {
                drawPath(shadowPath, color = ReaderBackground.copy(alpha = 0.35f))
            }
        }
    }
}

private fun buildRemainingPagePath(
    path: Path,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    foldTop: Offset,
    foldBottom: Offset,
    side: CurlSide,
) {
    val right = left + width
    val bottom = top + height
    when (side) {
        CurlSide.Right -> {
            path.moveTo(left, top)
            path.lineTo(foldTop.x, foldTop.y)
            path.lineTo(foldBottom.x, foldBottom.y)
            if (foldBottom.y < bottom) path.lineTo(foldBottom.x, bottom)
            path.lineTo(left, bottom)
            path.close()
        }
        CurlSide.Left -> {
            path.moveTo(right, top)
            path.lineTo(foldTop.x, foldTop.y)
            path.lineTo(foldBottom.x, foldBottom.y)
            if (foldBottom.y < bottom) path.lineTo(foldBottom.x, bottom)
            path.lineTo(right, bottom)
            path.close()
        }
    }
}

private fun DrawScope.drawImageFit(
    image: androidx.compose.ui.graphics.ImageBitmap,
    srcW: Int,
    srcH: Int,
    dstW: Float,
    dstH: Float,
) {
    val fit = fittedPageRect(srcW, srcH, dstW, dstH) ?: return
    val scale = fit.width / srcW
    withTransform({
        translate(fit.left, fit.top)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawImage(image)
    }
}
