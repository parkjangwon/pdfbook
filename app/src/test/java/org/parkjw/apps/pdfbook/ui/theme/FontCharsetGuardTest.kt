package org.parkjw.apps.pdfbook.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.awt.Font
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class FontCharsetGuardTest {
    @Test
    fun middleDotIsInPretendardSubsetCharset() {
        val charset = File(projectRoot(), "fonts/pretendard_subset_chars.txt").readText(Charsets.UTF_8).toSet()
        assertTrue('·' in charset)
    }

    @Test
    fun stringsXmlCharsAreInPretendardSubsetCharset() {
        val root = projectRoot()
        val charset = File(root, "fonts/pretendard_subset_chars.txt").readText(Charsets.UTF_8).toSet()
        assertTrue(charset.size > 2000)
        // Numbers and the punctuation around them are drawn with the JetBrains Mono subset,
        // which the build script bundles from MONO_CHARS. A numeric string must therefore be
        // drawable by the mono font subset; a glyph missing from monoChars is a .notdef box.
        val monoChars = monoChars(root)
        assertTrue("the page range separator needs the mono subset", '–' in monoChars)
        val monoFont = File(root, "app/src/main/res/font/jetbrains_mono_regular.ttf")
        assertTrue("jetbrains_mono_regular.ttf cmap must contain en dash", Font.createFont(Font.TRUETYPE_FONT, monoFont).canDisplay('–'))
        // A number placeholder still paints a digit, so it stays as a digit; other
        // placeholders carry words and are dropped before the character check.
        val numberArg = Regex("""%(\d+\$)?d""")
        val formatArg = Regex("""%(\d+\$)?[a-zA-Z]""")
        val missing = linkedSetOf<Char>()
        File(root, "app/src/main/res").walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .forEach { file ->
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                for (tag in listOf("string", "item")) {
                    val nodes = doc.getElementsByTagName(tag)
                    for (i in 0 until nodes.length) {
                        val text = formatArg.replace(
                            numberArg.replace(nodes.item(i).textContent ?: continue, "0"),
                            "",
                        )
                        val name = (nodes.item(i) as? org.w3c.dom.Element)?.getAttribute("name")
                        if (name != "go_to_page_hint" && isNumeric(text)) {
                            val undrawable = text.filter { it !in monoChars }.toSet()
                            assertEquals(
                                "numeric string that mono font cannot draw: $text -> $undrawable",
                                emptySet<Char>(),
                                undrawable,
                            )
                            continue
                        }
                        for (ch in text) {
                            if (ch !in charset) missing.add(ch)
                        }
                    }
                }
            }
        assertEquals("missing from pretendard charset: $missing", emptySet<Char>(), missing)
    }

    private fun isNumeric(text: String): Boolean =
        text.any { it.isDigit() } && text.none { it.isLetter() }

    private fun monoChars(root: File): Set<Char> {
        val script = File(root, "scripts/subset_fonts.py").readText(Charsets.UTF_8)
        val line = Regex("""MONO_CHARS = "([^"]*)"""").find(script)
            ?: fail("MONO_CHARS not found in scripts/subset_fonts.py").let { error("unreachable") }
        return line.groupValues[1].toSet()
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
