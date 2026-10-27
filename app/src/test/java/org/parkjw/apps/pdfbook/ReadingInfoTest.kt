package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
