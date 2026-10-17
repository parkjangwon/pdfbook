package org.parkjw.apps.pdfbook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.safeDrawingPadding(): Modifier =
    windowInsetsPadding(WindowInsets.safeDrawing)

@Composable
fun Modifier.safeDrawingTopBarPadding(): Modifier =
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    )

@Composable
fun Modifier.safeDrawingBottomBarPadding(): Modifier =
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    )

@Composable
fun SafeTopBar(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingTopBarPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

@Composable
fun safeDrawingGridContentPadding(base: Dp = 16.dp): PaddingValues {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = base + insets.calculateStartPadding(layoutDirection),
        top = base,
        end = base + insets.calculateEndPadding(layoutDirection),
        bottom = base + insets.calculateBottomPadding(),
    )
}
