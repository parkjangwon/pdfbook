package dev.parkjw.pdfbook

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

fun shouldUseTwoPage(widthDp: Int, halfOpened: Boolean): Boolean =
    !halfOpened && widthDp >= 600

@Composable
fun rememberTwoPage(): Boolean {
    val context = LocalContext.current
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val widthDp = (containerSize.width / density.density).toInt()
    val halfOpened by produceState(initialValue = false, context) {
        val activity = context as? Activity ?: return@produceState
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { info ->
                value = info.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .any { it.state == FoldingFeature.State.HALF_OPENED }
            }
    }
    return shouldUseTwoPage(widthDp, halfOpened)
}
