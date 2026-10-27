package org.parkjw.apps.pdfbook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import android.view.RoundedCorner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.parkjw.apps.pdfbook.ui.theme.PdfBookNumeric
import org.parkjw.apps.pdfbook.ui.theme.PdfBookShapeTokens
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneLabel

internal val READING_INFO_MIN_MARGIN = 8.dp
private val READING_INFO_ICON = 12.dp
private val READING_INFO_CORNER_SHARE = 0.3f
private val READING_INFO_TEXT = PdfBookNumeric.copy(fontSize = 11.sp, color = ReaderOnOverlayVariant)

/** The spread's page number(s), smaller number first regardless of the reading direction. */
sealed interface ReadingInfoPage {
    data class Single(val page: Int, val count: Int) : ReadingInfoPage
    data class Spread(val first: Int, val last: Int, val count: Int) : ReadingInfoPage
}

fun readingInfoPage(spread: List<Int>, pageCount: Int): ReadingInfoPage? {
    if (pageCount <= 0) return null
    val pages = spread.filter { it in 1..pageCount }
    if (pages.isEmpty()) return null
    val first = pages.min()
    val last = pages.max()
    return if (first == last) {
        ReadingInfoPage.Single(first, pageCount)
    } else {
        ReadingInfoPage.Spread(first, last, pageCount)
    }
}

sealed interface ReadingInfoItem {
    data class Page(val value: ReadingInfoPage) : ReadingInfoItem
    data class Time(val text: String) : ReadingInfoItem
    data class Battery(val percent: Int) : ReadingInfoItem
}

/** Fixed order (page, time, battery); a disabled item leaves no gap. */
fun readingInfoItems(
    page: ReadingInfoPage?,
    time: String?,
    batteryPercent: Int?,
): List<ReadingInfoItem> = listOfNotNull(
    page?.let { ReadingInfoItem.Page(it) },
    time?.takeIf { it.isNotBlank() }?.let { ReadingInfoItem.Time(it) },
    batteryPercent?.let { ReadingInfoItem.Battery(it.coerceIn(0, 100)) },
)

fun readingInfoClockText(
    timeMillis: Long,
    is24Hour: Boolean,
    locale: Locale,
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm"
    return SimpleDateFormat(pattern, locale).apply { this.timeZone = timeZone }.format(Date(timeMillis))
}

fun readingInfoBatteryPercent(level: Int, scale: Int): Int? {
    if (level < 0 || scale <= 0) return null
    return ((level * 100.0) / scale).roundToInt().coerceIn(0, 100)
}

/**
 * Corner margin: display cutout (waterfall included) + max(8dp, 0.3 × rounded corner radius).
 */
fun readingInfoEdgeInsetPx(cutoutPx: Int, cornerRadiusPx: Int, density: Float): Float =
    cutoutPx + max(density * READING_INFO_MIN_MARGIN.value, READING_INFO_CORNER_SHARE * cornerRadiusPx)

@Composable
fun ReadingInfoHost(
    spreads: List<List<Int>>,
    pagerState: PagerState,
    maxWidth: Dp,
    showPage: Boolean,
    showTime: Boolean,
    showBattery: Boolean,
    omitPage: Boolean,
    atFold: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showPage && !showTime && !showBattery) return
    val density = LocalDensity.current
    val view = LocalView.current
    val cutout = view.rootWindowInsets?.displayCutout
    val cutoutRight = max(cutout?.safeInsetRight ?: 0, cutout?.waterfallInsets?.right ?: 0)
    val cutoutBottom = max(cutout?.safeInsetBottom ?: 0, cutout?.waterfallInsets?.bottom ?: 0)
    val cornerRadiusPx = view.display?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
    val endInset = with(density) {
        readingInfoEdgeInsetPx(cutoutRight, cornerRadiusPx, density.density).toDp()
    }
    // The tabletop pill sits at the hinge, not at a rounded screen edge.
    val bottomInset = if (atFold) {
        READING_INFO_MIN_MARGIN
    } else {
        with(density) {
            readingInfoEdgeInsetPx(cutoutBottom, cornerRadiusPx, density.density).toDp()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(END_NOTICE_FADE_MILLIS)),
        exit = fadeOut(tween(END_NOTICE_FADE_MILLIS)),
        modifier = modifier
            .padding(end = endInset, bottom = bottomInset),
    ) {
        // Clock and battery listeners live here so they stop while the chip is hidden.
        ReadingInfoLive(
            spreads = spreads,
            pagerState = pagerState,
            maxWidth = maxWidth,
            showPage = showPage,
            showTime = showTime,
            showBattery = showBattery,
            omitPage = omitPage,
        )
    }
}

