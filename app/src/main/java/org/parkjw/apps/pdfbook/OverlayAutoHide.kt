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

/**
 * Pure auto-hide state machine for the reader overlay. All times come from the
 * caller's monotonic clock, and the screen drives this same [nextDelay] sequence
 * the unit tests assert, so a pause and a reset behave identically in both.
 */
class OverlayAutoHideTimer(private val timeoutMillis: Long = OVERLAY_AUTO_HIDE_MILLIS) {
    private var lastInteractionAt = 0L

    /** A touch (or the overlay opening) restarts the countdown from [now]. */
    fun onInteraction(now: Long) {
        lastInteractionAt = now
    }

    /**
     * Returns how long the bars may stay visible from [now], or null while a
     * sheet is open, the slider is dragged, a pointer is held, TalkBack is on,
     * or the reader is not resumed.
     */
    fun nextDelay(
        now: Long,
        sheetOpen: Boolean,
        sliderDragging: Boolean,
        touchExploration: Boolean,
        pointerPressed: Boolean = false,
        resumed: Boolean = true,
    ): Long? {
        if (sheetOpen || sliderDragging || touchExploration || pointerPressed || !resumed) return null
        return (lastInteractionAt + timeoutMillis - now).coerceAtLeast(0L)
    }
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
    var resumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
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
