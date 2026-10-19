package org.parkjw.apps.pdfbook

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

fun trimCallbacks(renderer: PageBitmaps): ComponentCallbacks2 = object : ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        renderer.trimToCurrent()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        renderer.trimToCurrent()
    }
}

@Composable
fun TrimCacheOnMemoryPressure(renderer: PageBitmaps) {
    val app = LocalContext.current.applicationContext
    DisposableEffect(app, renderer) {
        val callbacks = trimCallbacks(renderer)
        app.registerComponentCallbacks(callbacks)
        onDispose { app.unregisterComponentCallbacks(callbacks) }
    }
}
