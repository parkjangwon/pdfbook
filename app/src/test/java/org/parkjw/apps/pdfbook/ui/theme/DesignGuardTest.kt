package org.parkjw.apps.pdfbook.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class DesignGuardTest {
    @Test
    fun noShadowBrushOrNonZeroElevationOutsideTokens() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val lineBanned = listOf(
            Regex("""\bshadow\s*\("""),
            Regex("""Brush\."""),
            Regex("""shadowElevation\s*=\s*(?!0(\.dp|\.0\.dp)\b)"""),
            Regex("""tonalElevation\s*=\s*(?!0(\.dp|\.0\.dp)\b)"""),
        )
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val rel = file.relativeTo(root)
                val lineHits = file.readLines().mapIndexedNotNull { index, line ->
                    if (lineBanned.any { it.containsMatchIn(line) }) {
                        "$rel:${index + 1}: $line"
                    } else {
                        null
                    }
                }
                val elevHits = findBadCardElevations(file.readText()).map { "$rel: $it" }
                lineHits + elevHits
            }
            .toList()
        assertEquals("design elevation/brush violations:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun primaryNotUsedAsButtonContainerColor() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                val hits = Regex(
                    """buttonColors\s*\([^)]*containerColor\s*=\s*[^,\n)]*primary""",
                    RegexOption.IGNORE_CASE,
                ).findAll(text)
                hits.map { "${file.relativeTo(root)}: ${it.value}" }
            }
            .toList()
        assertEquals("primary used as button container:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun buttonElevationCallsAreZero() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                findBadButtonElevations(file.readText()).map { "${file.relativeTo(root)}: $it" }
            }
            .toList()
        assertEquals("non-zero buttonElevation:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun guardsCatchViolationSnippets() {
        assertTrue(
            findBadButtonElevations("ButtonDefaults.buttonElevation(defaultElevation = 4.dp)").isNotEmpty(),
        )
        assertTrue(
            findBadButtonElevations(
                """
                ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 0.dp,
                )
                """.trimIndent(),
            ).isNotEmpty(),
        )
        assertFalse(
            findBadButtonElevations("ButtonDefaults.buttonElevation(defaultElevation = 0.dp)").isNotEmpty(),
        )
        assertTrue(
            findBadCardElevations("CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)").isNotEmpty(),
        )
        assertTrue(
            findBadCardElevations(
                """
                CardDefaults.cardElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 0.dp,
                )
                """.trimIndent(),
            ).isNotEmpty(),
        )
        assertTrue(findBadCardElevations("CardDefaults.cardElevation()").isNotEmpty())
        assertFalse(
            findBadCardElevations("CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)").isNotEmpty(),
        )
    }

    private fun projectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(12) {
            if (File(dir, "settings.gradle.kts").isFile || File(dir, "settings.gradle").isFile) {
                return dir
            }
            dir = dir.parentFile ?: fail("settings.gradle.kts not found").let { return dir }
        }
        fail("settings.gradle.kts not found")
        throw IllegalStateException()
    }

    companion object {
        private val nonZeroDpAssign = Regex("""=\s*(?!0\.dp\b)[\d.]+\.dp\b""")
        private val buttonElevationCall =
            Regex("""buttonElevation\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
        private val cardElevationCall =
            Regex("""CardDefaults\.cardElevation\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)

        fun findBadButtonElevations(source: String): List<String> =
            buttonElevationCall.findAll(source).mapNotNull { match ->
                val args = match.groupValues[1]
                if (args.isBlank() || nonZeroDpAssign.containsMatchIn(args)) match.value else null
            }.toList()

        fun findBadCardElevations(source: String): List<String> =
            cardElevationCall.findAll(source).mapNotNull { match ->
                val args = match.groupValues[1]
                if (args.isBlank() || nonZeroDpAssign.containsMatchIn(args)) match.value else null
            }.toList()
    }
}
