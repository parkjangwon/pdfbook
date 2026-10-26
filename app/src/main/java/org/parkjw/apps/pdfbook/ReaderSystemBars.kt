package org.parkjw.apps.pdfbook

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun readerHidesSystemBars(
    inReader: Boolean,
    fullScreenReading: Boolean,
    overlayVisible: Boolean,
): Boolean = inReader && fullScreenReading && !overlayVisible

fun interface ReaderSystemBarsController {
    fun setBarsHidden(hidden: Boolean)
}

internal val LocalReaderSystemBarsController =
    staticCompositionLocalOf<ReaderSystemBarsController?> { null }

@Composable
private fun rememberReaderSystemBarsController(): ReaderSystemBarsController? {
    val view = LocalView.current
    val provided = LocalReaderSystemBarsController.current
    return remember(view, provided) {
        provided ?: view.context.findActivity()?.window?.let { window ->
            WindowReaderSystemBarsController(window, view)
        }
    }
}

@Composable
internal fun ReaderSystemBars(
    fullScreenReading: Boolean,
    overlayVisible: Boolean,
) {
    val controller = rememberReaderSystemBarsController()
    val hidden = readerHidesSystemBars(
        inReader = true,
        fullScreenReading = fullScreenReading,
        overlayVisible = overlayVisible,
    )
    LaunchedEffect(controller, hidden) { controller?.setBarsHidden(hidden) }
}

/** Owns the bars for the whole reader route so a book switch keeps them hidden. */
@Composable
internal fun ReaderSystemBarsScope() {
    val controller = rememberReaderSystemBarsController()
    DisposableEffect(controller) {
        onDispose { controller?.setBarsHidden(false) }
    }
}

private class WindowReaderSystemBarsController(
    window: Window,
    view: View,
) : ReaderSystemBarsController {
    private val controller = WindowCompat.getInsetsController(window, view)

    override fun setBarsHidden(hidden: Boolean) {
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
