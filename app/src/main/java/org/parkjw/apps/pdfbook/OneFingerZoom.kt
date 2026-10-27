package org.parkjw.apps.pdfbook

enum class OneFingerZoomPhase {
    Idle,
    FirstDown,
    AwaitSecondDown,
    SecondDown,
    Holding,
}

sealed interface OneFingerZoomCommand {
    data object Tap : OneFingerZoomCommand
    data object DoubleTap : OneFingerZoomCommand
    data class HoldStart(val x: Float, val y: Float) : OneFingerZoomCommand
    data class HoldPan(val dx: Float, val dy: Float) : OneFingerZoomCommand
    data object HoldEnd : OneFingerZoomCommand
}

/**
 * Tap / double-tap / double-tap-and-hold decisions for one tap zone.
 *
 * Timeouts belong to the caller: it waits [timeoutMillis] for the current phase and then calls
 * [timeout]. Events inside a phase (small jitter) therefore never restart the hold timer.
 */
class OneFingerZoomDetector(
    private val doubleTapMillis: Long = 300L,
    private val holdMillis: Long = 500L,
    private val touchSlop: Float = 0f,
) {
    var phase: OneFingerZoomPhase = OneFingerZoomPhase.Idle
        private set

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private var firstHeld = false

    fun timeoutMillis(): Long? = when (phase) {
        OneFingerZoomPhase.FirstDown -> if (firstHeld) null else holdMillis
        OneFingerZoomPhase.AwaitSecondDown -> doubleTapMillis
        OneFingerZoomPhase.SecondDown -> holdMillis
        OneFingerZoomPhase.Idle, OneFingerZoomPhase.Holding -> null
    }

    fun down(x: Float, y: Float): List<OneFingerZoomCommand> {
        val nextPhase = when (phase) {
            OneFingerZoomPhase.Idle -> OneFingerZoomPhase.FirstDown
            OneFingerZoomPhase.AwaitSecondDown -> OneFingerZoomPhase.SecondDown
            else -> return emptyList()
        }
        downX = x
        downY = y
        lastX = x
        lastY = y
        moved = false
        firstHeld = false
        phase = nextPhase
        return emptyList()
    }

    fun move(x: Float, y: Float): List<OneFingerZoomCommand> {
        val dx = x - lastX
        val dy = y - lastY
        lastX = x
        lastY = y
        return when (phase) {
            OneFingerZoomPhase.FirstDown, OneFingerZoomPhase.SecondDown -> {
                if (beyondSlop(x, y)) {
                    moved = true
                    phase = OneFingerZoomPhase.Idle
                }
                emptyList()
            }
            OneFingerZoomPhase.Holding ->
                if (dx == 0f && dy == 0f) emptyList() else listOf(OneFingerZoomCommand.HoldPan(dx, dy))
            OneFingerZoomPhase.Idle, OneFingerZoomPhase.AwaitSecondDown -> emptyList()
        }
    }

    fun up(x: Float, y: Float): List<OneFingerZoomCommand> = when (phase) {
        OneFingerZoomPhase.FirstDown -> {
            if (firstHeld) {
                phase = OneFingerZoomPhase.Idle
                listOf(OneFingerZoomCommand.Tap)
            } else {
                phase = OneFingerZoomPhase.AwaitSecondDown
                emptyList()
            }
        }
        OneFingerZoomPhase.AwaitSecondDown -> {
            phase = OneFingerZoomPhase.Idle
            emptyList()
        }
        OneFingerZoomPhase.SecondDown -> {
            phase = OneFingerZoomPhase.Idle
            if (moved) emptyList() else listOf(OneFingerZoomCommand.DoubleTap)
        }
        OneFingerZoomPhase.Holding -> {
            phase = OneFingerZoomPhase.Idle
            listOf(OneFingerZoomCommand.HoldEnd)
        }
        OneFingerZoomPhase.Idle -> emptyList()
    }

    fun timeout(): List<OneFingerZoomCommand> = when (phase) {
        OneFingerZoomPhase.FirstDown -> {
            // A long first press is a long press, not a tap; wait for its release.
            firstHeld = true
            emptyList()
        }
        OneFingerZoomPhase.AwaitSecondDown -> {
            phase = OneFingerZoomPhase.Idle
            listOf(OneFingerZoomCommand.Tap)
        }
        OneFingerZoomPhase.SecondDown -> {
            if (moved) {
                phase = OneFingerZoomPhase.Idle
                emptyList()
            } else {
                phase = OneFingerZoomPhase.Holding
                listOf(OneFingerZoomCommand.HoldStart(lastX, lastY))
            }
        }
        OneFingerZoomPhase.Idle, OneFingerZoomPhase.Holding -> emptyList()
    }

    fun cancel(): List<OneFingerZoomCommand> {
        val commands = if (phase == OneFingerZoomPhase.Holding) {
            listOf(OneFingerZoomCommand.HoldEnd)
        } else {
            emptyList()
        }
        phase = OneFingerZoomPhase.Idle
        return commands
    }

    private fun beyondSlop(x: Float, y: Float): Boolean {
        val dx = x - downX
        val dy = y - downY
        return dx * dx + dy * dy > touchSlop * touchSlop
    }
}

/** Starts the temporary zoom at the existing double-tap scale, centred on the held point. */
fun oneFingerZoomHoldStart(
    base: ZoomState,
    x: Float,
    y: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    targetScale: Float = DoubleTapZoomScale,
    contentWidth: Float = viewportWidth,
    contentHeight: Float = viewportHeight,
): ZoomState =
    if (base.scale > MinZoomScale) {
        base
    } else {
        base.applyPinch(
            zoomChange = targetScale / base.scale.coerceAtLeast(MinZoomScale),
            centroidX = x,
            centroidY = y,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
        )
    }
