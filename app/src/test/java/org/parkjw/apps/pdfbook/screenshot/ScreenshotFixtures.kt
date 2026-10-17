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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.parkjw.apps.pdfbook.BookInfo
import org.parkjw.apps.pdfbook.BookState
import org.parkjw.apps.pdfbook.FolderShelfUi
import org.parkjw.apps.pdfbook.Listing
import org.parkjw.apps.pdfbook.ReadingDirection
import org.parkjw.apps.pdfbook.bookKey
import org.parkjw.apps.pdfbook.buildFolderShelfUi
import org.parkjw.apps.pdfbook.folders.Folder
import org.parkjw.apps.pdfbook.folders.FolderAccess
import org.parkjw.apps.pdfbook.settings.AppSettings
import org.parkjw.apps.pdfbook.settings.ThemeMode
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme

internal const val FOLDED_QUALIFIERS = "ko-w360dp-h920dp-xxhdpi"
internal const val FOLDED_NIGHT_QUALIFIERS = "ko-w360dp-h920dp-night-xxhdpi"
internal const val UNFOLDED_QUALIFIERS = "ko-w656dp-h728dp-xxhdpi"
internal const val UNFOLDED_NIGHT_QUALIFIERS = "ko-w656dp-h728dp-night-xxhdpi"
internal const val FOLDED_EN_QUALIFIERS = "en-w360dp-h920dp-xxhdpi"

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

internal fun sampleShelfFolders(): List<Folder> = listOf(
    Folder("content://tree/weekly", "주간 연재", 0, FolderAccess.Ok, lastOpenedAt = 3_000L),
    Folder("content://tree/done", "완결작", 1, FolderAccess.Ok, lastOpenedAt = 2_000L),
    Folder("content://tree/lost", "옛 폴더", 2, FolderAccess.Lost, lastOpenedAt = 1_000L),
)

internal fun sampleShelfListings(): Map<String, Listing?> {
    val weekly = listOf(
        BookInfo("content://tree/weekly/doc/a.pdf", "밤의 정원.pdf", 1000),
        BookInfo("content://tree/weekly/doc/b.pdf", "Harbor Lights.pdf", 2000),
        BookInfo("content://tree/weekly/doc/c.pdf", "시계탑 아래.pdf", 3000),
        BookInfo("content://tree/weekly/doc/d.pdf", "잔잔한 오후.pdf", 4000),
    )
    val done = listOf(
        BookInfo("content://tree/done/doc/e.pdf", "North Line.pdf", 5000),
        BookInfo("content://tree/done/doc/f.pdf", "Paper Moon.pdf", 6000),
    )
    return mapOf(
        "content://tree/weekly" to Listing("주간 연재", weekly),
        "content://tree/done" to Listing("완결작", done),
        "content://tree/lost" to null,
    )
}

internal fun sampleShelfStates(listings: Map<String, Listing?>): Map<String, BookState> {
    val weekly = listings["content://tree/weekly"]!!.books
    val done = listings["content://tree/done"]!!.books
    return mapOf(
        bookKey(weekly[1].uri, weekly[1].size) to BookState(page = 3, pageCount = 7, openedAt = 9_000L),
        bookKey(weekly[0].uri, weekly[0].size) to BookState(page = 12, pageCount = 40, openedAt = 4_000L),
        bookKey(done[0].uri, done[0].size) to BookState(page = 8, pageCount = 8, openedAt = 5_000L),
    )
}

internal fun sampleShelfCovers(listings: Map<String, Listing?>): Map<String, Bitmap?> {
    val books = listings.values.filterNotNull().flatMap { it.books }
    val colors = listOf(
        0xFFD97757.toInt() to 0xFF2F2A26.toInt(),
        0xFF6B8F71.toInt() to 0xFFF4F0E8.toInt(),
        0xFF4A6FA5.toInt() to 0xFFE8EEF5.toInt(),
        0xFFC4A35A.toInt() to 0xFF2A2418.toInt(),
        0xFF8B5E6E.toInt() to 0xFFF6EBEF.toInt(),
        0xFF3E8E6E.toInt() to 0xFFE8F2EC.toInt(),
    )
    return books.mapIndexed { index, book ->
        val (bg, accent) = colors[index % colors.size]
        bookKey(book.uri, book.size) to solidCover(bg, accent)
    }.toMap()
}

internal fun sampleShelfUi(): FolderShelfUi {
    val listings = sampleShelfListings()
    return buildFolderShelfUi(sampleShelfFolders(), listings, sampleShelfStates(listings))
}

internal fun sampleLostOnlyShelfUi(): FolderShelfUi {
    val folders = listOf(
        Folder("content://tree/lost", "옛 폴더", 0, FolderAccess.Lost, lastOpenedAt = 1_000L),
    )
    return buildFolderShelfUi(folders, mapOf("content://tree/lost" to null), emptyMap())
}

@Composable
internal fun ScreenshotTheme(
    dark: Boolean,
    systemBarsColor: Color? = null,
    content: @Composable () -> Unit,
) {
    PdfBookTheme(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val bars = systemBarsColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(bars),
                )
                Box(Modifier.weight(1f).fillMaxWidth(), content = { content() })
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(bars),
                )
            }
        }
    }
}
