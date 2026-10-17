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
            Regex("""shadowElevation\s*=\s*+(?!0(?:\.0)?\.dp\b)"""),
            Regex("""tonalElevation\s*=\s*+(?!0(?:\.0)?\.dp\b)"""),
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
    fun textButtonsSpecifyInkColors() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                findTextButtonsWithoutColors(file.readText()).map { "${file.relativeTo(root)}: $it" }
            }
            .toList()
        assertEquals("TextButton without colors/wrapper:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun radioButtonsUseSharedColors() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                findRadioButtonsWithoutSharedColors(file.readText()).map { "${file.relativeTo(root)}: $it" }
            }
            .toList()
        assertEquals("RadioButton without shared colors:\n${offenders.joinToString("\n")}", emptyList<String>(), offenders)
    }

    @Test
    fun progressAndSliderOnlyViaSharedWrappers() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val allowed = setOf("ProgressBar.kt")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in allowed }
            .flatMap { file ->
                findDirectProgressOrSliderCalls(file.readText()).map { "${file.relativeTo(root)}: $it" }
            }
            .toList()
        assertEquals(
            "LinearProgressIndicator/Slider outside ProgressBar.kt:\n${offenders.joinToString("\n")}",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun dropdownMenuOnlyViaSharedWrapper() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val allowed = setOf("PdfBookDropdownMenu.kt")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in allowed }
            .flatMap { file ->
                findDirectDropdownMenuCalls(file.readText()).map { "${file.relativeTo(root)}: $it" }
            }
            .toList()
        assertEquals(
            "DropdownMenu outside PdfBookDropdownMenu.kt:\n${offenders.joinToString("\n")}",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun snackbarOnlyViaSharedWrapper() {
        val root = projectRoot()
        val javaRoot = File(root, "app/src/main/java")
        val allowed = setOf("PdfBookSnackbar.kt")
        val offenders = javaRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in allowed }
            .flatMap { file ->
                val text = file.readText()
                (findDirectSnackbarCalls(text) + findDirectSnackbarHostCalls(text))
                    .map { "${file.relativeTo(root)}: $it" }
            }
            .toList()
        assertEquals(
            "Snackbar/SnackbarHost outside PdfBookSnackbar.kt:\n${offenders.joinToString("\n")}",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun sharedSnackbarUsesZeroShadowElevation() {
        val source = File(
            projectRoot(),
            "app/src/main/java/org/parkjw/apps/pdfbook/ui/theme/PdfBookSnackbar.kt",
        ).readText()
        assertTrue(
            "PdfBookSnackbar must set shadowElevation = 0.dp",
            usesZeroShadowElevation(source),
        )
        assertTrue(
            "PdfBookSnackbar must set tonalElevation = 0.dp",
            usesZeroTonalElevation(source),
        )
        assertFalse(
            "self-check: missing zero shadow must fail",
            usesZeroShadowElevation("Surface(color = c) {}"),
        )
        assertFalse(
            "self-check: non-zero shadow must fail",
            usesZeroShadowElevation("Surface(shadowElevation = 6.dp) {}"),
        )
        assertFalse(
            "self-check: missing zero tonal must fail",
            usesZeroTonalElevation("Surface(shadowElevation = 0.dp) {}"),
        )
        assertFalse(
            "self-check: non-zero tonal must fail",
            usesZeroTonalElevation("Surface(tonalElevation = 6.dp) {}"),
        )
    }

    @Test
    fun sharedSnackbarFillsWidthWithMaxCap() {
        val source = File(
            projectRoot(),
            "app/src/main/java/org/parkjw/apps/pdfbook/ui/theme/PdfBookSnackbar.kt",
        ).readText()
        assertTrue(
            "PdfBookSnackbar must use widthIn(max = 600.dp)",
            usesSnackbarMaxWidth(source),
        )
        assertTrue(
            "PdfBookSnackbar must use fillMaxWidth()",
            usesSnackbarFillMaxWidth(source),
        )
        assertFalse(
            "self-check: missing widthIn max must fail",
            usesSnackbarMaxWidth("Modifier.fillMaxWidth()"),
        )
        assertFalse(
            "self-check: missing fillMaxWidth must fail",
            usesSnackbarFillMaxWidth("Modifier.widthIn(max = 600.dp)"),
        )
    }

    @Test
    fun m3ShapesDoNotUsePill() {
        val shape = File(projectRoot(), "app/src/main/java/org/parkjw/apps/pdfbook/ui/theme/Shape.kt").readText()
        assertTrue("DialogCorner missing", Regex("""val\s+DialogCorner\s*=""").containsMatchIn(shape))
        assertTrue(
            "extraLarge must be DialogCorner",
            Regex("""extraLarge\s*=\s*DialogCorner""").containsMatchIn(shape),
        )
        assertFalse(
            "pill must not map to M3 Shapes slots",
            Regex("""(?:extraSmall|small|medium|large|extraLarge)\s*=\s*(?:PillCorner|PdfBookShapeTokens\.pill|CircleShape)""")
                .containsMatchIn(shape),
        )
        assertTrue(
            "PdfBookShapeTokens.pill required",
            Regex("""object\s+PdfBookShapeTokens[\s\S]*?\bpill\b""").containsMatchIn(shape),
        )
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
        assertTrue(findTextButtonsWithoutColors("TextButton(onClick = {}) { Text(\"x\") }").isNotEmpty())
        assertFalse(
            findTextButtonsWithoutColors(
                "TextButton(onClick = {}, colors = pdfBookTextButtonColors()) { Text(\"x\") }",
            ).isNotEmpty(),
        )
        assertFalse(
            findTextButtonsWithoutColors(
                "TextButton(onClick = {}, colors = ButtonDefaults.textButtonColors(contentColor = c)) {}",
            ).isNotEmpty(),
        )
        assertTrue(findRadioButtonsWithoutSharedColors("RadioButton(selected = true, onClick = null)").isNotEmpty())
        assertFalse(
            findRadioButtonsWithoutSharedColors(
                "RadioButton(selected = true, onClick = null, colors = pdfBookRadioButtonColors())",
            ).isNotEmpty(),
        )
        assertTrue(findDirectProgressOrSliderCalls("LinearProgressIndicator(progress = { 0.5f })").isNotEmpty())
        assertTrue(findDirectProgressOrSliderCalls("Slider(value = 0f, onValueChange = {})").isNotEmpty())
        assertFalse(findDirectProgressOrSliderCalls("PdfBookProgressBar(progress = 0.5f)").isNotEmpty())
        assertFalse(findDirectProgressOrSliderCalls("PdfBookReaderSlider(value = 0f, onValueChange = {})").isNotEmpty())
        assertTrue(findDirectDropdownMenuCalls("DropdownMenu(expanded = true, onDismissRequest = {}) {}").isNotEmpty())
        assertFalse(findDirectDropdownMenuCalls("PdfBookDropdownMenu(expanded = true, onDismissRequest = {}) {}").isNotEmpty())
        assertTrue(findDirectSnackbarCalls("Snackbar(snackbarData = data)").isNotEmpty())
        assertFalse(findDirectSnackbarCalls("PdfBookSnackbar(snackbarData = data)").isNotEmpty())
        assertTrue(findDirectSnackbarHostCalls("SnackbarHost(hostState = state) {}").isNotEmpty())
        assertFalse(findDirectSnackbarHostCalls("PdfBookSnackbarHost(hostState = state)").isNotEmpty())
        assertTrue(usesZeroShadowElevation("Surface(shadowElevation = 0.dp, tonalElevation = 0.dp) {}"))
        assertTrue(usesZeroTonalElevation("Surface(shadowElevation = 0.dp, tonalElevation = 0.dp) {}"))
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

        private val textButtonCall =
            Regex("""(?<![A-Za-z0-9_])TextButton\s*\(""")
        private val radioButtonCall =
            Regex("""(?<![A-Za-z0-9_])RadioButton\s*\(""")
        private val directProgressOrSliderCall =
            Regex("""(?<![A-Za-z0-9_])(?:LinearProgressIndicator|Slider)\s*\(""")
        private val directDropdownMenuCall =
            Regex("""(?<![A-Za-z0-9_])DropdownMenu\s*\(""")
        private val directSnackbarCall =
            Regex("""(?<![A-Za-z0-9_])Snackbar\s*\(""")
        private val directSnackbarHostCall =
            Regex("""(?<![A-Za-z0-9_])SnackbarHost\s*\(""")
        private val zeroShadowElevation =
            Regex("""shadowElevation\s*=\s*0(?:\.0)?\.dp\b""")
        private val zeroTonalElevation =
            Regex("""tonalElevation\s*=\s*0(?:\.0)?\.dp\b""")
        private val snackbarMaxWidth =
            Regex("""widthIn\s*\(\s*max\s*=\s*600(?:\.0)?\.dp\s*\)""")
        private val snackbarFillMaxWidth =
            Regex("""fillMaxWidth\s*\(\s*\)""")

        fun findTextButtonsWithoutColors(source: String): List<String> {
            val hits = mutableListOf<String>()
            textButtonCall.findAll(source).forEach { match ->
                val args = balancedArgs(source, match.range.last) ?: return@forEach
                if (!Regex("""\bcolors\s*=""").containsMatchIn(args)) {
                    hits += "TextButton($args)"
                }
            }
            return hits
        }

        fun findRadioButtonsWithoutSharedColors(source: String): List<String> {
            val hits = mutableListOf<String>()
            radioButtonCall.findAll(source).forEach { match ->
                val args = balancedArgs(source, match.range.last) ?: return@forEach
                if (!Regex("""\bcolors\s*=\s*pdfBookRadioButtonColors\s*\(""").containsMatchIn(args)) {
                    hits += "RadioButton($args)"
                }
            }
            return hits
        }

        fun findDirectProgressOrSliderCalls(source: String): List<String> =
            directProgressOrSliderCall.findAll(source).map { it.value.trim() }.toList()

        fun findDirectDropdownMenuCalls(source: String): List<String> =
            directDropdownMenuCall.findAll(source).map { it.value.trim() }.toList()

        fun findDirectSnackbarCalls(source: String): List<String> =
            directSnackbarCall.findAll(source).map { it.value.trim() }.toList()

        fun findDirectSnackbarHostCalls(source: String): List<String> =
            directSnackbarHostCall.findAll(source).map { it.value.trim() }.toList()

        fun usesZeroShadowElevation(source: String): Boolean =
            zeroShadowElevation.containsMatchIn(source)

        fun usesZeroTonalElevation(source: String): Boolean =
            zeroTonalElevation.containsMatchIn(source)

        fun usesSnackbarMaxWidth(source: String): Boolean =
            snackbarMaxWidth.containsMatchIn(source)

        fun usesSnackbarFillMaxWidth(source: String): Boolean =
            snackbarFillMaxWidth.containsMatchIn(source)

        private fun balancedArgs(source: String, openParenIndex: Int): String? {
            if (openParenIndex !in source.indices || source[openParenIndex] != '(') return null
            var depth = 0
            for (i in openParenIndex until source.length) {
                when (source[i]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) return source.substring(openParenIndex + 1, i)
                    }
                }
            }
            return null
        }
    }
}
