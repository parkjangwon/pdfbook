package org.parkjw.apps.pdfbook

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun ReaderBrightnessEffect(followSystem: Boolean, percent: Int) {
    val activity = LocalActivity.current ?: return
    val override = readerBrightnessOverride(followSystem, percent)
    DisposableEffect(activity, override) {
        val window = activity.window
        window.attributes = window.attributes.apply { screenBrightness = override }
        onDispose {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
}
