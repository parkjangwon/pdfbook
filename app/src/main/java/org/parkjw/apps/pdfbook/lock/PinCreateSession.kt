package org.parkjw.apps.pdfbook.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PinCreateSession {
    private val buffer = PinBuffer()
    private val first = CharArray(8)
    var firstLength by mutableIntStateOf(0)
        private set
    var confirming by mutableStateOf(false)
        private set
    var mismatch by mutableStateOf(false)
        private set

    val length: Int get() = buffer.length

    fun digit(digit: Char): CharArray? {
        if (confirming) {
            if (buffer.length >= firstLength) return null
            buffer.push(digit)
            if (buffer.length != firstLength) return null
            val typed = buffer.copyOut()
            val matches = sameDigits(first, firstLength, typed, typed.size)
            typed.fill('\u0000')
            return if (matches) {
                val created = first.copyOf(firstLength)
                reset()
                created
            } else {
                buffer.clear()
                clearFirst()
                confirming = false
                mismatch = true
                null
            }
        }
        if (buffer.length < 8) {
            mismatch = false
            buffer.push(digit)
        }
        return null
    }

    fun delete() {
        buffer.pop()
    }

    fun confirmFirst(): Boolean {
        if (confirming || buffer.length !in PIN_LENGTH) return false
        val typed = buffer.copyOut()
        typed.copyInto(first)
        firstLength = typed.size
        typed.fill('\u0000')
        buffer.clear()
        confirming = true
        mismatch = false
        return true
    }

    fun back(): Boolean {
        if (!confirming) {
            reset()
            return true
        }
        confirming = false
        mismatch = false
        buffer.clear()
        clearFirst()
        return false
    }

    private fun clearFirst() {
        first.fill('\u0000')
        firstLength = 0
    }

    private fun reset() {
        buffer.clear()
        clearFirst()
        confirming = false
        mismatch = false
    }
}
