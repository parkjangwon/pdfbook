package org.parkjw.apps.pdfbook.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class DesignGuardTest {
    @Test
    fun noShadowBrushOrNonZeroElevationOutsideTokens() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val banned = listOf(
            Regex("""\bshadow\s*\("""),
            Regex("""Brush\."""),
            Regex("""shadowElevation\s*=\s*(?!0(\.dp|\.0\.dp)\b)"""),
            Regex("""tonalElevation\s*=\s*(?!0(\.dp|\.0\.dp)\b)"""),
            Regex("""CardDefaults\.cardElevation\s*\((?![^)]*0\.dp)[^)]*\)"""),
        )
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (banned.any { it.containsMatchIn(line) }) {
                        "${file.relativeTo(root)}:${index + 1}: $line"
                    } else {
                        null
                    }
                }
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
                val text = file.readText()
                Regex("""buttonElevation\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
                    .findAll(text)
                    .mapNotNull { match ->
                        val args = match.groupValues[1]
                        val nonZero = Regex("""=\s*(?!0\.dp)[\d.]+\\.dp""").containsMatchIn(args)
                        if (nonZero) "${file.relativeTo(root)}: ${match.value}" else null
                    }
            }
            .toList()
        assertEquals("non-zero buttonElevation:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
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
}
