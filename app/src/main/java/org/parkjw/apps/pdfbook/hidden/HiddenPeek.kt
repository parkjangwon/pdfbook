package org.parkjw.apps.pdfbook.hidden

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HiddenPeek {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun set(enabled: Boolean) {
        _enabled.value = enabled
    }

    fun reset() {
        _enabled.value = false
    }
}

fun shouldResetPeekOnStop(isChangingConfigurations: Boolean): Boolean =
    !isChangingConfigurations
