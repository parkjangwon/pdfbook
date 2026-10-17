package org.parkjw.apps.pdfbook.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme

internal const val FOLDED_QUALIFIERS = "w360dp-h920dp-xxhdpi"
internal const val FOLDED_NIGHT_QUALIFIERS = "w360dp-h920dp-night-xxhdpi"
internal const val UNFOLDED_QUALIFIERS = "w656dp-h728dp-xxhdpi"
internal const val UNFOLDED_NIGHT_QUALIFIERS = "w656dp-h728dp-night-xxhdpi"

internal fun solidCover(color: Int, accent: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = accent
        style = Paint.Style.FILL
    }
    canvas.drawRect(16f, 24f, 104f, 72f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.drawCircle(60f, 120f, 28f, paint)
    return bitmap
}

internal fun sampleListing(): Listing = Listing(
    folderName = "서재",
    books = listOf(
        BookInfo("content://shot/a.pdf", "첫 번째 책.pdf", 1000),
        BookInfo("content://shot/b.pdf", "두 번째 책.pdf", 2000),
        BookInfo("content://shot/c.pdf", "세 번째 책.pdf", 3000),
    ),
)

internal fun sampleStates(listing: Listing): Map<String, BookState> {
    val first = listing.books.first()
    val second = listing.books[1]
    return mapOf(
        bookKey(first.uri, first.size) to BookState(page = 12, pageCount = 40, openedAt = 3_000L),
        bookKey(second.uri, second.size) to BookState(page = 3, pageCount = 20, openedAt = 2_000L),
        bookKey(listing.books[2].uri, listing.books[2].size) to BookState(page = 1, pageCount = 10, openedAt = 1_000L),
    )
}

internal fun sampleCovers(listing: Listing): Map<String, Bitmap?> = mapOf(
    bookKey(listing.books[0].uri, listing.books[0].size) to solidCover(0xFFD97757.toInt(), 0xFF2F2A26.toInt()),
    bookKey(listing.books[1].uri, listing.books[1].size) to solidCover(0xFF6B8F71.toInt(), 0xFFF4F0E8.toInt()),
    bookKey(listing.books[2].uri, listing.books[2].size) to solidCover(0xFF4A6FA5.toInt(), 0xFFE8EEF5.toInt()),
)

internal fun sampleSettings(): AppSettings = AppSettings(
    themeMode = ThemeMode.Light,
    defaultReadingDirection = ReadingDirection.RTL,
)

@Composable
internal fun ScreenshotTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    PdfBookTheme(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                )
                Box(Modifier.weight(1f).fillMaxWidth(), content = { content() })
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                )
            }
        }
    }
}
