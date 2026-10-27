package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.parkjw.apps.pdfbook.CropMargins
import org.parkjw.apps.pdfbook.PageBitmaps
import org.parkjw.apps.pdfbook.detectCropMargins

class SolidPageBitmaps(
    override val pageCount: Int,
    private val pageColor: Int,
    private val accentColor: Int,
) : PageBitmaps {
    private val crops = HashMap<Int, CropMargins>()
    private val cropUpdatesFlow = MutableStateFlow(0)
    @Volatile private var autoCrop = false

    override fun setAutoCrop(enabled: Boolean) {
        autoCrop = enabled
    }

    override fun cropMargins(page: Int): CropMargins? =
        if (autoCrop) synchronized(crops) { crops[page] } else null

    override val cropUpdates: Flow<Int> get() = cropUpdatesFlow

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
        if (autoCrop && synchronized(crops) { page !in crops }) {
            val margins = detectCropMargins(bitmap)
            synchronized(crops) { crops[page] = margins }
            cropUpdatesFlow.update { it + 1 }
        }
        return bitmap
    }
}