@Composable
private fun ReadingInfoLive(
    spreads: List<List<Int>>,
    pagerState: PagerState,
    maxWidth: Dp,
    showPage: Boolean,
    showTime: Boolean,
    showBattery: Boolean,
    omitPage: Boolean,
) {
    val page = if (showPage && !omitPage) {
        readingInfoPage(spreads.getOrNull(pagerState.settledPage).orEmpty(), pagerState.pageCount)
    } else {
        null
    }
    val clock = if (showTime) rememberReadingInfoClock() else null
    val battery = if (showBattery) rememberReadingInfoBattery() else null
    ReadingInfoChip(page = page, clock = clock, battery = battery, maxWidth = maxWidth)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReadingInfoChip(
    page: ReadingInfoPage?,
    clock: ReadingInfoClock?,
    battery: Int?,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val items = readingInfoItems(page, clock?.display, battery)
    if (items.isEmpty()) return
    val pageSpoken = when (page) {
        is ReadingInfoPage.Single -> stringResource(R.string.reader_info_page_a11y, page.page, page.count)
        is ReadingInfoPage.Spread -> stringResource(
            R.string.reader_info_pages_a11y,
            page.first,
            page.last,
            page.count,
        )
        null -> null
    }
    val batterySpoken = battery?.let { stringResource(R.string.reader_info_battery_a11y, it) }
    val description = listOfNotNull(pageSpoken, clock?.spoken, batterySpoken).joinToString(", ")
    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .testTag("reader_info_chip")
            .clip(PdfBookShapeTokens.pill)
            .background(ReaderTapZoneLabel, PdfBookShapeTokens.pill)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = PdfBookSpacing.xs, vertical = PdfBookSpacing.xxs),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.xs, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                when (item) {
                    is ReadingInfoItem.Page -> InfoText(
                        when (val value = item.value) {
                            is ReadingInfoPage.Single -> stringResource(
                                R.string.reader_info_page,
                                value.page,
                                value.count,
                            )
                            is ReadingInfoPage.Spread -> stringResource(
                                R.string.reader_info_pages,
                                value.first,
                                value.last,
                                value.count,
                            )
                        },
                    )
                    is ReadingInfoItem.Time -> InfoText(item.text)
                    is ReadingInfoItem.Battery -> Row(
                        horizontalArrangement = Arrangement.spacedBy(PdfBookSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_battery),
                            contentDescription = null,
                            tint = ReaderOnOverlayVariant,
                            modifier = Modifier.size(READING_INFO_ICON),
                        )
                        InfoText(stringResource(R.string.reader_info_battery, item.percent))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text = text, style = READING_INFO_TEXT, maxLines = 1)
}

internal data class ReadingInfoClock(val display: String, val spoken: String)

@Composable
private fun rememberReadingInfoClock(): ReadingInfoClock {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var formatTick by remember { mutableIntStateOf(0) }
    val is24Hour = remember(formatTick, context) { DateFormat.is24HourFormat(context) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            now = current
            delay(60_000L - current % 60_000L)
        }
    }
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                now = System.currentTimeMillis()
                formatTick++
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val display = remember(now, is24Hour, locale) {
        readingInfoClockText(now, is24Hour, locale)
    }
    val spoken = remember(now, locale, formatTick) {
        DateFormat.getTimeFormat(context).format(Date(now))
    }
    return ReadingInfoClock(display, spoken)
}

@Composable
private fun rememberReadingInfoBattery(): Int? {
    val context = LocalContext.current
    var percent by remember {
        mutableStateOf<Int?>(
            readingInfoBatteryPercentFrom(
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)),
            ),
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                percent = readingInfoBatteryPercentFrom(intent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return percent
}

private fun readingInfoBatteryPercentFrom(intent: Intent?): Int? = readingInfoBatteryPercent(
    level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
    scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1,
)
