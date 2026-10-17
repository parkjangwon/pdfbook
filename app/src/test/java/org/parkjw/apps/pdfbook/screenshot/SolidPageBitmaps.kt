package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.parkjw.apps.pdfbook.PageBitmaps

class SolidPageBitmaps(
    override val pageCount: Int,
    private val pageColor: Int,
    private val accentColor: Int,
) : PageBitmaps {
    override suspend fun render(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (page < 1 || page > pageCount || maxWidth <= 0 || maxHeight <= 0) return null
        val width = maxWidth.coerceAtMost(480).coerceAtLeast(1)
        val height = maxHeight.coerceAtMost(720).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(pageColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        val margin = width / 8f
        canvas.drawRect(margin, margin, width - margin, height / 3f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(width / 2f, height * 0.65f, width / 5f, paint)
        return bitmap
    }
}
