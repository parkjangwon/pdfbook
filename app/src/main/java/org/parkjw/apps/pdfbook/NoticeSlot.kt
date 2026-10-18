package org.parkjw.apps.pdfbook

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val END_NOTICE_BOTTOM = 28.dp
val END_NOTICE_ABOVE_BAR = 12.dp

fun noticeBottomInset(bottomBarShown: Boolean, bottomBarHeight: Dp): Dp =
    if (bottomBarShown) bottomBarHeight + END_NOTICE_ABOVE_BAR else END_NOTICE_BOTTOM
