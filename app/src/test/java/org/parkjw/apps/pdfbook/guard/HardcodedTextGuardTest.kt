package org.parkjw.apps.pdfbook.guard

import org.junit.Assert.assertEquals
import org.junit.Test

class HardcodedTextGuardTest {
    @Test
    fun composablesUseStringResourcesForUserFacingText() {
        val offenders = SourceFiles.kotlin().flatMap { file ->
            findHardcodedText(file.readText()).map { "${SourceFiles.rel(file)}:$it" }
        }
        assertEquals("hardcoded user-facing text:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun guardDetectsSampleViolations() {
        assertEquals(1, findHardcodedText("""Text("설정")""").size)
        assertEquals(1, findHardcodedText("""Text(text = "Settings", color = c)""").size)
        assertEquals(1, findHardcodedText("""Icon(painter = p, contentDescription = "Back")""").size)
        assertEquals(1, findHardcodedText("""host.showSnackbar("Saved")""").size)
        assertEquals(2, findHardcodedText("""showSnackbar(message = "Saved", actionLabel = "Undo")""").size)
        assertEquals(0, findHardcodedText("""Text(stringResource(R.string.settings))""").size)
        assertEquals(0, findHardcodedText("""Text(text = "${'$'}percent%")""").size)
        assertEquals(0, findHardcodedText("""Text("${'$'}{page} / ${'$'}{count}")""").size)
        assertEquals(0, findHardcodedText("""BookCover(title = "", cover = b)""").size)
        assertEquals(0, findHardcodedText("""Icon(painter = p, contentDescription = null)""").size)
        assertEquals(0, findHardcodedText("""// Text("note")""").size)
        assertEquals(0, findHardcodedText("@Preview\n@Composable\nfun P() {\n    Text(\"Preview\")\n}\n").size)
    }

    companion object {
        private val literalArg = Regex(
            """(?:\b(?:Text|showSnackbar)\s*\(\s*|\b(?:text|contentDescription|label|title|placeholder|actionLabel|onClickLabel|stateDescription|message)\s*=\s*)"((?:[^"\\]|\\.)*)"""",
        )
        private val template = Regex("""\$\{[^}]*\}|\$\w+""")
        private val letter = Regex("""\p{L}""")
        private val previewFunction = Regex("""@Preview\b[\s\S]*?\n}""")
        private val lineComment = Regex("""//[^\n]*""")

        fun findHardcodedText(source: String): List<String> {
            val code = source
                .replace(previewFunction) { m -> m.value.replace(Regex("[^\n]"), " ") }
                .replace(lineComment) { m -> " ".repeat(m.value.length) }
            return literalArg.findAll(code)
                .filter { letter.containsMatchIn(template.replace(it.groupValues[1], "")) }
                .map { "${SourceFiles.lineOf(code, it.range.first)}: ${it.value}" }
                .toList()
        }
    }
}
