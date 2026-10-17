package org.parkjw.apps.pdfbook.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class ColorLiteralGuardTest {
    @Test
    fun kotlinSourcesOutsideTokensHaveNoColorLiterals() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        assertTrue(javaRoot.isDirectory)
        val banned = Regex("""Color\(0x|Color\.White\b|Color\.Black\b""")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "Tokens.kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (banned.containsMatchIn(line)) {
                        "${file.relativeTo(root)}:${index + 1}: $line"
                    } else {
                        null
                    }
                }
            }
            .toList()
        assertEquals("color literals outside Tokens.kt:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun xmlSourcesOutsideColorsHaveNoHexColorLiterals() {
        val root = projectRoot()
        val resRoot = File(root, "app/src/main/res")
        assertTrue(resRoot.isDirectory)
        val hex = Regex("""#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})\b""")
        val offenders = resRoot.walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .filterNot { it.name == "colors.xml" }
            .filterNot { isLauncherIcon(it) }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (hex.containsMatchIn(line)) {
                        "${file.relativeTo(root)}:${index + 1}: $line"
                    } else {
                        null
                    }
                }
            }
            .toList()
        assertEquals("hex colors outside colors.xml:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun windowBackgroundMatchesTokenBackgrounds() {
        val root = projectRoot()
        val tokens = File(root, "app/src/main/java/org/parkjw/apps/pdfbook/ui/theme/Tokens.kt").readText()
        val lightXml = File(root, "app/src/main/res/values/colors.xml").readText()
        val darkXml = File(root, "app/src/main/res/values-night/colors.xml").readText()
        assertEquals(resolveTokenHex(tokens, "LightBackground"), colorHex(lightXml, "window_background"))
        assertEquals(resolveTokenHex(tokens, "DarkBackground"), colorHex(darkXml, "window_background"))
        assertEquals(resolveTokenHex(tokens, "LightOnSurfaceVariant"), colorHex(lightXml, "illustration_line"))
        assertEquals(resolveTokenHex(tokens, "DarkOnSurfaceVariant"), colorHex(darkXml, "illustration_line"))
        assertEquals(resolveTokenHex(tokens, "LightPrimary"), colorHex(lightXml, "illustration_accent"))
        assertEquals(resolveTokenHex(tokens, "DarkPrimary"), colorHex(darkXml, "illustration_accent"))
    }

    @Test
    fun normalizeHexKeepsAlphaAndGuardsCatchMismatch() {
        assertEquals("#80FFFFFF", normalizeHex("#80FFFFFF"))
        assertEquals("#88FFFFFF", normalizeHex("#8FFF"))
        assertEquals("#FFFFFFFF", normalizeHex("#FFFFFFFF"))
        assertEquals("#FFFFFFFF", normalizeHex("#FFFFFF"))
        assertNotEquals(normalizeHex("#80FFFFFF"), normalizeHex("#FFFFFFFF"))
        val tokens = "val Ghost = Color(0x80FFFFFF)\n"
        val xml = """<color name="window_background">#FFFFFFFF</color>"""
        assertNotEquals(resolveTokenHex(tokens, "Ghost"), colorHex(xml, "window_background"))
    }

    private fun isLauncherIcon(file: File): Boolean =
        file.name.startsWith("ic_launcher") ||
            file.parentFile?.name?.startsWith("mipmap") == true

    private fun colorHex(xml: String, name: String): String {
        val match = Regex("""<color\s+name="$name">\s*(#[0-9A-Fa-f]+)\s*</color>""")
            .find(xml)
            ?: error("$name missing")
        return normalizeHex(match.groupValues[1])
    }

    private fun resolveTokenHex(tokens: String, name: String, depth: Int = 0): String {
        assertFalse("token alias loop: $name", depth > 8)
        val match = Regex("""val\s+\b$name\b\s*=\s*(.+)""").find(tokens)
            ?: error("missing token $name")
        val rhs = match.groupValues[1].replace(Regex("""//.*$"""), "").trim()
        val hex = Regex("""Color\(0x([0-9A-Fa-f]+)\)""").find(rhs)
        if (hex != null) {
            return normalizeHex("#${hex.groupValues[1]}")
        }
        val alias = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\s*$""").find(rhs)?.groupValues?.get(1)
            ?: error("unresolved token $name = $rhs")
        return resolveTokenHex(tokens, alias, depth + 1)
    }

    companion object {
        fun normalizeHex(raw: String): String {
            val h = raw.removePrefix("#")
            val argb = when (h.length) {
                3 -> "FF" + h.map { "$it$it" }.joinToString("")
                4 -> {
                    val chars = h.map { "$it$it" }
                    chars[0] + chars[1] + chars[2] + chars[3]
                }
                6 -> "FF$h"
                8 -> h
                else -> error("bad hex $raw")
            }
            return "#${argb.uppercase()}"
        }
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
