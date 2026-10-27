package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val END_NOTICE_BOTTOM = 28.dp
val END_NOTICE_ABOVE_BAR = 12.dp
val READING_INFO_NOTICE_BOTTOM = 40.dp

fun noticeBottomInset(
    bottomBarShown: Boolean,
    bottomBarHeight: Dp,
    readingInfoShown: Boolean = false,
): Dp = when {
    bottomBarShown -> bottomBarHeight + END_NOTICE_ABOVE_BAR
    readingInfoShown -> READING_INFO_NOTICE_BOTTOM
    else -> END_NOTICE_BOTTOM
}
