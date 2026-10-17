package org.parkjw.apps.pdfbook.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class TokensContrastTest {
    @Test
    fun bodyTextMeets45() {
        assertContrast("LightOnBackground", "LightBackground", 4.5)
        assertContrast("DarkOnBackground", "DarkBackground", 4.5)
        assertContrast("LightOnSurface", "LightSurfaceContainer", 4.5)
        assertContrast("DarkOnSurface", "DarkSurfaceContainer", 4.5)
        assertContrast("LightOnSurfaceVariant", "LightBackground", 4.5)
        assertContrast("DarkOnSurfaceVariant", "DarkBackground", 4.5)
        assertContrast("LightOnInkButton", "LightInkButton", 4.5)
        assertContrast("DarkOnInkButton", "DarkInkButton", 4.5)
        assertContrast("ReaderOnOverlay", "ReaderOverlay", 4.5)
    }

    @Test
    fun iconBarAndSliderMeet30() {
        assertContrast("LightPrimary", "LightBackground", 3.0)
        assertContrast("DarkPrimary", "DarkBackground", 3.0)
        assertContrast("LightPrimary", "LightSurfaceContainerHighest", 3.0)
        assertContrast("DarkPrimary", "DarkSurfaceVariant", 3.0)
        assertContrast("ReaderOnOverlayVariant", "ReaderOverlay", 3.0)
        assertContrast("ReaderAccent", "ReaderTrack", 3.0)
        assertContrast("ReaderAccent", "ReaderBackground", 3.0)
    }

    @Test
    fun documentedContrastPairsMatchCalculatedRatios() {
        assertRatioNear("LightOnBackground", "LightBackground", 14.3)
        assertRatioNear("DarkOnBackground", "DarkBackground", 14.6)
        assertRatioNear("LightOnSurface", "LightSurfaceContainer", 15.4)
        assertRatioNear("DarkOnSurface", "DarkSurfaceContainer", 13.4)
        assertRatioNear("LightOnSurfaceVariant", "LightBackground", 6.6)
        assertRatioNear("DarkOnSurfaceVariant", "DarkBackground", 8.2)
        assertRatioNear("LightPrimary", "LightBackground", 3.28)
        assertRatioNear("DarkPrimary", "DarkBackground", 7.47)
        assertRatioNear("LightPrimary", "LightSurfaceContainerHighest", 3.02)
        assertRatioNear("DarkPrimary", "DarkSurfaceContainerHighest", 5.34)
        assertRatioNear("LightOnInkButton", "LightInkButton", 14.3)
        assertRatioNear("DarkOnInkButton", "DarkInkButton", 14.6)
        assertRatioNear("ReaderOnOverlay", "ReaderOverlay", 15.5)
        assertRatioNear("ReaderOnOverlayVariant", "ReaderOverlay", 7.6)
        assertRatioNear("ReaderAccent", "ReaderTrack", 4.9)
    }

    @Test
    fun colorSchemeSlotsAreExplicitAndNotDefaultPurple() {
        val tokens = tokensText()
        val required = listOf(
            "background", "onBackground", "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
            "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer", "surfaceContainerHigh",
            "surfaceContainerHighest", "surfaceBright", "surfaceDim", "surfaceTint",
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "outline", "outlineVariant", "error", "onError", "errorContainer", "onErrorContainer",
            "inverseSurface", "inverseOnSurface", "inversePrimary", "scrim",
            "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
            "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
            "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
        )
        for (scheme in listOf("LightColorScheme", "DarkColorScheme")) {
            val block = schemeBlock(tokens, scheme)
            for (slot in required) {
                assertTrue("$scheme missing $slot", Regex("""\b$slot\s*=""").containsMatchIn(block))
            }
        }
        val purpleDefaults = setOf(
            "FF6750A4", "FFD0BCFF", "FF381E72", "FFEADDFF", "FF4F378B",
            "FFCCC2DC", "FF332D41", "FFE8DEF8", "FF4A4458",
            "FFEFB8C8", "FF492532", "FFFFD8E4", "FF633B48",
        )
        val hexes = Regex("""Color\(0x([0-9A-Fa-f]{8})\)""").findAll(tokens)
            .map { it.groupValues[1].uppercase() }
            .toSet()
        val overlap = hexes.intersect(purpleDefaults)
        assertEquals("M3 purple defaults still present: $overlap", emptySet<String>(), overlap)
        assertTrue(hexes.contains("FFF54E00"))
        assertTrue(hexes.contains("FFFF8A50"))
        assertTrue(hexes.contains("FFF7F7F4"))
        assertTrue(hexes.contains("FF1B1A16"))
    }

    private fun assertContrast(fgName: String, bgName: String, minRatio: Double) {
        val tokens = tokensText()
        val fg = parseArgb(resolveTokenHex(tokens, fgName))
        val bg = parseArgb(resolveTokenHex(tokens, bgName))
        val ratio = contrastRatio(fg, bg)
        assertTrue("$fgName on $bgName = $ratio < $minRatio", ratio + 1e-6 >= minRatio)
    }

    private fun assertRatioNear(fgName: String, bgName: String, expected: Double) {
        val tokens = tokensText()
        val fg = parseArgb(resolveTokenHex(tokens, fgName))
        val bg = parseArgb(resolveTokenHex(tokens, bgName))
        val ratio = contrastRatio(fg, bg)
        assertEquals("$fgName on $bgName", expected, ratio, 0.06)
    }

    private fun schemeBlock(tokens: String, name: String): String {
        val start = tokens.indexOf("val $name = ")
        require(start >= 0) { "missing $name" }
        val open = tokens.indexOf('(', start)
        var depth = 0
        for (i in open until tokens.length) {
            when (tokens[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return tokens.substring(open, i + 1)
                }
            }
        }
        error("unclosed $name")
    }

    private fun tokensText(): String {
        val root = projectRoot()
        return File(root, "app/src/main/java/org/parkjw/apps/pdfbook/ui/theme/Tokens.kt").readText()
    }

    private fun resolveTokenHex(tokens: String, name: String, depth: Int = 0): String {
        require(depth <= 8) { "alias loop $name" }
        val match = Regex("""val\s+$name\s*=\s*(.+)""").find(tokens)
            ?: error("missing token $name")
        val rhs = match.groupValues[1].trim()
        val hex = Regex("""Color\(0x([0-9A-Fa-f]+)\)""").find(rhs)
        if (hex != null) return hex.groupValues[1].uppercase().padStart(8, 'F').takeLast(8)
        val alias = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\s*$""").find(rhs)?.groupValues?.get(1)
            ?: error("unresolved $name = $rhs")
        return resolveTokenHex(tokens, alias, depth + 1)
    }

    private fun parseArgb(hex8: String): Int = hex8.toLong(16).toInt()

    private fun relativeLuminance(argb: Int): Double {
        fun channel(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(fg: Int, bg: Int): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun projectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(12) {
            if (File(dir, "settings.gradle.kts").isFile || File(dir, "settings.gradle").isFile) {
                return dir
            }
            dir = dir.parentFile ?: run {
                fail("settings.gradle.kts not found")
                error("unreachable")
            }
        }
        fail("settings.gradle.kts not found")
        error("unreachable")
    }
}
