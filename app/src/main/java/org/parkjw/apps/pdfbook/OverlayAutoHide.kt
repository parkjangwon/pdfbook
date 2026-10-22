package org.parkjw.apps.pdfbook

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

const val OVERLAY_AUTO_HIDE_MILLIS = 4_000L

// Overlay bars share their own fade duration; retiming the next-book notice
// (END_NOTICE_FADE_MILLIS) must not retime them.
const val OVERLAY_FADE_MILLIS = 150

/**
 * Pure auto-hide state machine for the reader overlay. All times are injected by
 * the caller, so resets and pauses are unit-testable without a Compose clock.
 */
class OverlayAutoHideTimer(private val timeoutMillis: Long = OVERLAY_AUTO_HIDE_MILLIS) {
    private var lastInteractionAt = 0L
    private var paused = true

    /** A touch anywhere on the overlay restarts the countdown from [now]. */
    fun onInteraction(now: Long) {
        lastInteractionAt = now
    }

    /** Recompute after a pause condition changed; returns the expiry, or null while paused. */
    fun update(
        sheetOpen: Boolean,
        sliderDragging: Boolean,
        touchExploration: Boolean,
    ): Long? {
        paused = sheetOpen || sliderDragging || touchExploration
        return if (paused) null else lastInteractionAt + timeoutMillis
    }

    /** True once [now] has reached the expiry last returned by [update]. */
    fun expiredAt(now: Long): Boolean = !paused && now - lastInteractionAt >= timeoutMillis
}

@Composable
fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = manager?.isTouchExplorationEnabled == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

@Composable
fun rememberLifecycleResumed(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return resumed
}
