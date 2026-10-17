package org.parkjw.apps.pdfbook.ui.theme

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Test

class NumericTextTest {
    @Test
    fun findsDigitRunsAndSubsetSymbols() {
        assertEquals(listOf(2..3), numericRuns("책 12권"))
        assertEquals(listOf(0..4), numericRuns("3 / 7"))
        assertEquals(listOf(0..2), numericRuns("42%"))
        assertEquals(listOf(0..3), numericRuns("2.0×"))
        assertEquals(listOf(0..4), numericRuns("1.2.3"))
        assertEquals(emptyList<IntRange>(), numericRuns("책 권"))
    }

    @Test
    fun withNumericSpansAppliesStyleOnlyToRuns() {
        val style = SpanStyle(fontFamily = FontFamily.Monospace)
        val annotated = withNumericSpans("책 12권 · 42%", style)
        assertEquals("책 12권 · 42%", annotated.text)
        val spans = annotated.spanStyles
        assertEquals(2, spans.size)
        assertEquals(2, spans[0].start)
        assertEquals(4, spans[0].end)
        assertEquals(8, spans[1].start)
        assertEquals(11, spans[1].end)
    }
}
