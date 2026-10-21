package org.parkjw.apps.pdfbook

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntRect
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/** Device posture for foldables. Flat covers FLAT state and devices with no fold. */
sealed interface FoldPosture {
    data object Flat : FoldPosture
    data class Book(val foldBounds: IntRect) : FoldPosture
    data class Tabletop(val foldBounds: IntRect) : FoldPosture
}

/**
 * Platform-free fold feature for [postureOf].
 * [verticalHinge] true = fold runs top–bottom (book); false = left–right (tabletop).
 */
data class FoldFeatureInput(
    val halfOpened: Boolean,
    val verticalHinge: Boolean,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

fun postureOf(features: List<FoldFeatureInput>): FoldPosture {
    val open = features.firstOrNull { it.halfOpened } ?: return FoldPosture.Flat
    val bounds = IntRect(open.left, open.top, open.right, open.bottom)
    return if (open.verticalHinge) FoldPosture.Book(bounds) else FoldPosture.Tabletop(bounds)
}

fun FoldingFeature.toFoldFeatureInput(): FoldFeatureInput = FoldFeatureInput(
    halfOpened = state == FoldingFeature.State.HALF_OPENED,
    verticalHinge = orientation == FoldingFeature.Orientation.VERTICAL,
    left = bounds.left,
    top = bounds.top,
    right = bounds.right,
    bottom = bounds.bottom,
)

val LocalFoldPosture = compositionLocalOf<FoldPosture?> { null }

@Composable
fun rememberFoldPosture(): FoldPosture {
    LocalFoldPosture.current?.let { return it }
    val context = LocalContext.current
    val posture by produceState<FoldPosture>(FoldPosture.Flat, context) {
        val activity = context as? Activity ?: return@produceState
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { info ->
                value = postureOf(
                    info.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .map { it.toFoldFeatureInput() },
                )
            }
    }
    return posture
}
