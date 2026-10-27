package org.parkjw.apps.pdfbook.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
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
        // Strings made only of these characters are drawn with the JetBrains Mono subset,
        // which carries the en dash for the two-page range.
        val numericChars = "0123456789.,%/×:-– ".toSet()
        val formatArg = Regex("""%(\d+\$)?[a-zA-Z]""")
        val missing = linkedSetOf<Char>()
        File(root, "app/src/main/res").walkTopDown()
            .filter { it.isFile && it.name == "strings.xml" }
            .forEach { file ->
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                for (tag in listOf("string", "item")) {
                    val nodes = doc.getElementsByTagName(tag)
                    for (i in 0 until nodes.length) {
                        val text = formatArg.replace(nodes.item(i).textContent ?: continue, "")
                        if (text.isNotEmpty() && text.all { it in numericChars }) continue
                        for (ch in text) {
                            if (ch !in charset) missing.add(ch)
                        }
                    }
                }
            }
        assertEquals("missing from pretendard charset: $missing", emptySet<Char>(), missing)
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
