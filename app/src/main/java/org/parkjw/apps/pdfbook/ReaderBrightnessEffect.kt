package org.parkjw.apps.pdfbook

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ReaderBrightnessEffect(followSystem: Boolean, percent: Int) {
    val activity = LocalActivity.current ?: return
    val window = activity.window
    val override = readerBrightnessOverride(followSystem, percent)
    DisposableEffect(activity) {
        onDispose {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
    LaunchedEffect(window, override) {
        window.attributes = window.attributes.apply { screenBrightness = override }
    }
}
