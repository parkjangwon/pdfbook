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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import org.parkjw.apps.pdfbook.ui.theme.ReaderBackground
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageBack
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageFoldHighlight
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShade
import org.parkjw.apps.pdfbook.ui.theme.ReaderPageShadow

/**
 * Canvas geometry of a two-page curl. All rects are canvas pixels.
 *
 * [sheet] is the drawn rect of the turning page, [landing] the rect of the same-side page it
 * reveals, [backFace] the opposite page's drawn rect (where the flap's back face lands) and
 * [still] the current spread's opposite page, kept on screen while the pager is on the target
 * spread. [spread] is the two facing page rects. [spineX] is the canvas x where the flap crosses
 * the hinge. ReaderScreen measures each rect with the same fit as SpreadPageLayout, so pages of
 * a real book that differ by a few points are drawn at their own rects and both the first and
 * the last frame match the settled spreads; it falls back to Slide when the two slots do not
 * meet at the spine.
 *
 * [hinge] marks a Book posture with a real hinge gap: the rects are then in virtual-spread
 * coordinates (the halves butted together) and the overlay paints the frame clipped on each
 * side of the gap, so the flap never covers the hinge.
 *
 * A single-page curl passes null and keeps the v0.7.0 geometry (the whole canvas is the sheet).
 */
data class CurlSheetLayout(
    val sheet: Rect,
    val spread: Rect,
    val backFace: Rect,
    val spineX: Float,
    val landing: Rect = sheet,
    val still: Rect = backFace,
    /** Set for a Book posture with a hinge gap: the overlay cuts the frame at [spineX]. */
    val hinge: CurlHinge? = null,
)

/**
 * One page of the incoming spread, drawn under a single-sheet turn in canvas pixels.
 * [crop] is a source rect fraction (0..1) and [dest] is where the cropped page is fitted.
 */
