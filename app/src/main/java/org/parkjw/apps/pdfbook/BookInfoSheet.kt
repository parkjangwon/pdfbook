package org.parkjw.apps.pdfbook

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.parkjw.apps.pdfbook.ui.safeDrawingBottomBarPadding
import org.parkjw.apps.pdfbook.ui.theme.PdfBookMonoFamily
import org.parkjw.apps.pdfbook.ui.theme.SheetTopCorner
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.withNumericSpans
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.math.roundToLong

const val BOOK_INFO_SHEET_TAG = "book_info_sheet"

enum class FileSizeUnit { B, KB, MB, GB }

data class FileSizeText(val value: String, val unit: FileSizeUnit)

data class ReadingProgress(val page: Int, val pageCount: Int, val percent: Int)

fun fileSizeOf(bytes: Long): FileSizeText? {
    if (bytes <= 0) return null
    val unit = when {
        bytes >= 1024L * 1024 * 1024 -> FileSizeUnit.GB
        bytes >= 1024L * 1024 -> FileSizeUnit.MB
        bytes >= 1024L -> FileSizeUnit.KB
        else -> FileSizeUnit.B
    }
    val size = sizeIn(bytes, unit)
    val promoted = when {
        size.value != "1024" -> null
        unit == FileSizeUnit.KB -> FileSizeUnit.MB
        unit == FileSizeUnit.MB -> FileSizeUnit.GB
        else -> null
    }
    return promoted?.let { sizeIn(bytes, it) } ?: size
}

private fun sizeIn(bytes: Long, unit: FileSizeUnit): FileSizeText {
    val factor = when (unit) {
        FileSizeUnit.B -> 1L
        FileSizeUnit.KB -> 1024L
        FileSizeUnit.MB -> 1024L * 1024
        FileSizeUnit.GB -> 1024L * 1024 * 1024
    }
    return if (unit == FileSizeUnit.B) {
        FileSizeText(bytes.toString(), unit)
    } else {
        FileSizeText(decimal(bytes.toDouble() / factor), unit)
    }
}

private fun decimal(value: Double): String {
    val rounded = (value * 10).roundToLong() / 10.0
    val text = String.format(Locale.ROOT, "%.1f", rounded)
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

fun readingProgressOf(state: BookState?, pageCount: Int): ReadingProgress? {
    if (pageCount <= 0) return null
    val page = state?.takeIf { it.openedAt > 0 }?.page?.coerceIn(0, pageCount) ?: 0
    val percent = (page.toFloat() / pageCount * 100f).roundToInt()
    return ReadingProgress(page, pageCount, percent)
}

fun lastReadText(
    openedAt: Long,
    locale: Locale,
    timeZone: TimeZone = TimeZone.getDefault(),
): String? {
    if (openedAt <= 0) return null
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply {
        this.timeZone = timeZone
    }.format(Date(openedAt))
}

suspend fun readPageCount(context: Context, book: BookInfo): Int? = withContext(Dispatchers.IO) {
    val pfd = try {
        context.contentResolver.openFileDescriptor(Uri.parse(book.uri), "r")
    } catch (_: Exception) {
        null
    } ?: return@withContext null
    try {
        PageRenderer.open(pfd).use { it.pageCount }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoSheet(
    book: BookInfo,
    state: BookState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    pageCountLoader: (suspend (BookInfo) -> Int?)? = null,
) {
    val context = LocalContext.current
    val loadPageCount = pageCountLoader ?: { target: BookInfo -> readPageCount(context, target) }
    val storedPageCount = state?.pageCount?.takeIf { it > 0 }
    var pageCount by remember(book.uri, storedPageCount) { mutableStateOf(storedPageCount) }
    var loading by remember(book.uri, storedPageCount) { mutableStateOf(storedPageCount == null) }
    var unreadable by remember(book.uri, storedPageCount) { mutableStateOf(false) }
    LaunchedEffect(book.uri, storedPageCount) {
        if (storedPageCount != null) return@LaunchedEffect
        loading = true
        unreadable = false
        val count = loadPageCount(book)
        pageCount = count
        unreadable = count == null
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(BOOK_INFO_SHEET_TAG).semantics { testTagsAsResourceId = true },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 640.dp,
        shape = SheetTopCorner,
        containerColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = 0.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.extendedColors.hairlineStrong)
        },
    ) {
        val scheme = MaterialTheme.colorScheme
        val locale = LocalConfiguration.current.locales[0]
        val size = fileSizeOf(book.size)
        val progress = readingProgressOf(state, pageCount ?: 0)
        val lastRead = remember(state?.openedAt, locale) {
            lastReadText(state?.openedAt ?: 0L, locale)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingBottomBarPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.book_info_title),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
            )
            InfoRow(
                label = stringResource(R.string.book_info_file_name),
                value = book.name,
                numeric = false,
                labelColor = scheme.onSurfaceVariant,
                valueColor = scheme.onSurface,
            )
            InfoRow(
                label = stringResource(R.string.book_info_pages),
                value = when {
                    loading -> stringResource(R.string.book_info_pages_loading)
                    unreadable -> stringResource(R.string.book_info_pages_unreadable)
                    else -> pluralStringResource(
                        R.plurals.book_info_page_count,
                        pageCount ?: 0,
                        pageCount ?: 0,
                    )
                },
                numeric = pageCount != null,
                labelColor = scheme.onSurfaceVariant,
                valueColor = scheme.onSurface,
            )
            InfoRow(
                label = stringResource(R.string.book_info_size),
                value = size?.let { stringResource(sizeUnitLabel(it.unit), it.value) }
                    ?: stringResource(R.string.book_info_unknown),
                numeric = true,
                labelColor = scheme.onSurfaceVariant,
                valueColor = scheme.onSurface,
            )
            InfoRow(
                label = stringResource(R.string.book_info_last_read),
                value = lastRead ?: stringResource(R.string.book_info_not_read),
                numeric = lastRead != null,
                labelColor = scheme.onSurfaceVariant,
                valueColor = scheme.onSurface,
            )
            InfoRow(
                label = stringResource(R.string.book_info_progress),
                value = progress?.let {
                    stringResource(R.string.book_info_progress_value, it.page, it.pageCount, it.percent)
                } ?: stringResource(R.string.book_info_unknown),
                numeric = progress != null,
                labelColor = scheme.onSurfaceVariant,
                valueColor = scheme.onSurface,
            )
        }
    }
}

private fun sizeUnitLabel(unit: FileSizeUnit): Int = when (unit) {
    FileSizeUnit.B -> R.string.book_info_size_b
    FileSizeUnit.KB -> R.string.book_info_size_kb
    FileSizeUnit.MB -> R.string.book_info_size_mb
    FileSizeUnit.GB -> R.string.book_info_size_gb
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    numeric: Boolean,
    labelColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Text(
            text = if (numeric) {
                withNumericSpans(
                    value,
                    SpanStyle(fontFamily = PdfBookMonoFamily, letterSpacing = (-0.2).sp),
                )
            } else {
                AnnotatedString(value)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
        )
    }
}
