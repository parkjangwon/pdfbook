package org.parkjw.apps.pdfbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.parkjw.apps.pdfbook.settings.TwoPageMode

fun shouldUseTwoPage(mode: TwoPageMode, widthDp: Int, posture: FoldPosture): Boolean =
    when (posture) {
        is FoldPosture.Tabletop -> false
        is FoldPosture.Book -> mode != TwoPageMode.AlwaysSingle
        FoldPosture.Flat -> when (mode) {
            TwoPageMode.AlwaysSingle -> false
            TwoPageMode.AlwaysDouble -> true
            TwoPageMode.Auto -> widthDp >= 600
        }
    }

@Composable
fun rememberTwoPage(mode: TwoPageMode, posture: FoldPosture = rememberFoldPosture()): Boolean {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val widthDp = (containerSize.width / density.density).toInt()
    return shouldUseTwoPage(mode, widthDp, posture)
}
