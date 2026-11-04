package org.parkjw.apps.pdfbook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlayVariant
import org.parkjw.apps.pdfbook.ui.theme.ReaderTapZoneLabel

class ReadingInfoTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun singlePageSpreadFormatsOneNumber() {
        assertEquals(ReadingInfoPage.Single(12, 180), readingInfoPage(listOf(12), 180))
    }

    @Test
    fun twoPageSpreadFormatsSmallerNumberFirst() {
        assertEquals(ReadingInfoPage.Spread(12, 13, 180), readingInfoPage(listOf(12, 13), 180))
    }

    @Test
    fun rtlSpreadStillPutsSmallerNumberFirst() {
        assertEquals(ReadingInfoPage.Spread(12, 13, 180), readingInfoPage(listOf(13, 12), 180))
    }

    @Test
    fun loneCoverPageStaysSingleInTwoPageMode() {
        assertEquals(ReadingInfoPage.Single(1, 180), readingInfoPage(listOf(1), 180))
    }

    @Test
    fun pagesOutsideTheBookAreDropped() {
        assertNull(readingInfoPage(listOf(181), 180))
        assertNull(readingInfoPage(emptyList(), 180))
        assertNull(readingInfoPage(listOf(1), 0))
    }

    @Test
    fun disabledItemsLeaveNoGap() {
        assertEquals(emptyList<ReadingInfoItem>(), readingInfoItems(null, null, null))
        assertEquals(
            listOf(ReadingInfoItem.Page(ReadingInfoPage.Single(12, 180))),
            readingInfoItems(ReadingInfoPage.Single(12, 180), null, null),
        )
        assertEquals(
            listOf(ReadingInfoItem.Time("22:52")),
            readingInfoItems(null, "22:52", null),
        )
        assertEquals(
            listOf(ReadingInfoItem.Battery(87)),
            readingInfoItems(null, null, 87),
        )
    }

    @Test
    fun itemsKeepPageTimeBatteryOrder() {
        assertEquals(
            listOf(
                ReadingInfoItem.Page(ReadingInfoPage.Spread(12, 13, 180)),
                ReadingInfoItem.Time("22:52"),
                ReadingInfoItem.Battery(87),
            ),
            readingInfoItems(ReadingInfoPage.Spread(12, 13, 180), "22:52", 87),
        )
        assertEquals(
            listOf(
                ReadingInfoItem.Page(ReadingInfoPage.Single(12, 180)),
                ReadingInfoItem.Battery(87),
            ),
            readingInfoItems(ReadingInfoPage.Single(12, 180), null, 87),
        )
    }

    @Test
    fun blankClockIsOmitted() {
        assertEquals(emptyList<ReadingInfoItem>(), readingInfoItems(null, " ", null))
    }

    @Test
    fun batteryPercentRoundsAndClamps() {
        assertEquals(87, readingInfoBatteryPercent(87, 100))
        assertEquals(33, readingInfoBatteryPercent(1, 3))
        assertEquals(100, readingInfoBatteryPercent(110, 100))
        assertNull(readingInfoBatteryPercent(-1, 100))
        assertNull(readingInfoBatteryPercent(50, 0))
    }

    @Test
    fun clockUsesDeviceFormatWithoutAmPm() {
        val tenFiftyTwo = 10 * 3_600_000L + 52 * 60_000L
        assertEquals(
            "10:52",
            readingInfoClockText(tenFiftyTwo, is24Hour = true, locale = Locale.US, timeZone = utc),
        )
        assertEquals(
            "10:52",
            readingInfoClockText(tenFiftyTwo, is24Hour = false, locale = Locale.US, timeZone = utc),
        )

        val nineOhFive = 9 * 3_600_000L + 5 * 60_000L
        assertEquals(
            "09:05",
            readingInfoClockText(nineOhFive, is24Hour = true, locale = Locale.US, timeZone = utc),
        )
        assertEquals(
            "9:05",
            readingInfoClockText(nineOhFive, is24Hour = false, locale = Locale.US, timeZone = utc),
        )
    }

    @Test
    fun edgeInsetIsCutoutPlusLargerOf8dpAnd30PercentRadius() {
        assertEquals(16f, readingInfoEdgeInsetPx(0, 0, 2f), 0.01f)
        assertEquals(24f, readingInfoEdgeInsetPx(0, 80, 2f), 0.01f)
        assertEquals(54f, readingInfoEdgeInsetPx(30, 80, 2f), 0.01f)
    }

    @Test
    fun chipTextStaysAtFiveToOneOverEveryPageMode() {
        // Default (white paper) is the worst case; inverted paper is black and sepia is warm.
        for (page in listOf(Color.White, Color.Black, Color(0xFFF4ECD8))) {
            val ratio = chipContrast(page)
            assertTrue("chip on ${page.value.toString(16)} = $ratio", ratio + 1e-6 >= 5.0)
        }
    }

    private fun chipContrast(page: Color): Double {
        val overlay = ReaderTapZoneLabel
        val alpha = overlay.alpha
        val background = Color(
            red = overlay.red * alpha + page.red * (1 - alpha),
            green = overlay.green * alpha + page.green * (1 - alpha),
            blue = overlay.blue * alpha + page.blue * (1 - alpha),
        )
        return contrastRatio(ReaderOnOverlayVariant, background)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val first = luminance(foreground)
        val second = luminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val s = value.toDouble()
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    @Test
    fun endNoticeRisesOnlyForTheChip() {
        assertEquals(END_NOTICE_BOTTOM, noticeBottomInset(bottomBarShown = false, bottomBarHeight = 0.dp))
        assertEquals(
            READING_INFO_NOTICE_BOTTOM,
            noticeBottomInset(bottomBarShown = false, bottomBarHeight = 0.dp, readingInfoShown = true),
        )
        assertEquals(
            64.dp + END_NOTICE_ABOVE_BAR,
            noticeBottomInset(bottomBarShown = true, bottomBarHeight = 64.dp, readingInfoShown = true),
        )
    }
}