data class CurlUnderlay(
    val bitmap: Bitmap,
    val crop: Rect?,
    val dest: Rect,
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
 * [flapBack] is an optional back-face bitmap for the peeled flap: when non-null it is
 * turned with the sheet (R ∘ Mspine) into its still page across the spine at full opacity,
 * cropped by [flapContentCrop] and tinted by [colorFilter], so the text on it is never
 * mirrored and the last frame equals the still spread; when null the flap keeps the v0.7.0
 * look (paper-back color plus the front page mirrored at 8%). [spreadFlap] lets the flap
 * extend over the opposite page across the spine; the default keeps the single-sheet output
 * unchanged.
 *
 * [layout] switches to the two-page curl: the front page is drawn into [CurlSheetLayout.sheet],
 * the revealed page under it into [CurlSheetLayout.landing], and the flap's back face into its
 * still page across the spine.
 * Null keeps the single-sheet output byte-identical.
 *
 * [frontRect] switches to a lone-page turn: the curl geometry, flap and shading run over
 * [frontRect] (the lone page's drawn rect), the page is drawn there, and [underlay] pages (the
 * target spread) are painted only where the sheet has peeled away. This is the cover / last-page
 * turn; [layout] stays null for it, and [frontRect] must contain every underlay dest (the
 * builder otherwise returns Slide).
 *
 * [still] is the current spread's other page, the one that must not move during the turn.
 * With [layout] set the pager has already moved to the target spread, so the overlay first
 * paints its canvas with [ReaderBackground], then draws [still] into [CurlSheetLayout.still]
 * and the revealed page into [CurlSheetLayout.landing]. Each page keeps its own measured rect,
 * so the first frame equals the current spread and the last frame the target spread even when
 * the book's pages differ in size by a few points. The flap's back face is drawn into
 * [CurlSheetLayout.backFace] with the reflection that is the identity at progress 1.
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
    still: Bitmap? = null,
    stillContentCrop: Rect? = null,
    spreadFlap: Boolean = false,
    layout: CurlSheetLayout? = null,
    frontRect: Rect? = null,
    underlay: List<CurlUnderlay> = emptyList(),
    flapColor: Color = ReaderPageBack,
    shadowColor: Color = ReaderPageShadow,
    shadowAlpha: Float = CURL_SHADOW_ALPHA,
    shadowMidAlpha: Float = CURL_SHADOW_MID_ALPHA,
    foldHighlightColor: Color = ReaderPageFoldHighlight.copy(alpha = CURL_FOLD_HIGHLIGHT_ALPHA),
) {
    val frontImage = remember(front) { front.asImageBitmap() }
    val backImage = remember(back) { back.asImageBitmap() }
    val stillImage = remember(still) { still?.asImageBitmap() }
    val androidFilter = remember(colorFilter) { colorFilter?.asAndroidColorFilter() }
    val frontCrop = remember(front, contentCrop) {
        sourceCropPixels(front.width, front.height, contentCrop)
    }
    val backCrop = remember(back, backContentCrop) {
        sourceCropPixels(back.width, back.height, backContentCrop)
    }
    val stillCrop = remember(still, stillContentCrop) {
        still?.let { sourceCropPixels(it.width, it.height, stillContentCrop) }
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
    val backFaceValues = remember { FloatArray(9) }
    val mirrorPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    }
    val srcRect = remember { android.graphics.Rect() }
    val dstRect = remember { RectF() }
    // Draw-phase geometry lives here so a curl frame allocates nothing.
    val sheet = remember { RectF() }
    val fit = remember { RectF() }
    val drawFit = remember { RectF() }
    val stillDest = remember(layout) {
        layout?.let { RectF(it.backFace.left, it.backFace.top, it.backFace.right, it.backFace.bottom) }
    }
    val stillPageDest = remember(layout) {
        layout?.let { RectF(it.still.left, it.still.top, it.still.right, it.still.bottom) }
    }
    val revealDest = remember(layout) {
        layout?.let { RectF(it.landing.left, it.landing.top, it.landing.right, it.landing.bottom) }
    }
    val singleDest = remember(frontRect) {
        frontRect?.let { RectF(it.left, it.top, it.right, it.bottom) }
    }
    val underlayDraws = remember(underlay) {
        Array(underlay.size) { index ->
            val page = underlay[index]
            UnderlayDraw(
                image = page.bitmap.asImageBitmap(),
                crop = sourceCropPixels(page.bitmap.width, page.bitmap.height, page.crop),
                dest = RectF(page.dest.left, page.dest.top, page.dest.right, page.dest.bottom),
            )
        }
    }
    val scratch = remember { RectF() }
    // A two-page turn whose four pages differ in size by a few pixels runs the fold over their
    // union and clears the flap to the background; equal rects keep the v0.7.1 fast path.
    val uniformRects = remember(layout) { layout == null || layout.rectsMatchSheet() }
    val foldUnion = remember(layout) { layout?.let { curlFoldUnion(it) } }
    val foldBounds = remember { FloatArray(4) }
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
        val twoPage = layout
        val singlePage = singleDest
        val sheetRect = sheet
        if (twoPage != null) {
            sheetRect.set(
                twoPage.sheet.left,
                twoPage.sheet.top,
                twoPage.sheet.right,
                twoPage.sheet.bottom,
            )
        } else if (singlePage != null) {
            // The lone page is its own sheet: the fold, flap and shading stay inside the page,
            // so a cover or last page never curls over the black margin beside it.
            sheetRect.set(singlePage.left, singlePage.top, singlePage.right, singlePage.bottom)
        } else {
            sheetRect.set(0f, 0f, size.width, size.height)
        }
        if (sheetRect.width() <= 0f || sheetRect.height() <= 0f) return@Canvas
        // ContentScale.Fit in primitives: the draw phase must not allocate. For a lone page
        // sheetRect is already the page's drawn rect, so this is the identity.
        fitInto(crop, sheetRect, fit)
        drawFit.set(fit)
        val left = fit.left
        val top = fit.top
        val w = fit.width()
        val h = fit.height()
        // The draw dest of the turning page; a lone page is drawn into its own rect.
        val frontDest = sheetRect

        // One shape and one set of polygons per frame. A Book hinge gap draws the frame in two
        // clipped passes, so building them inside drawPass() would do the same work twice.
        val bounds = if (foldUnion != null && !uniformRects) {
            foldBounds[0] = foldUnion.left - left
            foldBounds[1] = foldUnion.top - top
            foldBounds[2] = foldUnion.right - left
            foldBounds[3] = foldUnion.bottom - top
            foldBounds
        } else {
            null
        }
        updateCurlShape(w, h, p, side, shape, spreadFlap || layout != null, bounds)
        val foldValid = shape.foldValid
        val diag = hypot(w, h)
        val foldTop = Offset(left + shape.foldTopX, top + shape.foldTopY)
        val foldBottom = Offset(left + shape.foldBottomX, top + shape.foldBottomY)
        val mid = Offset(left + shape.midX, top + shape.midY)
        val shadowWidth = CURL_SHADOW_WIDTH_DP.dp.toPx() * curlShadowWidthFraction(p)
        // The revealed page keeps its own measured rect, which can differ by a few pixels from the
        // turning page. Clip it to the part the fold has uncovered so it never peeks out beside
        // the turning page. Equal-size pages draw exactly as before.
        val reveal = revealDest ?: sheetRect
        val sameRect = abs(reveal.left - sheetRect.left) <= 1f &&
            abs(reveal.top - sheetRect.top) <= 1f &&
            abs(reveal.right - sheetRect.right) <= 1f &&
            abs(reveal.bottom - sheetRect.bottom) <= 1f
        if (shape.revealedCount >= 3) {
            revealedPath.setPolygon(shape.revealed, shape.revealedCount, left, top)
        }
        if (shape.frontCount >= 3) {
            frontPath.setPolygon(shape.front, shape.frontCount, left, top)
        }

        if (shape.flapCount >= 3) {
            flapPath.setPolygon(shape.flap, shape.flapCount, left, top)
        }

        // One pass of the whole frame in virtual-spread coordinates. A Book hinge gap draws it
        // twice with a clip on each side of the spine, so the flap is cut at the fold and the
        // gap keeps showing the fold gap (V071-TWOPAGE.md §4, option A).
        // Local function: the draw phase must not allocate.
        fun DrawScope.drawPass() {

            // The pager already shows the target spread, so two-page and single-page-over-spread
            // turns repaint its canvas and the page that must stay still from the current spread.
            if (twoPage != null || singlePage != null) {
                drawRect(ReaderBackground)
                val stillBitmap = stillImage
                val stillCropPx = stillCrop
                val stillRect = stillPageDest
                if (stillBitmap != null && stillCropPx != null && stillRect != null) {
                    // Uneven pages: the flap's opaque back face can be a few pixels shorter than the
                    // still page, so retire the still page's extra rows at its own outer edge as the
                    // turn advances. Exact at progress 0 (the whole still page) and 1 (only the rows
                    // under the back face), so the first and last frames equal the settled spreads.
                    if (uniformRects || flapBack == null || twoPage == null) {
                        drawImageInto(stillBitmap, stillCropPx, stillRect, colorFilter, scratch)
                    } else {
                        val face = twoPage.backFace
                        val clipTop = stillRect.top +
                            (face.top.coerceAtLeast(stillRect.top) - stillRect.top) * p
                        val clipBottom = stillRect.bottom +
                            (face.bottom.coerceAtMost(stillRect.bottom) - stillRect.bottom) * p
                        if (clipBottom > clipTop) {
                            clipRect(top = clipTop, bottom = clipBottom) {
                                drawImageInto(stillBitmap, stillCropPx, stillRect, colorFilter, scratch)
                            }
                        }
                    }
                }
            }

            if (twoPage != null) {
                if (p > 0.001f && shape.revealedCount >= 3) {
                    if (sameRect) {
                        drawImageInto(backImage, backCrop, reveal, colorFilter, scratch)
                    } else {
                        clipPath(revealedPath) {
                            drawImageInto(backImage, backCrop, reveal, colorFilter, scratch)
                        }
                    }
                }
            } else if (singlePage == null) {
                drawImageInto(backImage, backCrop, sheetRect, colorFilter, scratch)
            }
            if (p <= 0.001f) {
                drawImageInto(frontImage, crop, frontDest, colorFilter, scratch)
                return
            }

            if (!foldValid) {
                drawImageInto(frontImage, crop, frontDest, colorFilter, scratch)
                return
            }

            // The incoming spread shows only where the sheet has peeled away.
            if (underlayDraws.isNotEmpty() && shape.revealedCount >= 3) {
                clipPath(revealedPath) {
                    for (i in 0 until underlayDraws.size) {
                        val page = underlayDraws[i]
                        drawImageInto(page.image, page.crop, page.dest, colorFilter, scratch)
                    }
                }
            }

            // Shadow: one linear gradient from the fold onto the revealed page only.
            if (shadowWidth > 0.5f && shape.revealedCount >= 3) {
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
                clipPath(frontPath) {
                    drawImageInto(frontImage, crop, frontDest, colorFilter, scratch)
                }
            }

            // Flap: back face, inner shade, fold highlight.
            if (shape.flapCount >= 3) {
                // The back face covers the flap at full opacity. With uneven page rects it is shorter
                // than the flap polygon (the union), so clearing the whole flap would leave a black
                // hem over the paper; drawing nothing there lets the front or still page underneath
                // show, and the still page's own rows are retired above. Equal rects keep the fill.
                if (uniformRects || flapBack == null) {
                    drawPath(flapPath, flapColor)
                }
                clipPath(flapPath) {
                    val backFace = flapBack
                    val backFaceCrop = flapBackCrop
                    if (backFace != null && backFaceCrop != null) {
                        drawMirroredPage(
                            bitmap = backFace,
                            crop = backFaceCrop,
                            frontFit = drawFit,
                            midBaseX = left,
                            midBaseY = top,
                            stillDest = stillDest,
                            shape = shape,
                            mirror = mirror,
                            mirrorValues = backFaceValues,
                            paint = mirrorPaint,
                            androidFilter = androidFilter,
                            srcRect = srcRect,
                            dstRect = dstRect,
                            alpha = 255,
                            spineSide = side,
                        )
                    } else {
                        drawMirroredPage(
                            bitmap = front,
                            crop = crop,
                            frontFit = drawFit,
                            midBaseX = left,
                            midBaseY = top,
                            stillDest = null,
                            shape = shape,
                            mirror = mirror,
                            mirrorValues = mirrorValues,
                            paint = mirrorPaint,
                            androidFilter = androidFilter,
                            srcRect = srcRect,
                            dstRect = dstRect,
                            alpha = (CURL_MIRROR_ALPHA * 255f).roundToInt(),
                            spineSide = null,
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
                // The crease reaches the union's rows, which can be a few pixels past the pages that
                // are actually there at the end of the turn; clip it to the rows both the back face
                // and the landing page cover so the 1dp stroke does not stick out over the background.
                val creaseTop: Float
                val creaseBottom: Float
                if (!uniformRects && twoPage != null) {
                    val face = twoPage.backFace
                    val landingRect = twoPage.landing
                    creaseTop = foldTop.y.coerceAtLeast(maxOf(face.top, landingRect.top))
                    creaseBottom = foldBottom.y.coerceAtMost(minOf(face.bottom, landingRect.bottom))
                } else {
                    creaseTop = foldTop.y
                    creaseBottom = foldBottom.y
                }
                if (creaseBottom > creaseTop) {
                    clipRect(top = creaseTop, bottom = creaseBottom) {
                        drawLine(
                            color = foldHighlightColor,
                            start = foldTop,
                            end = foldBottom,
                            strokeWidth = CURL_FOLD_HIGHLIGHT_DP.dp.toPx(),
                        )
                    }
                }
            }
        }

        val hinge = twoPage?.hinge
        if (hinge == null) {
            drawPass()
        } else {
            val spine = twoPage.spineX
            clipRect(0f, 0f, spine, size.height) { drawPass() }
            clipRect(spine + hinge.gap, 0f, size.width, size.height) {
                translate(hinge.gap, 0f) { drawPass() }
            }
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
 * Draws [bitmap] into the turning flap.
 *
 * With [spineSide] null the transform is the fold reflection alone (the v0.7.0 mirror) and
 * the bitmap is drawn into [frontFit]. [midBaseX]/[midBaseY] are the shape's origin offset,
 * which is the front fit's top-left except for a lone page turning over the whole pane.
 * With [spineSide] set it is R∘Mspine and the bitmap is
 * turned into its still page across the spine, so at progress 1 it lands right-side up.
 */
private fun DrawScope.drawMirroredPage(
    bitmap: Bitmap,
    crop: PixelCrop,
    frontFit: RectF,
    midBaseX: Float,
    midBaseY: Float,
    stillDest: RectF?,
    shape: CurlShape,
    mirror: Matrix,
    mirrorValues: FloatArray,
    paint: Paint,
    androidFilter: android.graphics.ColorFilter?,
    srcRect: android.graphics.Rect,
    dstRect: RectF,
    alpha: Int,
    spineSide: CurlSide?,
) {
    if (crop.width <= 0 || crop.height <= 0 || frontFit.width() <= 0f || frontFit.height() <= 0f) return
    if (spineSide == null) {
        val nx = shape.normalX
        val ny = shape.normalY
        val a = 1f - 2f * nx * nx
        val b = -2f * nx * ny
        val d = 1f - 2f * ny * ny
        val midX = midBaseX + shape.midX
        val midY = midBaseY + shape.midY
        // Filled in place: the draw phase must not allocate.
        mirrorValues[0] = a
        mirrorValues[1] = b
        mirrorValues[2] = midX - (a * midX + b * midY)
        mirrorValues[3] = b
        mirrorValues[4] = d
        mirrorValues[5] = midY - (b * midX + d * midY)
        mirrorValues[6] = 0f
        mirrorValues[7] = 0f
        mirrorValues[8] = 1f
        dstRect.set(frontFit.left, frontFit.top, frontFit.right, frontFit.bottom)
    } else {
        flapBackMatrixValues(shape, midBaseX, midBaseY, frontFit.width(), spineSide, mirrorValues)
        if (stillDest != null) {
            // Two-page: land on the target page's measured rect so the last frame matches.
            dstRect.set(stillDest.left, stillDest.top, stillDest.right, stillDest.bottom)
        } else {
            val stillLeft = if (spineSide == CurlSide.Right) frontFit.left - frontFit.width() else frontFit.right
            dstRect.set(stillLeft, frontFit.top, stillLeft + frontFit.width(), frontFit.bottom)
        }
    }
    mirror.setValues(mirrorValues)
    paint.alpha = alpha
    paint.colorFilter = androidFilter
    srcRect.set(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height)
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val save = native.save()
        native.concat(mirror)
        native.drawBitmap(bitmap, srcRect, dstRect, paint)
        native.restoreToCount(save)
    }
}

/**
 * Writes the flap back-face matrix M = R ∘ Mspine into [out]: the sheet is first reflected
 * about the spine (x → 2·spineX − x, with spineX = [left] for [CurlSide.Right] and
 * left + [width] for [CurlSide.Left]) and then folded by R. The back face is drawn into its
 * still rect across the spine, so at progress 1 M is the identity and the frame equals the
 * still spread with no mirrored text. Filled in place: the draw phase must not allocate.
 */
internal fun flapBackMatrixValues(
    shape: CurlShape,
    left: Float,
    top: Float,
    width: Float,
    side: CurlSide,
    out: FloatArray,
) {
    val nx = shape.normalX
    val ny = shape.normalY
    val a = 1f - 2f * nx * nx
    val b = -2f * nx * ny
    val d = 1f - 2f * ny * ny
    val midX = left + shape.midX
    val midY = top + shape.midY
    val tx = midX - (a * midX + b * midY)
    val ty = midY - (b * midX + d * midY)
    val spineX = if (side == CurlSide.Right) left else left + width
    out[0] = -a
    out[1] = b
    out[2] = 2f * spineX * a + tx
    out[3] = -b
    out[4] = d
    out[5] = 2f * spineX * b + ty
    out[6] = 0f
    out[7] = 0f
    out[8] = 1f
}

/** A resolved underlay page: image, source crop and dest rect, ready for the draw phase. */
private class UnderlayDraw(
    val image: ImageBitmap,
    val crop: PixelCrop,
    val dest: RectF,
)

/** Draws the cropped source of [image] fit and centered inside [dest]. */
private fun DrawScope.drawImageInto(
    image: ImageBitmap,
    crop: PixelCrop,
    dest: RectF,
    colorFilter: ColorFilter?,
    fit: RectF,
) {
    if (crop.width <= 0 || crop.height <= 0 || dest.width() <= 0f || dest.height() <= 0f) return
    fitInto(crop, dest, fit)
    val scale = fit.width() / crop.width
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

/** Largest centered ContentScale.Fit rect for [crop] inside [dest], written into [out]. */
private fun fitInto(crop: PixelCrop, dest: RectF, out: RectF) {
    val scale = min(dest.width() / crop.width, dest.height() / crop.height)
    val width = crop.width * scale
    val height = crop.height * scale
    val left = dest.left + (dest.width() - width) / 2f
    val top = dest.top + (dest.height() - height) / 2f
    out.set(left, top, left + width, top + height)
}
