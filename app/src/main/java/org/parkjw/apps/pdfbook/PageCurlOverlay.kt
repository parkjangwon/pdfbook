package org.parkjw.apps.pdfbook

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
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
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawImageFit(backImage, back.width, back.height, w, h)
        val p = clampCurlProgress(progress)
        if (p <= 0.001f) {
            drawImageFit(frontImage, front.width, front.height, w, h)
            return@Canvas
        }
        if (p >= 0.999f) return@Canvas
        val geo = curlGeometry(w, h, p, side)
        if (geo == null) {
            drawImageFit(frontImage, front.width, front.height, w, h)
            return@Canvas
        }
        foldPath.reset()
        buildRemainingPagePath(foldPath, w, h, geo, side)
        clipPath(foldPath, ClipOp.Intersect) {
            drawImageFit(frontImage, front.width, front.height, w, h)
        }
        curlPath.reset()
        curlPath.moveTo(geo.foldTop.x, geo.foldTop.y)
        curlPath.lineTo(geo.tip.x, geo.tip.y)
        curlPath.lineTo(geo.foldBottom.x, geo.foldBottom.y)
        curlPath.close()
        drawPath(curlPath, color = ReaderTrack.copy(alpha = 0.92f))
        shadowPath.reset()
        val shadowPad = 18f * (1f - p)
        val foldDx = geo.foldBottom.x - geo.foldTop.x
        val foldDy = geo.foldBottom.y - geo.foldTop.y
        val fl = kotlin.math.hypot(foldDx, foldDy).coerceAtLeast(1f)
        val sx = -foldDy / fl * shadowPad
        val sy = foldDx / fl * shadowPad
        val sign = if (side == CurlSide.Right) 1f else -1f
        shadowPath.moveTo(geo.foldTop.x, geo.foldTop.y)
        shadowPath.lineTo(geo.foldBottom.x, geo.foldBottom.y)
        shadowPath.lineTo(geo.foldBottom.x + sx * sign, geo.foldBottom.y + sy * sign)
        shadowPath.lineTo(geo.foldTop.x + sx * sign, geo.foldTop.y + sy * sign)
        shadowPath.close()
        drawPath(shadowPath, color = ReaderBackground.copy(alpha = 0.35f))
    }
}

private fun buildRemainingPagePath(
    path: Path,
    width: Float,
    height: Float,
    geo: CurlGeometry,
    side: CurlSide,
) {
    // Polygon covering the still-flat part of the front page.
    when (side) {
        CurlSide.Right -> {
            path.moveTo(0f, 0f)
            path.lineTo(geo.foldTop.x, geo.foldTop.y)
            path.lineTo(geo.foldBottom.x, geo.foldBottom.y)
            if (geo.foldBottom.y < height) path.lineTo(geo.foldBottom.x, height)
            path.lineTo(0f, height)
            path.close()
        }
        CurlSide.Left -> {
            path.moveTo(width, 0f)
            path.lineTo(geo.foldTop.x, geo.foldTop.y)
            path.lineTo(geo.foldBottom.x, geo.foldBottom.y)
            if (geo.foldBottom.y < height) path.lineTo(geo.foldBottom.x, height)
            path.lineTo(width, height)
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
    if (srcW <= 0 || srcH <= 0 || dstW <= 0f || dstH <= 0f) return
    val scale = minOf(dstW / srcW, dstH / srcH)
    val dw = srcW * scale
    val dh = srcH * scale
    val left = (dstW - dw) / 2f
    val top = (dstH - dh) / 2f
    withTransform({
        translate(left, top)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawImage(image)
    }
}
